import express from 'express'
import fs from 'fs'
import net from 'net'
import path from 'path'
import { fileURLToPath } from 'url'
import { projectsRouter } from './routes/projects.js'
import { zonesRouter } from './routes/zones.js'
import { entitiesRouter } from './routes/entities.js'
import { exportRouter } from './routes/export.js'
import { getProjectsDir, getActiveProject, disconnectDb, deleteProject } from './db.js'
import { importNmd } from './import.js'
import { settingsRouter } from './routes/settings.js'

const __dirname = path.dirname(fileURLToPath(import.meta.url))

const app = express()
app.use(express.json())

app.use('/api/projects', projectsRouter)
app.use('/api', zonesRouter)
app.use('/api', entitiesRouter)
app.use('/api/export', exportRouter)
app.use('/api/settings', settingsRouter)

// Serve assets from the active project's assets directory
app.use('/api/assets', (req, res, next) => {
  const active = getActiveProject()
  if (!active) {
    res.status(404).json({ error: 'No project is open' })
    return
  }
  const assetsDir = path.join(getProjectsDir(), `${active}_assets`, 'assets')
  express.static(assetsDir)(req, res, next)
})

const port = parseInt(process.env.MAKER_PORT || '3001', 10)

function checkPortAvailable(p: number): Promise<boolean> {
  return new Promise((resolve) => {
    const server = net.createServer()
    server.once('error', () => resolve(false))
    server.once('listening', () => { server.close(() => resolve(true)) })
    server.listen(p)
  })
}

/** Try to gracefully shut down an existing maker server, then force-kill if needed. */
async function killExistingServer(p: number): Promise<void> {
  // First try the graceful shutdown endpoint
  try {
    const controller = new AbortController()
    setTimeout(() => controller.abort(), 2000)
    await fetch(`http://localhost:${p}/api/shutdown`, {
      method: 'POST',
      signal: controller.signal,
    })
    // Wait for it to actually release the port
    for (let i = 0; i < 10; i++) {
      await new Promise((r) => setTimeout(r, 500))
      if (await checkPortAvailable(p)) {
        console.log(`[startup] Previous server shut down gracefully.`)
        return
      }
    }
  } catch {
    // Endpoint not reachable or timed out — fall through to force kill
  }

  // Force kill whatever is holding the port (Windows-specific)
  if (process.platform === 'win32') {
    const { execSync } = await import('child_process')
    try {
      const output = execSync(`netstat -ano | findstr :${p} | findstr LISTENING`, { encoding: 'utf-8' })
      const pids = new Set(
        output.trim().split('\n')
          .map((line) => line.trim().split(/\s+/).pop())
          .filter((pid): pid is string => !!pid && pid !== '0')
      )
      for (const pid of pids) {
        console.log(`[startup] Force-killing PID ${pid} on port ${p}`)
        try { execSync(`taskkill /F /PID ${pid}`, { encoding: 'utf-8' }) } catch { /* already dead */ }
      }
    } catch {
      // netstat found nothing — port may have freed itself
    }
  } else {
    const { execSync } = await import('child_process')
    try {
      execSync(`fuser -k ${p}/tcp`, { encoding: 'utf-8' })
    } catch { /* nothing listening */ }
  }

  // Final check
  for (let i = 0; i < 6; i++) {
    await new Promise((r) => setTimeout(r, 500))
    if (await checkPortAvailable(p)) {
      console.log(`[startup] Port ${p} is now free.`)
      return
    }
  }
  console.error(`[startup] Could not free port ${p} — giving up.`)
  process.exit(1)
}

async function autoImportDefaultWorld() {
  // Look for the .nmd bundle relative to the maker directory
  const nmdPath = process.env.NEOMUD_DEFAULT_WORLD
    || path.resolve(__dirname, '..', '..', 'server', 'build', 'worlds', 'default-world.nmd')
  if (!fs.existsSync(nmdPath)) {
    console.log(`[startup] No default-world.nmd found at ${nmdPath}, skipping auto-import.`)
    return
  }

  const nmdMtime = fs.statSync(nmdPath).mtimeMs.toString()
  const dbPath = path.join(getProjectsDir(), '_default_world.db')

  if (fs.existsSync(dbPath)) {
    // Check staleness: compare .nmd mtime against what we stored at last import
    const mtimePath = path.join(getProjectsDir(), '_default_world.mtime')
    const storedMtime = fs.existsSync(mtimePath) ? fs.readFileSync(mtimePath, 'utf-8').trim() : null
    if (storedMtime === nmdMtime) {
      console.log('[startup] _default_world is up-to-date, skipping re-import.')
      return
    }
    console.log('[startup] default-world.nmd has changed, re-importing...')
    await deleteProject('_default_world')
  }

  try {
    console.log(`[startup] Importing default world from ${nmdPath}...`)
    await importNmd(nmdPath, '_default_world', true)
    // Persist the .nmd mtime so we can detect staleness next startup
    fs.writeFileSync(path.join(getProjectsDir(), '_default_world.mtime'), nmdMtime)
    console.log('[startup] Default world imported successfully as read-only project.')
  } catch (err) {
    console.error('[startup] Failed to import default world:', err)
  }
}

autoImportDefaultWorld().then(async () => {
  if (!await checkPortAvailable(port)) {
    console.log(`[startup] Port ${port} is in use — attempting to reclaim...`)
    await killExistingServer(port)
  }

  const server = app.listen(port, () => {
    console.log(`NeoMUDMaker API server running on http://localhost:${port}`)
  })

  server.on('error', (err: NodeJS.ErrnoException) => {
    if (err.code === 'EADDRINUSE') {
      console.error(`Port ${port} is already in use. Is another NeoMUDMaker instance running? Set MAKER_PORT env var to use a different port.`)
      process.exit(1)
    }
    throw err
  })

  // Graceful shutdown — close HTTP server and DB connection
  let shuttingDown = false
  function shutdown() {
    if (shuttingDown) return
    shuttingDown = true
    console.log('[shutdown] Closing maker server...')
    server.close(async () => {
      await disconnectDb()
      process.exit(0)
    })
    setTimeout(() => process.exit(0), 3000)
  }

  // POST /api/shutdown — allows the UI to gracefully stop the server
  app.post('/api/shutdown', (_req, res) => {
    res.json({ ok: true })
    shutdown()
  })

  process.on('SIGINT', shutdown)
  process.on('SIGTERM', shutdown)
  process.on('SIGBREAK', shutdown)
  process.on('exit', (code) => {
    console.log(`[shutdown] Process exiting with code ${code}`)
  })
})

import express from 'express'
import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'
import { projectsRouter } from './routes/projects.js'
import { zonesRouter } from './routes/zones.js'
import { entitiesRouter } from './routes/entities.js'
import { exportRouter } from './routes/export.js'
import { getProjectsDir } from './db.js'
import { importNmd } from './import.js'

const __dirname = path.dirname(fileURLToPath(import.meta.url))

const app = express()
app.use(express.json())

app.use('/api/projects', projectsRouter)
app.use('/api', zonesRouter)
app.use('/api', entitiesRouter)
app.use('/api/export', exportRouter)

const port = parseInt(process.env.MAKER_PORT || '3001', 10)

async function autoImportDefaultWorld() {
  const dbPath = path.join(getProjectsDir(), '_default_world.db')
  if (fs.existsSync(dbPath)) {
    console.log('[startup] _default_world already exists, skipping auto-import.')
    return
  }

  // Look for the .nmd bundle relative to the maker directory
  const nmdPath = path.resolve(__dirname, '..', '..', 'server', 'build', 'worlds', 'default-world.nmd')
  if (!fs.existsSync(nmdPath)) {
    console.log(`[startup] No default-world.nmd found at ${nmdPath}, skipping auto-import.`)
    return
  }

  try {
    console.log(`[startup] Importing default world from ${nmdPath}...`)
    await importNmd(nmdPath, '_default_world', true)
    console.log('[startup] Default world imported successfully as read-only project.')
  } catch (err) {
    console.error('[startup] Failed to import default world:', err)
  }
}

autoImportDefaultWorld().then(() => {
  const server = app.listen(port, () => {
    console.log(`NeoMUDMaker API server running on http://localhost:${port}`)
  })

  server.on('error', (err: NodeJS.ErrnoException) => {
    if (err.code === 'EADDRINUSE') {
      console.error(`Port ${port} is already in use. Set MAKER_PORT env var to use a different port.`)
      process.exit(1)
    }
    throw err
  })
})

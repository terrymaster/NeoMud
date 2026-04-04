import express from 'express'
import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'
import { authenticate } from './middleware/auth.js'
import { projectMiddleware, getProjectsDir } from './projectContext.js'
import { projectsRouter } from './routes/projects.js'
import { zonesRouter } from './routes/zones.js'
import { entitiesRouter } from './routes/entities.js'
import { exportRouter } from './routes/export.js'
import { settingsRouter } from './routes/settings.js'
import { pcSpritesRouter } from './routes/pcSprites.js'
import { defaultSfxRouter } from './routes/defaultSfx.js'
import { generateRouter } from './routes/generate.js'
import { assetMgmtRouter } from './routes/assets.js'
import { deleteProject } from './db.js'
import { importNmd } from './import.js'

const __dirname = path.dirname(fileURLToPath(import.meta.url))

export const app = express()
app.use(express.json({ limit: '50mb' }))

// ─── Auth on all API routes ─────────────────────────────
app.use('/api', authenticate)

// ─── Project list/create/delete (no project context needed) ─
app.use('/api/projects', projectsRouter)

// ─── Project-scoped routes (project context resolves DB per request) ─
const projectRouter = express.Router({ mergeParams: true })
projectRouter.use(projectMiddleware)
projectRouter.use(zonesRouter)
projectRouter.use(entitiesRouter)
projectRouter.use('/export', exportRouter)
projectRouter.use('/settings', settingsRouter)
projectRouter.use('/pc-sprites', pcSpritesRouter)
projectRouter.use('/default-sfx', defaultSfxRouter)
projectRouter.use('/generate', generateRouter)
projectRouter.use('/asset-mgmt', assetMgmtRouter)

// Serve assets from the project's assets directory
projectRouter.use('/assets', (req, res, next) => {
  if (!req.projectDir) {
    res.status(404).json({ error: 'No project context' })
    return
  }
  express.static(req.projectDir)(req, res, next)
})

app.use('/api/projects/:name', projectRouter)

// ─── Production: serve React static build ────────────────
if (process.env.NODE_ENV === 'production') {
  const distDir = path.join(__dirname, '..', 'dist')
  app.use(express.static(distDir))
  app.get('*', (_req, res) => {
    res.sendFile(path.join(distDir, 'index.html'))
  })
}

/**
 * Auto-import the default world as a shared read-only template.
 * Stored in projects/_shared/ so all users can see it.
 */
export async function autoImportDefaultWorld() {
  const nmdPath = process.env.NEOMUD_DEFAULT_WORLD
    || path.resolve(__dirname, '..', 'default_world.nmd')
  if (!fs.existsSync(nmdPath)) {
    console.log(`[startup] No default-world.nmd found at ${nmdPath}, skipping auto-import.`)
    return
  }

  const sharedDir = path.join(getProjectsDir(), '_shared')
  if (!fs.existsSync(sharedDir)) {
    fs.mkdirSync(sharedDir, { recursive: true })
  }

  const nmdMtime = fs.statSync(nmdPath).mtimeMs.toString()
  const dbPath = path.join(sharedDir, '_default_world.db')

  if (fs.existsSync(dbPath)) {
    const mtimePath = path.join(sharedDir, '_default_world.mtime')
    const storedMtime = fs.existsSync(mtimePath) ? fs.readFileSync(mtimePath, 'utf-8').trim() : null
    if (storedMtime === nmdMtime) {
      console.log('[startup] _default_world is up-to-date, skipping re-import.')
      return
    }
    console.log('[startup] default-world.nmd has changed, re-importing...')
    await deleteProject('_shared', '_default_world')
  }

  try {
    console.log(`[startup] Importing default world from ${nmdPath}...`)
    await importNmd(nmdPath, '_shared', '_default_world', true)
    fs.writeFileSync(path.join(sharedDir, '_default_world.mtime'), nmdMtime)
    console.log('[startup] Default world imported successfully as read-only shared template.')
  } catch (err) {
    console.error('[startup] Failed to import default world:', err)
  }
}

import express from 'express'
import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'
import { projectsRouter } from './routes/projects.js'
import { zonesRouter } from './routes/zones.js'
import { entitiesRouter } from './routes/entities.js'
import { exportRouter } from './routes/export.js'
import { getProjectsDir, getActiveProject, deleteProject } from './db.js'
import { importNmd } from './import.js'
import { settingsRouter } from './routes/settings.js'
import { pcSpritesRouter } from './routes/pcSprites.js'
import { generateRouter } from './routes/generate.js'
import { assetMgmtRouter } from './routes/assets.js'

const __dirname = path.dirname(fileURLToPath(import.meta.url))

export const app = express()
app.use(express.json({ limit: '50mb' }))

app.use('/api/projects', projectsRouter)
app.use('/api', zonesRouter)
app.use('/api', entitiesRouter)
app.use('/api/export', exportRouter)
app.use('/api/settings', settingsRouter)
app.use('/api/pc-sprites', pcSpritesRouter)
app.use('/api/generate', generateRouter)
app.use('/api/asset-mgmt', assetMgmtRouter)

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

export async function autoImportDefaultWorld() {
  const nmdPath = process.env.NEOMUD_DEFAULT_WORLD
    || path.resolve(__dirname, '..', 'default_world.nmd')
  if (!fs.existsSync(nmdPath)) {
    console.log(`[startup] No default-world.nmd found at ${nmdPath}, skipping auto-import.`)
    return
  }

  const nmdMtime = fs.statSync(nmdPath).mtimeMs.toString()
  const dbPath = path.join(getProjectsDir(), '_default_world.db')

  if (fs.existsSync(dbPath)) {
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
    fs.writeFileSync(path.join(getProjectsDir(), '_default_world.mtime'), nmdMtime)
    console.log('[startup] Default world imported successfully as read-only project.')
  } catch (err) {
    console.error('[startup] Failed to import default world:', err)
  }
}

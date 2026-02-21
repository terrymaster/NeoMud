import express from 'express'
import fs from 'fs'
import path from 'path'
import os from 'os'
import { execSync } from 'child_process'
import { fileURLToPath } from 'url'
import { zonesRouter } from '../routes/zones.js'
import { entitiesRouter } from '../routes/entities.js'
import { projectsRouter } from '../routes/projects.js'
import { exportRouter } from '../routes/export.js'
import { pcSpritesRouter } from '../routes/pcSprites.js'
import { defaultSfxRouter } from '../routes/defaultSfx.js'
import { createProject, openProject, deleteProject, getProjectsDir } from '../db.js'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const projectsDir = getProjectsDir()

// Track all test projects created in this process so we can clean up on unexpected exit
const activeTestProjects = new Set<string>()

// Safety net: synchronously delete all tracked test project files on process exit.
// This fires on normal exit, SIGINT (vitest catches it and calls process.exit()),
// SIGTERM, and uncaught exceptions — covering interrupted test runs.
let exitHandlerRegistered = false
function registerExitHandler() {
  if (exitHandlerRegistered) return
  exitHandlerRegistered = true
  process.on('exit', () => {
    for (const name of activeTestProjects) {
      try {
        const dbPath = path.join(projectsDir, `${name}.db`)
        if (fs.existsSync(dbPath)) fs.unlinkSync(dbPath)
      } catch {}
      try {
        const assetsPath = path.join(projectsDir, `${name}_assets`)
        if (fs.existsSync(assetsPath)) fs.rmSync(assetsPath, { recursive: true, force: true })
      } catch {}
    }
  })
}

/**
 * Create a test Express app backed by a fresh temp SQLite DB.
 * Returns the app and a cleanup function.
 */
export async function createTestApp(projectName?: string): Promise<{
  app: express.Express
  projectName: string
  cleanup: () => Promise<void>
}> {
  const name = projectName || `test_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`

  registerExitHandler()
  activeTestProjects.add(name)

  // Create project (pushes schema + opens it)
  await createProject(name)

  const app = express()
  app.use(express.json())
  app.use('/api/projects', projectsRouter)
  app.use('/api', zonesRouter)
  app.use('/api', entitiesRouter)
  app.use('/api/export', exportRouter)
  app.use('/api/pc-sprites', pcSpritesRouter)
  app.use('/api/default-sfx', defaultSfxRouter)

  const cleanup = async () => {
    try {
      await deleteProject(name)
    } catch {}
    activeTestProjects.delete(name)
  }

  return { app, projectName: name, cleanup }
}

/**
 * Create a test app with a read-only project.
 */
export async function createReadOnlyTestApp(): Promise<{
  app: express.Express
  projectName: string
  cleanup: () => Promise<void>
}> {
  const name = `test_ro_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`

  registerExitHandler()
  activeTestProjects.add(name)

  await createProject(name, true)

  const app = express()
  app.use(express.json())
  app.use('/api/projects', projectsRouter)
  app.use('/api', zonesRouter)
  app.use('/api', entitiesRouter)
  app.use('/api/export', exportRouter)
  app.use('/api/pc-sprites', pcSpritesRouter)
  app.use('/api/default-sfx', defaultSfxRouter)

  const cleanup = async () => {
    try {
      await deleteProject(name)
    } catch {}
    activeTestProjects.delete(name)
  }

  return { app, projectName: name, cleanup }
}

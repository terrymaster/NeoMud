import { Router } from 'express'
import path from 'path'
import { listProjects, createProject, deleteProject, forkProject } from '../db.js'
import { importNmd } from '../import.js'
import { isValidProjectName } from '../middleware/validateInput.js'

export const projectsRouter = Router()

// GET / — list this user's projects + shared templates
projectsRouter.get('/', async (req, res) => {
  try {
    const userId = req.user!.userId
    const projects = await listProjects(userId)
    res.json({ projects })
  } catch (err: any) {
    res.status(500).json({ error: err.message })
  }
})

// POST / — create project in user's directory
projectsRouter.post('/', async (req, res) => {
  try {
    const userId = req.user!.userId
    const { name } = req.body
    if (!name || !isValidProjectName(name)) {
      res.status(400).json({ error: 'Invalid project name. Use only letters, numbers, hyphens, underscores.' })
      return
    }
    await createProject(userId, name)
    res.json({ name })
  } catch (err: any) {
    res.status(500).json({ error: err.message })
  }
})

// DELETE /:name — delete project (ownership enforced by user directory scoping)
projectsRouter.delete('/:name', async (req, res) => {
  try {
    const userId = req.user!.userId
    const name = req.params.name as string
    if (!isValidProjectName(name)) {
      res.status(400).json({ error: 'Invalid project name' })
      return
    }
    if (name.startsWith('_')) {
      res.status(403).json({ error: 'Cannot delete system projects' })
      return
    }
    await deleteProject(userId, name)
    res.json({ ok: true })
  } catch (err: any) {
    res.status(500).json({ error: err.message })
  }
})

// POST /:name/open — NO-OP for backward compatibility
// Per-request project context replaces "opening" a project.
projectsRouter.post('/:name/open', async (req, res) => {
  const name = req.params.name as string
  res.json({ name, message: 'Project context is now per-request. No need to open.' })
})

// POST /:name/fork — fork a project to a new name within user's directory
projectsRouter.post('/:name/fork', async (req, res) => {
  try {
    const userId = req.user!.userId
    const source = req.params.name as string
    const { newName } = req.body
    if (!newName || !isValidProjectName(newName)) {
      res.status(400).json({ error: 'Invalid new project name' })
      return
    }
    if (!isValidProjectName(source)) {
      res.status(400).json({ error: 'Invalid source project name' })
      return
    }
    await forkProject(userId, source, newName)
    res.json({ name: newName })
  } catch (err: any) {
    res.status(500).json({ error: err.message })
  }
})

// POST /import — import .nmd file as a new project
projectsRouter.post('/import', async (req, res) => {
  try {
    const userId = req.user!.userId
    const { path: nmdPath, name } = req.body
    if (!nmdPath) {
      res.status(400).json({ error: 'path is required' })
      return
    }
    // Validate import path stays within safe directories
    if (nmdPath.includes('..') || (!path.isAbsolute(nmdPath) && nmdPath.startsWith('/'))) {
      res.status(400).json({ error: 'Invalid file path' })
      return
    }
    if (!nmdPath.endsWith('.nmd')) {
      res.status(400).json({ error: 'Only .nmd files can be imported' })
      return
    }
    const projectName = name || nmdPath.replace(/.*[/\\]/, '').replace(/\.nmd$/, '')
    if (!isValidProjectName(projectName)) {
      res.status(400).json({ error: 'Invalid project name derived from file' })
      return
    }
    await importNmd(nmdPath, userId, projectName)
    res.json({ name: projectName })
  } catch (err: any) {
    res.status(500).json({ error: err.message })
  }
})

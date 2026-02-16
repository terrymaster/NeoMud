import { Router } from 'express'
import {
  db,
  listProjects,
  createProject,
  openProject,
  deleteProject,
  getActiveProject,
  isReadOnly,
  forkProject,
} from '../db.js'
import { importNmd } from '../import.js'

export const projectsRouter = Router()

// GET / — list projects + active
projectsRouter.get('/', async (_req, res) => {
  try {
    const projects = await listProjects()
    const active = getActiveProject()
    const activeReadOnly = isReadOnly()
    res.json({ projects, active, activeReadOnly })
  } catch (err: any) {
    res.status(500).json({ error: err.message })
  }
})

// POST / — create project
projectsRouter.post('/', async (req, res) => {
  try {
    const { name } = req.body
    if (!name) {
      res.status(400).json({ error: 'name is required' })
      return
    }
    await createProject(name)
    res.json({ name })
  } catch (err: any) {
    res.status(500).json({ error: err.message })
  }
})

// DELETE /:name — delete project
projectsRouter.delete('/:name', async (req, res) => {
  try {
    await deleteProject(req.params.name)
    res.json({ ok: true })
  } catch (err: any) {
    res.status(500).json({ error: err.message })
  }
})

// POST /:name/open — open project
projectsRouter.post('/:name/open', async (req, res) => {
  try {
    await openProject(req.params.name)
    res.json({ name: req.params.name, readOnly: isReadOnly() })
  } catch (err: any) {
    res.status(500).json({ error: err.message })
  }
})

// POST /:name/fork — fork a project to a new name
projectsRouter.post('/:name/fork', async (req, res) => {
  try {
    const { newName } = req.body
    if (!newName) {
      res.status(400).json({ error: 'newName is required' })
      return
    }
    await forkProject(req.params.name, newName)
    res.json({ name: newName })
  } catch (err: any) {
    res.status(500).json({ error: err.message })
  }
})

// POST /import — import .nmd file as a new project
projectsRouter.post('/import', async (req, res) => {
  try {
    const { path: nmdPath, name } = req.body
    if (!nmdPath) {
      res.status(400).json({ error: 'path is required' })
      return
    }
    const projectName = name || nmdPath.replace(/.*[/\\]/, '').replace(/\.nmd$/, '')
    await importNmd(nmdPath, projectName)
    res.json({ name: projectName })
  } catch (err: any) {
    res.status(500).json({ error: err.message })
  }
})

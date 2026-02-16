import { Router } from 'express'
import {
  db,
  listProjects,
  createProject,
  openProject,
  deleteProject,
  getActiveProject,
} from '../db.js'

export const projectsRouter = Router()

// GET / — list projects + active
projectsRouter.get('/', (_req, res) => {
  try {
    const projects = listProjects()
    const active = getActiveProject()
    res.json({ projects, active })
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
    res.json({ name: req.params.name })
  } catch (err: any) {
    res.status(500).json({ error: err.message })
  }
})

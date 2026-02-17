import { Router } from 'express'
import { readSettings, writeSettings } from '../settings.js'

export const settingsRouter = Router()

settingsRouter.get('/', (_req, res) => {
  try {
    res.json(readSettings())
  } catch (err: any) {
    res.status(500).json({ error: err.message })
  }
})

settingsRouter.put('/', (req, res) => {
  try {
    const settings = req.body
    writeSettings(settings)
    res.json({ ok: true })
  } catch (err: any) {
    res.status(500).json({ error: err.message })
  }
})

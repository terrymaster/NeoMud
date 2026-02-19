import { Router, Request, Response, NextFunction } from 'express'
import { db, isReadOnly } from '../db.js'
import { seedDefaultSfx } from '../defaultSfxDefaults.js'

export const defaultSfxRouter = Router()

function rejectIfReadOnly(_req: Request, res: Response, next: NextFunction) {
  if (isReadOnly()) {
    res.status(403).json({ error: 'Project is read-only. Fork it to make changes.' })
    return
  }
  next()
}

// GET / — list all, with optional ?category= filter
defaultSfxRouter.get('/', async (req, res) => {
  try {
    const where: Record<string, string> = {}
    if (req.query.category) where.category = req.query.category as string

    const entries = await db().defaultSfx.findMany({
      where,
      orderBy: { id: 'asc' },
    })
    res.json(entries)
  } catch (err: any) {
    res.status(500).json({ error: err.message })
  }
})

// GET /:id
defaultSfxRouter.get('/:id', async (req, res) => {
  try {
    const entry = await db().defaultSfx.findUnique({ where: { id: req.params.id } })
    if (!entry) { res.status(404).json({ error: 'DefaultSfx not found' }); return }
    res.json(entry)
  } catch (err: any) {
    res.status(500).json({ error: err.message })
  }
})

// PUT /:id — update prompt/duration fields
defaultSfxRouter.put('/:id', rejectIfReadOnly, async (req, res) => {
  try {
    const { prompt, duration } = req.body
    const entry = await db().defaultSfx.update({
      where: { id: req.params.id },
      data: {
        ...(prompt !== undefined && { prompt }),
        ...(duration !== undefined && { duration }),
      },
    })
    res.json(entry)
  } catch (err: any) {
    if (err.code === 'P2025') {
      res.status(404).json({ error: 'DefaultSfx not found' })
      return
    }
    res.status(500).json({ error: err.message })
  }
})

// POST /reset — re-seed to defaults
defaultSfxRouter.post('/reset', rejectIfReadOnly, async (req, res) => {
  try {
    await db().defaultSfx.deleteMany()
    await seedDefaultSfx(db())
    const count = await db().defaultSfx.count()
    res.json({ ok: true, count })
  } catch (err: any) {
    res.status(500).json({ error: err.message })
  }
})

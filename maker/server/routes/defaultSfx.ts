import { Router, Request, Response } from 'express'
import { rejectIfReadOnly } from '../middleware/readOnly.js'
import { seedDefaultSfx } from '../defaultSfxDefaults.js'

/** Extract a route parameter as a string (Express 5 types return string | string[]). */
function param(req: Request, name: string): string {
  const val = req.params[name]
  return Array.isArray(val) ? val[0] : val ?? ''
}

export const defaultSfxRouter = Router()

// GET / — list all, with optional ?category= filter
defaultSfxRouter.get('/', async (req, res) => {
  try {
    const where: Record<string, string> = {}
    if (req.query.category) where.category = req.query.category as string

    const entries = await req.db!.defaultSfx.findMany({
      where,
      orderBy: { id: 'asc' },
    })
    res.json(entries)
  } catch (err) {
    console.error('[defaultSfx] error:', err)
    res.status(500).json({ error: 'Internal server error' })
  }
})

// GET /:id
defaultSfxRouter.get('/:id', async (req, res) => {
  try {
    const entry = await req.db!.defaultSfx.findUnique({ where: { id: param(req, 'id') } })
    if (!entry) { res.status(404).json({ error: 'DefaultSfx not found' }); return }
    res.json(entry)
  } catch (err) {
    console.error('[defaultSfx] error:', err)
    res.status(500).json({ error: 'Internal server error' })
  }
})

// PUT /:id — update prompt/duration fields
defaultSfxRouter.put('/:id', rejectIfReadOnly, async (req, res) => {
  try {
    const { prompt, duration } = req.body
    const entry = await req.db!.defaultSfx.update({
      where: { id: param(req, 'id') },
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
    await req.db!.defaultSfx.deleteMany()
    await seedDefaultSfx(req.db!)
    const count = await req.db!.defaultSfx.count()
    res.json({ ok: true, count })
  } catch (err) {
    console.error('[defaultSfx] error:', err)
    res.status(500).json({ error: 'Internal server error' })
  }
})

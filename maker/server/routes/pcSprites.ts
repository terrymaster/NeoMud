import { Router, Request, Response } from 'express'
import { rejectIfReadOnly } from '../middleware/readOnly.js'
import { seedPcSprites } from '../pcSpriteDefaults.js'

/** Extract a route parameter as a string (Express 5 types return string | string[]). */
function param(req: Request, name: string): string {
  const val = req.params[name]
  return Array.isArray(val) ? val[0] : val ?? ''
}

export const pcSpritesRouter = Router()

// GET /api/pc-sprites — list all, with optional race/gender/class filters
pcSpritesRouter.get('/', async (req, res) => {
  try {
    const where: Record<string, string> = {}
    if (req.query.race) where.race = req.query.race as string
    if (req.query.gender) where.gender = req.query.gender as string
    if (req.query.class) where.characterClass = req.query.class as string

    const sprites = await req.db!.pcSprite.findMany({
      where,
      orderBy: { id: 'asc' },
    })
    res.json(sprites)
  } catch (err) {
    console.error('[pcSprites] error:', err)
    res.status(500).json({ error: 'Internal server error' })
  }
})

// GET /api/pc-sprites/:id
pcSpritesRouter.get('/:id', async (req, res) => {
  try {
    const sprite = await req.db!.pcSprite.findUnique({ where: { id: param(req, 'id') } })
    if (!sprite) { res.status(404).json({ error: 'PcSprite not found' }); return }
    res.json(sprite)
  } catch (err) {
    console.error('[pcSprites] error:', err)
    res.status(500).json({ error: 'Internal server error' })
  }
})

// PUT /api/pc-sprites/:id — update image fields only
pcSpritesRouter.put('/:id', rejectIfReadOnly, async (req, res) => {
  try {
    const { imagePrompt, imageStyle, imageNegativePrompt, imageWidth, imageHeight } = req.body
    const sprite = await req.db!.pcSprite.update({
      where: { id: param(req, 'id') },
      data: {
        ...(imagePrompt !== undefined && { imagePrompt }),
        ...(imageStyle !== undefined && { imageStyle }),
        ...(imageNegativePrompt !== undefined && { imageNegativePrompt }),
        ...(imageWidth !== undefined && { imageWidth }),
        ...(imageHeight !== undefined && { imageHeight }),
      },
    })
    res.json(sprite)
  } catch (err: any) {
    if (err.code === 'P2025') {
      res.status(404).json({ error: 'PcSprite not found' })
      return
    }
    res.status(500).json({ error: err.message })
  }
})

// POST /api/pc-sprites/reset — re-seed all rows to defaults
pcSpritesRouter.post('/reset', rejectIfReadOnly, async (req, res) => {
  try {
    await req.db!.pcSprite.deleteMany()
    await seedPcSprites(req.db!)
    const count = await req.db!.pcSprite.count()
    res.json({ ok: true, count })
  } catch (err) {
    console.error('[pcSprites] error:', err)
    res.status(500).json({ error: 'Internal server error' })
  }
})

import { Router, Request, Response, NextFunction } from 'express'
import { db, isReadOnly } from '../db.js'
import { seedPcSprites } from '../pcSpriteDefaults.js'

export const pcSpritesRouter = Router()

function rejectIfReadOnly(_req: Request, res: Response, next: NextFunction) {
  if (isReadOnly()) {
    res.status(403).json({ error: 'Project is read-only. Fork it to make changes.' })
    return
  }
  next()
}

// GET /api/pc-sprites — list all, with optional race/gender/class filters
pcSpritesRouter.get('/', async (req, res) => {
  try {
    const where: Record<string, string> = {}
    if (req.query.race) where.race = req.query.race as string
    if (req.query.gender) where.gender = req.query.gender as string
    if (req.query.class) where.characterClass = req.query.class as string

    const sprites = await db().pcSprite.findMany({
      where,
      orderBy: { id: 'asc' },
    })
    res.json(sprites)
  } catch (err: any) {
    res.status(500).json({ error: err.message })
  }
})

// GET /api/pc-sprites/:id
pcSpritesRouter.get('/:id', async (req, res) => {
  try {
    const sprite = await db().pcSprite.findUnique({ where: { id: req.params.id } })
    if (!sprite) { res.status(404).json({ error: 'PcSprite not found' }); return }
    res.json(sprite)
  } catch (err: any) {
    res.status(500).json({ error: err.message })
  }
})

// PUT /api/pc-sprites/:id — update image fields only
pcSpritesRouter.put('/:id', rejectIfReadOnly, async (req, res) => {
  try {
    const { imagePrompt, imageStyle, imageNegativePrompt, imageWidth, imageHeight } = req.body
    const sprite = await db().pcSprite.update({
      where: { id: req.params.id },
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
    await db().pcSprite.deleteMany()
    await seedPcSprites(db())
    res.json({ ok: true, count: 270 })
  } catch (err: any) {
    res.status(500).json({ error: err.message })
  }
})

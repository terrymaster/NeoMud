import { Router, Request, Response, NextFunction } from 'express'
import { db, isReadOnly } from '../db.js'

export const entitiesRouter = Router()

function rejectIfReadOnly(_req: Request, res: Response, next: NextFunction) {
  if (isReadOnly()) {
    res.status(403).json({ error: 'Project is read-only. Fork it to make changes.' })
    return
  }
  next()
}

// ─── Items ─────────────────────────────────────────────────────

entitiesRouter.get('/items', async (_req, res) => {
  try {
    const items = await db().item.findMany()
    res.json(items)
  } catch (err: any) {
    res.status(500).json({ error: err.message })
  }
})

entitiesRouter.get('/items/:id', async (req, res) => {
  try {
    const item = await db().item.findUnique({ where: { id: req.params.id } })
    if (!item) { res.status(404).json({ error: 'Item not found' }); return }
    res.json(item)
  } catch (err: any) {
    res.status(500).json({ error: err.message })
  }
})

entitiesRouter.post('/items', rejectIfReadOnly, async (req, res) => {
  try {
    const item = await db().item.create({ data: req.body })
    res.json(item)
  } catch (err: any) {
    res.status(500).json({ error: err.message })
  }
})

entitiesRouter.put('/items/:id', rejectIfReadOnly, async (req, res) => {
  try {
    const item = await db().item.update({ where: { id: req.params.id }, data: req.body })
    res.json(item)
  } catch (err: any) {
    res.status(500).json({ error: err.message })
  }
})

entitiesRouter.delete('/items/:id', rejectIfReadOnly, async (req, res) => {
  try {
    await db().item.delete({ where: { id: req.params.id } })
    res.json({ ok: true })
  } catch (err: any) {
    res.status(500).json({ error: err.message })
  }
})

// ─── NPCs ──────────────────────────────────────────────────────

entitiesRouter.get('/npcs', async (req, res) => {
  try {
    const where = req.query.zoneId ? { zoneId: req.query.zoneId as string } : {}
    const npcs = await db().npc.findMany({ where })
    res.json(npcs)
  } catch (err: any) {
    res.status(500).json({ error: err.message })
  }
})

entitiesRouter.get('/npcs/:id', async (req, res) => {
  try {
    const npc = await db().npc.findUnique({ where: { id: req.params.id } })
    if (!npc) { res.status(404).json({ error: 'NPC not found' }); return }
    res.json(npc)
  } catch (err: any) {
    res.status(500).json({ error: err.message })
  }
})

entitiesRouter.post('/npcs', rejectIfReadOnly, async (req, res) => {
  try {
    const { id, zoneId } = req.body
    if (!id || !id.trim()) {
      res.status(400).json({ error: 'NPC id is required' }); return
    }
    if (!zoneId || !zoneId.trim()) {
      res.status(400).json({ error: 'zoneId is required — every NPC must belong to a zone' }); return
    }
    const zone = await db().zone.findUnique({ where: { id: zoneId } })
    if (!zone) {
      res.status(400).json({ error: `Zone "${zoneId}" does not exist` }); return
    }
    const npc = await db().npc.create({ data: req.body })
    res.json(npc)
  } catch (err: any) {
    res.status(500).json({ error: err.message })
  }
})

entitiesRouter.put('/npcs/:id', rejectIfReadOnly, async (req, res) => {
  try {
    const npc = await db().npc.update({ where: { id: req.params.id }, data: req.body })
    res.json(npc)
  } catch (err: any) {
    res.status(500).json({ error: err.message })
  }
})

entitiesRouter.delete('/npcs/:id', rejectIfReadOnly, async (req, res) => {
  try {
    await db().npc.delete({ where: { id: req.params.id } })
    res.json({ ok: true })
  } catch (err: any) {
    res.status(500).json({ error: err.message })
  }
})

// ─── Character Classes ─────────────────────────────────────────

entitiesRouter.get('/character-classes', async (_req, res) => {
  try {
    const classes = await db().characterClass.findMany()
    res.json(classes)
  } catch (err: any) {
    res.status(500).json({ error: err.message })
  }
})

entitiesRouter.get('/character-classes/:id', async (req, res) => {
  try {
    const cls = await db().characterClass.findUnique({ where: { id: req.params.id } })
    if (!cls) { res.status(404).json({ error: 'Class not found' }); return }
    res.json(cls)
  } catch (err: any) {
    res.status(500).json({ error: err.message })
  }
})

entitiesRouter.post('/character-classes', rejectIfReadOnly, async (req, res) => {
  try {
    const cls = await db().characterClass.create({ data: req.body })
    res.json(cls)
  } catch (err: any) {
    res.status(500).json({ error: err.message })
  }
})

entitiesRouter.put('/character-classes/:id', rejectIfReadOnly, async (req, res) => {
  try {
    const cls = await db().characterClass.update({ where: { id: req.params.id }, data: req.body })
    res.json(cls)
  } catch (err: any) {
    res.status(500).json({ error: err.message })
  }
})

entitiesRouter.delete('/character-classes/:id', rejectIfReadOnly, async (req, res) => {
  try {
    await db().characterClass.delete({ where: { id: req.params.id } })
    res.json({ ok: true })
  } catch (err: any) {
    res.status(500).json({ error: err.message })
  }
})

// ─── Races ─────────────────────────────────────────────────────

entitiesRouter.get('/races', async (_req, res) => {
  try {
    const races = await db().race.findMany()
    res.json(races)
  } catch (err: any) {
    res.status(500).json({ error: err.message })
  }
})

entitiesRouter.get('/races/:id', async (req, res) => {
  try {
    const race = await db().race.findUnique({ where: { id: req.params.id } })
    if (!race) { res.status(404).json({ error: 'Race not found' }); return }
    res.json(race)
  } catch (err: any) {
    res.status(500).json({ error: err.message })
  }
})

entitiesRouter.post('/races', rejectIfReadOnly, async (req, res) => {
  try {
    const race = await db().race.create({ data: req.body })
    res.json(race)
  } catch (err: any) {
    res.status(500).json({ error: err.message })
  }
})

entitiesRouter.put('/races/:id', rejectIfReadOnly, async (req, res) => {
  try {
    const race = await db().race.update({ where: { id: req.params.id }, data: req.body })
    res.json(race)
  } catch (err: any) {
    res.status(500).json({ error: err.message })
  }
})

entitiesRouter.delete('/races/:id', rejectIfReadOnly, async (req, res) => {
  try {
    await db().race.delete({ where: { id: req.params.id } })
    res.json({ ok: true })
  } catch (err: any) {
    res.status(500).json({ error: err.message })
  }
})

// ─── Skills ────────────────────────────────────────────────────

entitiesRouter.get('/skills', async (_req, res) => {
  try {
    const skills = await db().skill.findMany()
    res.json(skills)
  } catch (err: any) {
    res.status(500).json({ error: err.message })
  }
})

entitiesRouter.get('/skills/:id', async (req, res) => {
  try {
    const skill = await db().skill.findUnique({ where: { id: req.params.id } })
    if (!skill) { res.status(404).json({ error: 'Skill not found' }); return }
    res.json(skill)
  } catch (err: any) {
    res.status(500).json({ error: err.message })
  }
})

entitiesRouter.post('/skills', rejectIfReadOnly, async (req, res) => {
  try {
    const skill = await db().skill.create({ data: req.body })
    res.json(skill)
  } catch (err: any) {
    res.status(500).json({ error: err.message })
  }
})

entitiesRouter.put('/skills/:id', rejectIfReadOnly, async (req, res) => {
  try {
    const skill = await db().skill.update({ where: { id: req.params.id }, data: req.body })
    res.json(skill)
  } catch (err: any) {
    res.status(500).json({ error: err.message })
  }
})

entitiesRouter.delete('/skills/:id', rejectIfReadOnly, async (req, res) => {
  try {
    await db().skill.delete({ where: { id: req.params.id } })
    res.json({ ok: true })
  } catch (err: any) {
    res.status(500).json({ error: err.message })
  }
})

// ─── Spells ────────────────────────────────────────────────────

entitiesRouter.get('/spells', async (_req, res) => {
  try {
    const spells = await db().spell.findMany()
    res.json(spells)
  } catch (err: any) {
    res.status(500).json({ error: err.message })
  }
})

entitiesRouter.get('/spells/:id', async (req, res) => {
  try {
    const spell = await db().spell.findUnique({ where: { id: req.params.id } })
    if (!spell) { res.status(404).json({ error: 'Spell not found' }); return }
    res.json(spell)
  } catch (err: any) {
    res.status(500).json({ error: err.message })
  }
})

entitiesRouter.post('/spells', rejectIfReadOnly, async (req, res) => {
  try {
    const spell = await db().spell.create({ data: req.body })
    res.json(spell)
  } catch (err: any) {
    res.status(500).json({ error: err.message })
  }
})

entitiesRouter.put('/spells/:id', rejectIfReadOnly, async (req, res) => {
  try {
    const spell = await db().spell.update({ where: { id: req.params.id }, data: req.body })
    res.json(spell)
  } catch (err: any) {
    res.status(500).json({ error: err.message })
  }
})

entitiesRouter.delete('/spells/:id', rejectIfReadOnly, async (req, res) => {
  try {
    await db().spell.delete({ where: { id: req.params.id } })
    res.json({ ok: true })
  } catch (err: any) {
    res.status(500).json({ error: err.message })
  }
})

// ─── Loot Tables ───────────────────────────────────────────────

entitiesRouter.get('/loot-tables', async (_req, res) => {
  try {
    const tables = await db().lootTable.findMany()
    res.json(tables)
  } catch (err: any) {
    res.status(500).json({ error: err.message })
  }
})

entitiesRouter.get('/loot-tables/:id', async (req, res) => {
  try {
    const table = await db().lootTable.findUnique({ where: { id: req.params.id } })
    if (!table) { res.status(404).json({ error: 'Loot table not found' }); return }
    res.json(table)
  } catch (err: any) {
    res.status(500).json({ error: err.message })
  }
})

entitiesRouter.post('/loot-tables', rejectIfReadOnly, async (req, res) => {
  try {
    const table = await db().lootTable.create({ data: req.body })
    res.json(table)
  } catch (err: any) {
    res.status(500).json({ error: err.message })
  }
})

entitiesRouter.put('/loot-tables/:id', rejectIfReadOnly, async (req, res) => {
  try {
    const table = await db().lootTable.update({ where: { id: req.params.id }, data: req.body })
    res.json(table)
  } catch (err: any) {
    res.status(500).json({ error: err.message })
  }
})

entitiesRouter.delete('/loot-tables/:id', rejectIfReadOnly, async (req, res) => {
  try {
    await db().lootTable.delete({ where: { id: req.params.id } })
    res.json({ ok: true })
  } catch (err: any) {
    res.status(500).json({ error: err.message })
  }
})

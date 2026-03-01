import { Router, Request, Response, NextFunction } from 'express'
import { db, isReadOnly } from '../db.js'
import { Prisma } from '../generated/prisma/client.js'

export const entitiesRouter = Router()

const VALID_ID_RE = /^[a-zA-Z0-9_:]+$/

function validateId(id: string | undefined, entityLabel: string, res: Response): boolean {
  if (!id || !id.trim()) {
    res.status(400).json({ error: `${entityLabel} ID is required` })
    return false
  }
  if (!VALID_ID_RE.test(id)) {
    res.status(400).json({ error: `${entityLabel} ID may only contain letters, numbers, underscores, and colons` })
    return false
  }
  return true
}

function validateName(name: string | undefined, entityLabel: string, res: Response): boolean {
  if (!name || !name.trim()) {
    res.status(400).json({ error: `${entityLabel} name is required` })
    return false
  }
  return true
}

function articleFor(label: string): string {
  if (/^[aeiou]/i.test(label)) return 'An'
  if (/^[A-Z]{2,}/.test(label) && /^[AEFHILMNORSX]/i.test(label)) return 'An'
  return 'A'
}

interface NumericRule {
  field: string;
  label: string;
  min: number;
}

const NPC_NUMERIC_RULES: NumericRule[] = [
  { field: 'level', label: 'Level', min: 1 },
  { field: 'maxHp', label: 'Max HP', min: 0 },
  { field: 'damage', label: 'Damage', min: 0 },
  { field: 'accuracy', label: 'Accuracy', min: 0 },
  { field: 'defense', label: 'Defense', min: 0 },
  { field: 'evasion', label: 'Evasion', min: 0 },
  { field: 'agility', label: 'Agility', min: 0 },
  { field: 'perception', label: 'Perception', min: 0 },
  { field: 'xpReward', label: 'XP Reward', min: 0 },
]

const ITEM_NUMERIC_RULES: NumericRule[] = [
  { field: 'value', label: 'Value', min: 0 },
  { field: 'weight', label: 'Weight', min: 0 },
  { field: 'damageBonus', label: 'Damage Bonus', min: 0 },
  { field: 'damageRange', label: 'Damage Range', min: 0 },
  { field: 'armorValue', label: 'Armor Value', min: 0 },
  { field: 'levelRequirement', label: 'Level Requirement', min: 0 },
  { field: 'maxStack', label: 'Max Stack', min: 1 },
]

function validateNumericRanges(body: Record<string, any>, rules: NumericRule[], res: Response): boolean {
  for (const rule of rules) {
    const val = body[rule.field]
    if (val !== undefined && val !== null && typeof val === 'number' && val < rule.min) {
      res.status(400).json({ error: `${rule.label} must be at least ${rule.min}` })
      return false
    }
  }
  return true
}

function handlePrismaError(err: unknown, entityLabel: string, res: Response) {
  if (err instanceof Prisma.PrismaClientKnownRequestError) {
    if (err.code === 'P2002') {
      res.status(409).json({ error: `${articleFor(entityLabel)} ${entityLabel} with that ID already exists` })
      return
    }
    if (err.code === 'P2025') {
      res.status(404).json({ error: `${entityLabel} not found` })
      return
    }
  }
  if (err instanceof Prisma.PrismaClientValidationError) {
    const msg = (err as any).message || ''
    const missing = msg.match(/Argument `(\w+)` is missing/)
    if (missing) {
      res.status(400).json({ error: `Required field '${missing[1]}' is missing` })
      return
    }
    res.status(400).json({ error: 'Invalid data — check required fields and types' })
    return
  }
  res.status(500).json({ error: (err as any)?.message || 'Internal server error' })
}

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
    handlePrismaError(err, 'entity', res)
  }
})

entitiesRouter.get('/items/:id', async (req, res) => {
  try {
    const item = await db().item.findUnique({ where: { id: req.params.id } })
    if (!item) { res.status(404).json({ error: 'Item not found' }); return }
    res.json(item)
  } catch (err: any) {
    handlePrismaError(err, 'entity', res)
  }
})

entitiesRouter.post('/items', rejectIfReadOnly, async (req, res) => {
  if (!validateId(req.body.id, 'Item', res)) return
  if (!validateName(req.body.name, 'Item', res)) return
  if (!validateNumericRanges(req.body, ITEM_NUMERIC_RULES, res)) return
  try {
    const item = await db().item.create({ data: req.body })
    res.json(item)
  } catch (err: any) {
    handlePrismaError(err, 'item', res)
  }
})

entitiesRouter.put('/items/:id', rejectIfReadOnly, async (req, res) => {
  if (!validateNumericRanges(req.body, ITEM_NUMERIC_RULES, res)) return
  try {
    const item = await db().item.update({ where: { id: req.params.id }, data: req.body })
    res.json(item)
  } catch (err: any) {
    handlePrismaError(err, 'entity', res)
  }
})

entitiesRouter.delete('/items/:id', rejectIfReadOnly, async (req, res) => {
  try {
    await db().item.delete({ where: { id: req.params.id } })
    res.json({ ok: true })
  } catch (err: any) {
    handlePrismaError(err, 'entity', res)
  }
})

// ─── NPCs ──────────────────────────────────────────────────────

entitiesRouter.get('/npcs', async (req, res) => {
  try {
    const where = req.query.zoneId ? { zoneId: req.query.zoneId as string } : {}
    const npcs = await db().npc.findMany({ where })
    res.json(npcs)
  } catch (err: any) {
    handlePrismaError(err, 'entity', res)
  }
})

entitiesRouter.get('/npcs/:id', async (req, res) => {
  try {
    const npc = await db().npc.findUnique({ where: { id: req.params.id } })
    if (!npc) { res.status(404).json({ error: 'NPC not found' }); return }
    res.json(npc)
  } catch (err: any) {
    handlePrismaError(err, 'entity', res)
  }
})

entitiesRouter.post('/npcs', rejectIfReadOnly, async (req, res) => {
  if (!validateId(req.body.id, 'NPC', res)) return
  if (!validateName(req.body.name, 'NPC', res)) return
  if (!validateNumericRanges(req.body, NPC_NUMERIC_RULES, res)) return
  try {
    const { zoneId } = req.body
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
    handlePrismaError(err, 'entity', res)
  }
})

entitiesRouter.put('/npcs/:id', rejectIfReadOnly, async (req, res) => {
  if (!validateNumericRanges(req.body, NPC_NUMERIC_RULES, res)) return
  try {
    const npc = await db().npc.update({ where: { id: req.params.id }, data: req.body })
    res.json(npc)
  } catch (err: any) {
    handlePrismaError(err, 'entity', res)
  }
})

entitiesRouter.delete('/npcs/:id', rejectIfReadOnly, async (req, res) => {
  try {
    await db().npc.delete({ where: { id: req.params.id } })
    res.json({ ok: true })
  } catch (err: any) {
    handlePrismaError(err, 'entity', res)
  }
})

// ─── Character Classes ─────────────────────────────────────────

entitiesRouter.get('/character-classes', async (_req, res) => {
  try {
    const classes = await db().characterClass.findMany()
    res.json(classes)
  } catch (err: any) {
    handlePrismaError(err, 'entity', res)
  }
})

entitiesRouter.get('/character-classes/:id', async (req, res) => {
  try {
    const cls = await db().characterClass.findUnique({ where: { id: req.params.id } })
    if (!cls) { res.status(404).json({ error: 'Class not found' }); return }
    res.json(cls)
  } catch (err: any) {
    handlePrismaError(err, 'entity', res)
  }
})

entitiesRouter.post('/character-classes', rejectIfReadOnly, async (req, res) => {
  if (!validateId(req.body.id, 'Class', res)) return
  if (!validateName(req.body.name, 'Class', res)) return
  try {
    const cls = await db().characterClass.create({ data: req.body })
    res.json(cls)
  } catch (err: any) {
    handlePrismaError(err, 'entity', res)
  }
})

entitiesRouter.put('/character-classes/:id', rejectIfReadOnly, async (req, res) => {
  try {
    const cls = await db().characterClass.update({ where: { id: req.params.id }, data: req.body })
    res.json(cls)
  } catch (err: any) {
    handlePrismaError(err, 'entity', res)
  }
})

entitiesRouter.delete('/character-classes/:id', rejectIfReadOnly, async (req, res) => {
  try {
    await db().characterClass.delete({ where: { id: req.params.id } })
    res.json({ ok: true })
  } catch (err: any) {
    handlePrismaError(err, 'entity', res)
  }
})

// ─── Races ─────────────────────────────────────────────────────

entitiesRouter.get('/races', async (_req, res) => {
  try {
    const races = await db().race.findMany()
    res.json(races)
  } catch (err: any) {
    handlePrismaError(err, 'entity', res)
  }
})

entitiesRouter.get('/races/:id', async (req, res) => {
  try {
    const race = await db().race.findUnique({ where: { id: req.params.id } })
    if (!race) { res.status(404).json({ error: 'Race not found' }); return }
    res.json(race)
  } catch (err: any) {
    handlePrismaError(err, 'entity', res)
  }
})

entitiesRouter.post('/races', rejectIfReadOnly, async (req, res) => {
  if (!validateId(req.body.id, 'Race', res)) return
  if (!validateName(req.body.name, 'Race', res)) return
  try {
    const race = await db().race.create({ data: req.body })
    res.json(race)
  } catch (err: any) {
    handlePrismaError(err, 'entity', res)
  }
})

entitiesRouter.put('/races/:id', rejectIfReadOnly, async (req, res) => {
  try {
    const race = await db().race.update({ where: { id: req.params.id }, data: req.body })
    res.json(race)
  } catch (err: any) {
    handlePrismaError(err, 'entity', res)
  }
})

entitiesRouter.delete('/races/:id', rejectIfReadOnly, async (req, res) => {
  try {
    await db().race.delete({ where: { id: req.params.id } })
    res.json({ ok: true })
  } catch (err: any) {
    handlePrismaError(err, 'entity', res)
  }
})

// ─── Skills ────────────────────────────────────────────────────

entitiesRouter.get('/skills', async (_req, res) => {
  try {
    const skills = await db().skill.findMany()
    res.json(skills)
  } catch (err: any) {
    handlePrismaError(err, 'entity', res)
  }
})

entitiesRouter.get('/skills/:id', async (req, res) => {
  try {
    const skill = await db().skill.findUnique({ where: { id: req.params.id } })
    if (!skill) { res.status(404).json({ error: 'Skill not found' }); return }
    res.json(skill)
  } catch (err: any) {
    handlePrismaError(err, 'entity', res)
  }
})

entitiesRouter.post('/skills', rejectIfReadOnly, async (req, res) => {
  if (!validateId(req.body.id, 'Skill', res)) return
  if (!validateName(req.body.name, 'Skill', res)) return
  try {
    const skill = await db().skill.create({ data: req.body })
    res.json(skill)
  } catch (err: any) {
    handlePrismaError(err, 'entity', res)
  }
})

entitiesRouter.put('/skills/:id', rejectIfReadOnly, async (req, res) => {
  try {
    const skill = await db().skill.update({ where: { id: req.params.id }, data: req.body })
    res.json(skill)
  } catch (err: any) {
    handlePrismaError(err, 'entity', res)
  }
})

entitiesRouter.delete('/skills/:id', rejectIfReadOnly, async (req, res) => {
  try {
    await db().skill.delete({ where: { id: req.params.id } })
    res.json({ ok: true })
  } catch (err: any) {
    handlePrismaError(err, 'entity', res)
  }
})

// ─── Spells ────────────────────────────────────────────────────

entitiesRouter.get('/spells', async (_req, res) => {
  try {
    const spells = await db().spell.findMany()
    res.json(spells)
  } catch (err: any) {
    handlePrismaError(err, 'entity', res)
  }
})

entitiesRouter.get('/spells/:id', async (req, res) => {
  try {
    const spell = await db().spell.findUnique({ where: { id: req.params.id } })
    if (!spell) { res.status(404).json({ error: 'Spell not found' }); return }
    res.json(spell)
  } catch (err: any) {
    handlePrismaError(err, 'entity', res)
  }
})

entitiesRouter.post('/spells', rejectIfReadOnly, async (req, res) => {
  if (!validateId(req.body.id, 'Spell', res)) return
  if (!validateName(req.body.name, 'Spell', res)) return
  try {
    const spell = await db().spell.create({ data: req.body })
    res.json(spell)
  } catch (err: any) {
    handlePrismaError(err, 'entity', res)
  }
})

entitiesRouter.put('/spells/:id', rejectIfReadOnly, async (req, res) => {
  try {
    const spell = await db().spell.update({ where: { id: req.params.id }, data: req.body })
    res.json(spell)
  } catch (err: any) {
    handlePrismaError(err, 'entity', res)
  }
})

entitiesRouter.delete('/spells/:id', rejectIfReadOnly, async (req, res) => {
  try {
    await db().spell.delete({ where: { id: req.params.id } })
    res.json({ ok: true })
  } catch (err: any) {
    handlePrismaError(err, 'entity', res)
  }
})


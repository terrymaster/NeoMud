import { describe, it, expect, beforeAll, afterAll } from 'vitest'
import request from 'supertest'
import type { Express } from 'express'
import { createTestApp } from './helpers.js'

let app: Express
let cleanup: () => Promise<void>

beforeAll(async () => {
  const ctx = await createTestApp()
  app = ctx.app
  cleanup = ctx.cleanup
})

afterAll(async () => {
  await cleanup()
})

// ─── Items ─────────────────────────────────────────────────────

describe('Items CRUD', () => {
  const itemData = {
    id: 'test_sword',
    name: 'Test Sword',
    description: 'A test weapon',
    type: 'WEAPON',
    slot: 'MAIN_HAND',
    damageBonus: 5,
    damageRange: 3,
  }

  it('GET /api/items returns empty list', async () => {
    const res = await request(app).get('/api/items')
    expect(res.status).toBe(200)
    expect(res.body).toEqual([])
  })

  it('POST /api/items rejects empty name', async () => {
    const res = await request(app).post('/api/items').send({ ...itemData, id: 'no_name', name: '' })
    expect(res.status).toBe(400)
    expect(res.body.error).toMatch(/name is required/i)
  })

  it('POST /api/items rejects whitespace-only name', async () => {
    const res = await request(app).post('/api/items').send({ ...itemData, id: 'no_name2', name: '   ' })
    expect(res.status).toBe(400)
    expect(res.body.error).toMatch(/name is required/i)
  })

  it('POST /api/items creates an item', async () => {
    const res = await request(app).post('/api/items').send(itemData)
    expect(res.status).toBe(200)
    expect(res.body.id).toBe('test_sword')
    expect(res.body.name).toBe('Test Sword')
    expect(res.body.damageBonus).toBe(5)
  })

  it('GET /api/items returns created item', async () => {
    const res = await request(app).get('/api/items')
    expect(res.status).toBe(200)
    expect(res.body).toHaveLength(1)
    expect(res.body[0].id).toBe('test_sword')
  })

  it('GET /api/items/:id returns item by ID', async () => {
    const res = await request(app).get('/api/items/test_sword')
    expect(res.status).toBe(200)
    expect(res.body.name).toBe('Test Sword')
  })

  it('GET /api/items/:id returns 404 for missing', async () => {
    const res = await request(app).get('/api/items/nonexistent')
    expect(res.status).toBe(404)
  })

  it('PUT /api/items/:id updates item', async () => {
    const res = await request(app)
      .put('/api/items/test_sword')
      .send({ name: 'Updated Sword', damageBonus: 10 })
    expect(res.status).toBe(200)
    expect(res.body.name).toBe('Updated Sword')
    expect(res.body.damageBonus).toBe(10)
  })

  it('DELETE /api/items/:id deletes item', async () => {
    const res = await request(app).delete('/api/items/test_sword')
    expect(res.status).toBe(200)
    expect(res.body.ok).toBe(true)

    const list = await request(app).get('/api/items')
    expect(list.body).toHaveLength(0)
  })
})

// ─── NPCs ──────────────────────────────────────────────────────

describe('NPCs CRUD', () => {
  // Need a zone first for NPC
  beforeAll(async () => {
    await request(app).post('/api/zones').send({
      id: 'npc_test_zone',
      name: 'NPC Test Zone',
      description: 'Zone for NPC tests',
    })
    await request(app).post('/api/zones/npc_test_zone/rooms').send({
      id: 'room1',
      name: 'Room 1',
      description: '',
      x: 0,
      y: 0,
    })
  })

  const npcData = {
    id: 'test_goblin',
    name: 'Test Goblin',
    description: 'A test NPC',
    zoneId: 'npc_test_zone',
    startRoomId: 'npc_test_zone:room1',
    behaviorType: 'aggressive',
    hostile: true,
    maxHp: 50,
    damage: 10,
    level: 3,
  }

  it('POST /api/npcs rejects missing id', async () => {
    const res = await request(app).post('/api/npcs').send({ ...npcData, id: '' })
    expect(res.status).toBe(400)
    expect(res.body.error).toMatch(/id is required/i)
  })

  it('POST /api/npcs rejects empty name', async () => {
    const res = await request(app).post('/api/npcs').send({ ...npcData, name: '' })
    expect(res.status).toBe(400)
    expect(res.body.error).toMatch(/name is required/i)
  })

  it('POST /api/npcs rejects missing zoneId', async () => {
    const res = await request(app).post('/api/npcs').send({ ...npcData, zoneId: '' })
    expect(res.status).toBe(400)
    expect(res.body.error).toMatch(/zoneId is required/i)
  })

  it('POST /api/npcs rejects nonexistent zone', async () => {
    const res = await request(app).post('/api/npcs').send({ ...npcData, zoneId: 'no_such_zone' })
    expect(res.status).toBe(400)
    expect(res.body.error).toMatch(/does not exist/i)
  })

  it('POST /api/npcs creates an NPC', async () => {
    const res = await request(app).post('/api/npcs').send(npcData)
    expect(res.status).toBe(200)
    expect(res.body.id).toBe('test_goblin')
    expect(res.body.hostile).toBe(true)
  })

  it('GET /api/npcs returns list', async () => {
    const res = await request(app).get('/api/npcs')
    expect(res.status).toBe(200)
    expect(res.body).toHaveLength(1)
  })

  it('GET /api/npcs?zoneId= filters by zone', async () => {
    const res = await request(app).get('/api/npcs?zoneId=npc_test_zone')
    expect(res.status).toBe(200)
    expect(res.body).toHaveLength(1)

    const res2 = await request(app).get('/api/npcs?zoneId=nonexistent')
    expect(res2.body).toHaveLength(0)
  })

  it('GET /api/npcs/:id returns NPC', async () => {
    const res = await request(app).get('/api/npcs/test_goblin')
    expect(res.status).toBe(200)
    expect(res.body.name).toBe('Test Goblin')
  })

  it('PUT /api/npcs/:id updates NPC', async () => {
    const res = await request(app)
      .put('/api/npcs/test_goblin')
      .send({ name: 'Updated Goblin', maxHp: 100 })
    expect(res.status).toBe(200)
    expect(res.body.name).toBe('Updated Goblin')
    expect(res.body.maxHp).toBe(100)
  })

  it('DELETE /api/npcs/:id deletes NPC', async () => {
    const res = await request(app).delete('/api/npcs/test_goblin')
    expect(res.status).toBe(200)
    const list = await request(app).get('/api/npcs')
    expect(list.body).toHaveLength(0)
  })
})

// ─── Character Classes ─────────────────────────────────────────

describe('Character Classes CRUD', () => {
  const classData = {
    id: 'test_warrior',
    name: 'Test Warrior',
    description: 'A test class',
    minimumStats: '{"strength":12}',
    hpPerLevelMin: 6,
    hpPerLevelMax: 12,
  }

  it('POST /api/character-classes creates a class', async () => {
    const res = await request(app).post('/api/character-classes').send(classData)
    expect(res.status).toBe(200)
    expect(res.body.id).toBe('test_warrior')
  })

  it('GET /api/character-classes returns list', async () => {
    const res = await request(app).get('/api/character-classes')
    expect(res.status).toBe(200)
    expect(res.body).toHaveLength(1)
  })

  it('GET /api/character-classes/:id returns class', async () => {
    const res = await request(app).get('/api/character-classes/test_warrior')
    expect(res.status).toBe(200)
    expect(res.body.name).toBe('Test Warrior')
  })

  it('PUT /api/character-classes/:id updates', async () => {
    const res = await request(app)
      .put('/api/character-classes/test_warrior')
      .send({ name: 'Updated Warrior' })
    expect(res.status).toBe(200)
    expect(res.body.name).toBe('Updated Warrior')
  })

  it('DELETE /api/character-classes/:id deletes', async () => {
    const res = await request(app).delete('/api/character-classes/test_warrior')
    expect(res.status).toBe(200)
    const list = await request(app).get('/api/character-classes')
    expect(list.body).toHaveLength(0)
  })
})

// ─── Races ─────────────────────────────────────────────────────

describe('Races CRUD', () => {
  const raceData = {
    id: 'test_elf',
    name: 'Test Elf',
    description: 'A test race',
    statModifiers: '{"agility":2}',
  }

  it('POST /api/races creates', async () => {
    const res = await request(app).post('/api/races').send(raceData)
    expect(res.status).toBe(200)
    expect(res.body.id).toBe('test_elf')
  })

  it('GET /api/races returns list', async () => {
    const res = await request(app).get('/api/races')
    expect(res.body).toHaveLength(1)
  })

  it('GET /api/races/:id returns race', async () => {
    const res = await request(app).get('/api/races/test_elf')
    expect(res.status).toBe(200)
  })

  it('PUT /api/races/:id updates', async () => {
    const res = await request(app).put('/api/races/test_elf').send({ name: 'Updated Elf' })
    expect(res.status).toBe(200)
    expect(res.body.name).toBe('Updated Elf')
  })

  it('DELETE /api/races/:id deletes', async () => {
    const res = await request(app).delete('/api/races/test_elf')
    expect(res.status).toBe(200)
    const list = await request(app).get('/api/races')
    expect(list.body).toHaveLength(0)
  })
})

// ─── Skills ────────────────────────────────────────────────────

describe('Skills CRUD', () => {
  const skillData = {
    id: 'test_slash',
    name: 'Test Slash',
    description: 'A test skill',
    category: 'combat',
    primaryStat: 'strength',
    secondaryStat: 'agility',
  }

  it('POST /api/skills creates', async () => {
    const res = await request(app).post('/api/skills').send(skillData)
    expect(res.status).toBe(200)
    expect(res.body.id).toBe('test_slash')
  })

  it('GET /api/skills returns list', async () => {
    const res = await request(app).get('/api/skills')
    expect(res.body).toHaveLength(1)
  })

  it('GET /api/skills/:id returns skill', async () => {
    const res = await request(app).get('/api/skills/test_slash')
    expect(res.status).toBe(200)
  })

  it('PUT /api/skills/:id updates', async () => {
    const res = await request(app).put('/api/skills/test_slash').send({ name: 'Updated Slash' })
    expect(res.status).toBe(200)
    expect(res.body.name).toBe('Updated Slash')
  })

  it('DELETE /api/skills/:id deletes', async () => {
    const res = await request(app).delete('/api/skills/test_slash')
    expect(res.status).toBe(200)
    const list = await request(app).get('/api/skills')
    expect(list.body).toHaveLength(0)
  })
})

// ─── Spells ────────────────────────────────────────────────────

describe('Spells CRUD', () => {
  const spellData = {
    id: 'test_fireball',
    name: 'Test Fireball',
    description: 'A test spell',
    school: 'fire',
    spellType: 'damage',
    manaCost: 20,
    targetType: 'ENEMY',
  }

  it('POST /api/spells creates', async () => {
    const res = await request(app).post('/api/spells').send(spellData)
    expect(res.status).toBe(200)
    expect(res.body.id).toBe('test_fireball')
  })

  it('GET /api/spells returns list', async () => {
    const res = await request(app).get('/api/spells')
    expect(res.body).toHaveLength(1)
  })

  it('GET /api/spells/:id returns spell', async () => {
    const res = await request(app).get('/api/spells/test_fireball')
    expect(res.status).toBe(200)
  })

  it('PUT /api/spells/:id updates', async () => {
    const res = await request(app).put('/api/spells/test_fireball').send({ name: 'Updated Fireball' })
    expect(res.status).toBe(200)
    expect(res.body.name).toBe('Updated Fireball')
  })

  it('DELETE /api/spells/:id deletes', async () => {
    const res = await request(app).delete('/api/spells/test_fireball')
    expect(res.status).toBe(200)
    const list = await request(app).get('/api/spells')
    expect(list.body).toHaveLength(0)
  })
})


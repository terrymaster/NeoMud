import { describe, it, expect, beforeAll, afterAll } from 'vitest'
import request from 'supertest'
import type { Express } from 'express'
import { createTestApp, createReadOnlyTestApp } from './helpers.js'

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

describe('Default SFX CRUD', () => {
  it('GET /api/default-sfx returns all 16 seeded entries', async () => {
    const res = await request(app).get('/api/default-sfx')
    expect(res.status).toBe(200)
    expect(res.body).toHaveLength(16)
  })

  it('entries are sorted by id ascending', async () => {
    const res = await request(app).get('/api/default-sfx')
    const ids = res.body.map((e: any) => e.id)
    const sorted = [...ids].sort()
    expect(ids).toEqual(sorted)
  })

  it('each entry has required fields', async () => {
    const res = await request(app).get('/api/default-sfx')
    const first = res.body[0]
    expect(first).toHaveProperty('id')
    expect(first).toHaveProperty('label')
    expect(first).toHaveProperty('category')
    expect(first).toHaveProperty('description')
    expect(first).toHaveProperty('prompt')
    expect(first).toHaveProperty('duration')
  })

  // ─── Filtering ──────────────────────────────────────────────

  it('GET /api/default-sfx?category=combat returns combat entries', async () => {
    const res = await request(app).get('/api/default-sfx?category=combat')
    expect(res.status).toBe(200)
    expect(res.body).toHaveLength(5)
    expect(res.body.every((e: any) => e.category === 'combat')).toBe(true)
  })

  it('GET /api/default-sfx?category=movement returns 6 footstep entries', async () => {
    const res = await request(app).get('/api/default-sfx?category=movement')
    expect(res.status).toBe(200)
    expect(res.body).toHaveLength(6)
    expect(res.body.every((e: any) => e.category === 'movement')).toBe(true)
  })

  it('GET /api/default-sfx?category=loot returns loot entries', async () => {
    const res = await request(app).get('/api/default-sfx?category=loot')
    expect(res.status).toBe(200)
    expect(res.body).toHaveLength(3)
  })

  it('GET /api/default-sfx?category=item returns item entries', async () => {
    const res = await request(app).get('/api/default-sfx?category=item')
    expect(res.status).toBe(200)
    expect(res.body).toHaveLength(1)
    expect(res.body[0].id).toBe('potion_drink')
  })

  it('GET /api/default-sfx?category=magic returns magic entries', async () => {
    const res = await request(app).get('/api/default-sfx?category=magic')
    expect(res.status).toBe(200)
    expect(res.body).toHaveLength(1)
    expect(res.body[0].id).toBe('spell_fizzle')
  })

  it('filter with no matches returns empty array', async () => {
    const res = await request(app).get('/api/default-sfx?category=nonexistent')
    expect(res.status).toBe(200)
    expect(res.body).toHaveLength(0)
  })

  // ─── Get by ID ──────────────────────────────────────────────

  it('GET /api/default-sfx/:id returns a single entry', async () => {
    const res = await request(app).get('/api/default-sfx/dodge')
    expect(res.status).toBe(200)
    expect(res.body.id).toBe('dodge')
    expect(res.body.label).toBe('Dodge')
    expect(res.body.category).toBe('combat')
    expect(res.body.prompt).toContain('dodge')
  })

  it('GET /api/default-sfx/:id returns 404 for unknown id', async () => {
    const res = await request(app).get('/api/default-sfx/nonexistent')
    expect(res.status).toBe(404)
  })

  // ─── Update ─────────────────────────────────────────────────

  it('PUT /api/default-sfx/:id updates prompt', async () => {
    const res = await request(app)
      .put('/api/default-sfx/dodge')
      .send({ prompt: 'custom dodge whoosh sound' })
    expect(res.status).toBe(200)
    expect(res.body.prompt).toBe('custom dodge whoosh sound')
    expect(res.body.id).toBe('dodge') // other fields unchanged
    expect(res.body.label).toBe('Dodge')
  })

  it('PUT /api/default-sfx/:id updates duration', async () => {
    const res = await request(app)
      .put('/api/default-sfx/parry')
      .send({ duration: 5 })
    expect(res.status).toBe(200)
    expect(res.body.duration).toBe(5)
  })

  it('PUT /api/default-sfx/:id updates both prompt and duration', async () => {
    const res = await request(app)
      .put('/api/default-sfx/miss')
      .send({ prompt: 'new miss sound', duration: 4 })
    expect(res.status).toBe(200)
    expect(res.body.prompt).toBe('new miss sound')
    expect(res.body.duration).toBe(4)
  })

  it('PUT /api/default-sfx/:id with partial fields only updates those', async () => {
    // Set a known state
    await request(app)
      .put('/api/default-sfx/backstab')
      .send({ prompt: 'original prompt', duration: 7 })

    // Update only duration
    const res = await request(app)
      .put('/api/default-sfx/backstab')
      .send({ duration: 10 })
    expect(res.status).toBe(200)
    expect(res.body.duration).toBe(10)
    expect(res.body.prompt).toBe('original prompt') // unchanged
  })

  it('PUT /api/default-sfx/:id returns 404 for unknown id', async () => {
    const res = await request(app)
      .put('/api/default-sfx/nonexistent')
      .send({ prompt: 'test' })
    expect(res.status).toBe(404)
  })

  // ─── Reset ──────────────────────────────────────────────────

  it('POST /api/default-sfx/reset re-seeds all entries to defaults', async () => {
    // Modify one entry first
    await request(app)
      .put('/api/default-sfx/dodge')
      .send({ prompt: 'CUSTOM PROMPT' })

    // Reset
    const resetRes = await request(app).post('/api/default-sfx/reset')
    expect(resetRes.status).toBe(200)
    expect(resetRes.body.ok).toBe(true)
    expect(resetRes.body.count).toBe(16)

    // Verify the modified entry is back to default
    const getRes = await request(app).get('/api/default-sfx/dodge')
    expect(getRes.body.prompt).toContain('dodge')
    expect(getRes.body.prompt).not.toBe('CUSTOM PROMPT')
  })

  // ─── Category completeness ─────────────────────────────────

  it('all 5 categories are represented', async () => {
    const res = await request(app).get('/api/default-sfx')
    const categories = new Set(res.body.map((e: any) => e.category))
    expect(categories).toEqual(new Set(['combat', 'loot', 'item', 'magic', 'movement']))
  })

  it('all expected IDs are present', async () => {
    const res = await request(app).get('/api/default-sfx')
    const ids = new Set(res.body.map((e: any) => e.id))
    const expected = [
      'dodge', 'parry', 'miss', 'backstab', 'enemy_death',
      'coin_pickup', 'item_pickup', 'loot_drop',
      'potion_drink', 'spell_fizzle',
      'footstep_cobblestone', 'footstep_dirt', 'footstep_grass',
      'footstep_marble', 'footstep_splash', 'footstep_wood',
    ]
    for (const id of expected) {
      expect(ids).toContain(id)
    }
  })

  it('default prompts contain relevant keywords', async () => {
    const res = await request(app).get('/api/default-sfx/footstep_wood')
    expect(res.body.prompt).toContain('wood')
    expect(res.body.category).toBe('movement')
  })
})

// ─── Read-Only Project ──────────────────────────────────────────

describe('Default SFX read-only mode', () => {
  let roApp: Express
  let roCleanup: () => Promise<void>

  beforeAll(async () => {
    const ctx = await createReadOnlyTestApp()
    roApp = ctx.app
    roCleanup = ctx.cleanup
  })

  afterAll(async () => {
    await roCleanup()
  })

  it('GET /api/default-sfx works in read-only mode', async () => {
    const res = await request(roApp).get('/api/default-sfx')
    expect(res.status).toBe(200)
    expect(res.body).toHaveLength(16)
  })

  it('PUT /api/default-sfx/:id is rejected in read-only mode', async () => {
    const res = await request(roApp)
      .put('/api/default-sfx/dodge')
      .send({ prompt: 'Should fail' })
    expect(res.status).toBe(403)
  })

  it('POST /api/default-sfx/reset is rejected in read-only mode', async () => {
    const res = await request(roApp).post('/api/default-sfx/reset')
    expect(res.status).toBe(403)
  })
})

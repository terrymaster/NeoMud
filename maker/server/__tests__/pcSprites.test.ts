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

describe('PC Sprites CRUD', () => {
  it('GET /api/pc-sprites returns all 270 seeded sprites', async () => {
    const res = await request(app).get('/api/pc-sprites')
    expect(res.status).toBe(200)
    expect(res.body).toHaveLength(270)
  })

  it('sprites are sorted by id ascending', async () => {
    const res = await request(app).get('/api/pc-sprites')
    const ids = res.body.map((s: any) => s.id)
    const sorted = [...ids].sort()
    expect(ids).toEqual(sorted)
  })

  it('each sprite has required fields', async () => {
    const res = await request(app).get('/api/pc-sprites')
    const first = res.body[0]
    expect(first).toHaveProperty('id')
    expect(first).toHaveProperty('race')
    expect(first).toHaveProperty('gender')
    expect(first).toHaveProperty('characterClass')
    expect(first).toHaveProperty('imagePrompt')
    expect(first).toHaveProperty('imageStyle')
    expect(first).toHaveProperty('imageNegativePrompt')
    expect(first).toHaveProperty('imageWidth')
    expect(first).toHaveProperty('imageHeight')
  })

  it('default dimensions are 384x512', async () => {
    const res = await request(app).get('/api/pc-sprites/human_male_warrior')
    expect(res.status).toBe(200)
    expect(res.body.imageWidth).toBe(384)
    expect(res.body.imageHeight).toBe(512)
  })

  // ─── Filtering ──────────────────────────────────────────────

  it('GET /api/pc-sprites?race=HUMAN returns 45 sprites (3 genders × 15 classes)', async () => {
    const res = await request(app).get('/api/pc-sprites?race=HUMAN')
    expect(res.status).toBe(200)
    expect(res.body).toHaveLength(45)
    expect(res.body.every((s: any) => s.race === 'HUMAN')).toBe(true)
  })

  it('GET /api/pc-sprites?gender=male returns 90 sprites (6 races × 15 classes)', async () => {
    const res = await request(app).get('/api/pc-sprites?gender=male')
    expect(res.status).toBe(200)
    expect(res.body).toHaveLength(90)
    expect(res.body.every((s: any) => s.gender === 'male')).toBe(true)
  })

  it('GET /api/pc-sprites?class=WARRIOR returns 18 sprites (6 races × 3 genders)', async () => {
    const res = await request(app).get('/api/pc-sprites?class=WARRIOR')
    expect(res.status).toBe(200)
    expect(res.body).toHaveLength(18)
    expect(res.body.every((s: any) => s.characterClass === 'WARRIOR')).toBe(true)
  })

  it('combined filters narrow results: race=HUMAN&gender=male returns 15', async () => {
    const res = await request(app).get('/api/pc-sprites?race=HUMAN&gender=male')
    expect(res.status).toBe(200)
    expect(res.body).toHaveLength(15)
  })

  it('all three filters return exactly 1 result', async () => {
    const res = await request(app).get('/api/pc-sprites?race=ELF&gender=female&class=MAGE')
    expect(res.status).toBe(200)
    expect(res.body).toHaveLength(1)
    expect(res.body[0].id).toBe('elf_female_mage')
  })

  it('filter with no matches returns empty array', async () => {
    const res = await request(app).get('/api/pc-sprites?race=GOBLIN')
    expect(res.status).toBe(200)
    expect(res.body).toHaveLength(0)
  })

  // ─── Get by ID ──────────────────────────────────────────────

  it('GET /api/pc-sprites/:id returns a single sprite', async () => {
    const res = await request(app).get('/api/pc-sprites/dwarf_neutral_cleric')
    expect(res.status).toBe(200)
    expect(res.body.race).toBe('DWARF')
    expect(res.body.gender).toBe('neutral')
    expect(res.body.characterClass).toBe('CLERIC')
    expect(res.body.imagePrompt).toContain('dwarf')
  })

  it('GET /api/pc-sprites/:id returns 404 for unknown id', async () => {
    const res = await request(app).get('/api/pc-sprites/nonexistent_sprite')
    expect(res.status).toBe(404)
  })

  // ─── Update ─────────────────────────────────────────────────

  it('PUT /api/pc-sprites/:id updates image prompt', async () => {
    const res = await request(app)
      .put('/api/pc-sprites/human_male_warrior')
      .send({ imagePrompt: 'A mighty warrior with a gleaming sword' })
    expect(res.status).toBe(200)
    expect(res.body.imagePrompt).toBe('A mighty warrior with a gleaming sword')
    // Other fields unchanged
    expect(res.body.race).toBe('HUMAN')
    expect(res.body.gender).toBe('male')
  })

  it('PUT /api/pc-sprites/:id updates multiple fields at once', async () => {
    const res = await request(app)
      .put('/api/pc-sprites/elf_female_mage')
      .send({
        imagePrompt: 'An elegant elven sorceress',
        imageStyle: 'watercolor fantasy art',
        imageNegativePrompt: 'ugly, deformed',
        imageWidth: 512,
        imageHeight: 768,
      })
    expect(res.status).toBe(200)
    expect(res.body.imagePrompt).toBe('An elegant elven sorceress')
    expect(res.body.imageStyle).toBe('watercolor fantasy art')
    expect(res.body.imageNegativePrompt).toBe('ugly, deformed')
    expect(res.body.imageWidth).toBe(512)
    expect(res.body.imageHeight).toBe(768)
  })

  it('PUT /api/pc-sprites/:id with partial fields only updates those fields', async () => {
    // First set a known state
    await request(app)
      .put('/api/pc-sprites/gnome_male_thief')
      .send({ imagePrompt: 'Original prompt', imageStyle: 'Original style' })

    // Update only the style
    const res = await request(app)
      .put('/api/pc-sprites/gnome_male_thief')
      .send({ imageStyle: 'New style only' })
    expect(res.status).toBe(200)
    expect(res.body.imageStyle).toBe('New style only')
    expect(res.body.imagePrompt).toBe('Original prompt') // unchanged
  })

  it('PUT /api/pc-sprites/:id returns 404 for unknown id', async () => {
    const res = await request(app)
      .put('/api/pc-sprites/nonexistent_sprite')
      .send({ imagePrompt: 'test' })
    expect(res.status).toBe(404)
  })

  // ─── Reset ──────────────────────────────────────────────────

  it('POST /api/pc-sprites/reset re-seeds all sprites to defaults', async () => {
    // Modify one sprite first
    await request(app)
      .put('/api/pc-sprites/human_male_warrior')
      .send({ imagePrompt: 'CUSTOM PROMPT' })

    // Reset
    const resetRes = await request(app).post('/api/pc-sprites/reset')
    expect(resetRes.status).toBe(200)
    expect(resetRes.body.ok).toBe(true)
    expect(resetRes.body.count).toBe(270)

    // Verify the modified sprite is back to default
    const getRes = await request(app).get('/api/pc-sprites/human_male_warrior')
    expect(getRes.body.imagePrompt).toContain('human')
    expect(getRes.body.imagePrompt).not.toBe('CUSTOM PROMPT')
  })

  // ─── Combo completeness ─────────────────────────────────────

  it('all 6 races are represented', async () => {
    const res = await request(app).get('/api/pc-sprites')
    const races = new Set(res.body.map((s: any) => s.race))
    expect(races).toEqual(new Set(['HUMAN', 'DWARF', 'ELF', 'HALFLING', 'GNOME', 'HALF_ORC']))
  })

  it('all 3 genders are represented', async () => {
    const res = await request(app).get('/api/pc-sprites')
    const genders = new Set(res.body.map((s: any) => s.gender))
    expect(genders).toEqual(new Set(['male', 'female', 'neutral']))
  })

  it('all 15 classes are represented', async () => {
    const res = await request(app).get('/api/pc-sprites')
    const classes = new Set(res.body.map((s: any) => s.characterClass))
    expect(classes).toEqual(new Set([
      'WARRIOR', 'PALADIN', 'WITCHHUNTER', 'CLERIC', 'PRIEST', 'MISSIONARY',
      'MAGE', 'WARLOCK', 'DRUID', 'RANGER', 'THIEF', 'NINJA', 'MYSTIC', 'BARD', 'GYPSY',
    ]))
  })

  it('sprite IDs follow naming convention: {race}_{gender}_{class}', async () => {
    const res = await request(app).get('/api/pc-sprites')
    for (const sprite of res.body) {
      const expectedId = `${sprite.race.toLowerCase()}_${sprite.gender}_${sprite.characterClass.toLowerCase()}`
      expect(sprite.id).toBe(expectedId)
    }
  })

  it('default image prompts contain race and class info', async () => {
    // After reset, check a few sprites have sensible default prompts
    const res = await request(app).get('/api/pc-sprites/half_orc_female_ninja')
    expect(res.body.imagePrompt).toContain('half orc')
    expect(res.body.imagePrompt).toContain('ninja')
    expect(res.body.imagePrompt).toContain('female')
  })
})

// ─── Read-Only Project ──────────────────────────────────────────

describe('PC Sprites read-only mode', () => {
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

  it('GET /api/pc-sprites works in read-only mode', async () => {
    const res = await request(roApp).get('/api/pc-sprites')
    expect(res.status).toBe(200)
    expect(res.body).toHaveLength(270)
  })

  it('PUT /api/pc-sprites/:id is rejected in read-only mode', async () => {
    const res = await request(roApp)
      .put('/api/pc-sprites/human_male_warrior')
      .send({ imagePrompt: 'Should fail' })
    expect(res.status).toBe(403)
  })

  it('POST /api/pc-sprites/reset is rejected in read-only mode', async () => {
    const res = await request(roApp).post('/api/pc-sprites/reset')
    expect(res.status).toBe(403)
  })
})

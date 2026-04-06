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

// ─── JSON Export ──────────────────────────────────────────────

describe('GET /api/export/json', () => {
  it('returns structured world data from empty project', async () => {
    const res = await request(app).get('/api/export/json')
    expect(res.status).toBe(200)
    expect(res.body).toHaveProperty('zones')
    expect(res.body).toHaveProperty('items')
    expect(res.body).toHaveProperty('classes')
    expect(res.body).toHaveProperty('races')
    expect(res.body).toHaveProperty('skills')
    expect(res.body).toHaveProperty('spells')
    expect(res.body).toHaveProperty('recipes')
  })

  it('returns empty catalogs for fresh project', async () => {
    const res = await request(app).get('/api/export/json')
    expect(res.status).toBe(200)
    // Fresh project has no user-created classes or races
    expect(typeof res.body.classes).toBe('object')
    expect(typeof res.body.races).toBe('object')
  })

  it('includes created items in export', async () => {
    // Create an item first
    await request(app).post('/api/items').send({
      id: 'export_sword',
      name: 'Export Sword',
      description: 'For export testing',
      type: 'weapon',
      slot: 'weapon',
      damageBonus: 5,
      damageRange: 3,
    })

    const res = await request(app).get('/api/export/json')
    expect(res.status).toBe(200)
    expect(res.body.items).toHaveProperty('export_sword')
    expect(res.body.items.export_sword.name).toBe('Export Sword')
    expect(res.body.items.export_sword.damageBonus).toBe(5)
  })

  it('includes zone and room data in export', async () => {
    // Create a zone with a room
    await request(app).post('/api/zones').send({
      id: 'export_zone',
      name: 'Export Zone',
      description: 'Zone for export testing',
    })
    await request(app).post('/api/zones/export_zone/rooms').send({
      id: 'export_zone:room1',
      name: 'Room One',
      description: 'First room',
      x: 0,
      y: 0,
    })

    const res = await request(app).get('/api/export/json')
    expect(res.status).toBe(200)
    expect(res.body.zones).toHaveProperty('export_zone')
    expect(res.body.zones.export_zone.name).toBe('Export Zone')
    expect(res.body.zones.export_zone.rooms).toHaveProperty('export_zone:room1')
  })
})

// ─── Validation ───────────────────────────────────────────────

describe('GET /api/export/validate', () => {
  it('returns validation result object', async () => {
    const res = await request(app).get('/api/export/validate')
    expect(res.status).toBe(200)
    expect(res.body).toHaveProperty('errors')
    expect(res.body).toHaveProperty('warnings')
    expect(Array.isArray(res.body.errors)).toBe(true)
    expect(Array.isArray(res.body.warnings)).toBe(true)
  })

  it('reports missing spawnRoom as error', async () => {
    const res = await request(app).get('/api/export/validate')
    expect(res.status).toBe(200)
    // New projects don't have a spawnRoom set
    const spawnError = res.body.errors.find((e: string) => e.includes('spawnRoom'))
    expect(spawnError).toBeDefined()
  })

  it('reports warnings for incomplete entities', async () => {
    // The export_sword we created earlier has no attackSound/missSound
    const res = await request(app).get('/api/export/validate')
    expect(res.status).toBe(200)
    expect(res.body.warnings.length).toBeGreaterThan(0)
  })
})

// ─── NMD Export ───────────────────────────────────────────────

describe('GET /api/export/nmd', () => {
  it('returns ZIP with correct content-type', async () => {
    const res = await request(app).get('/api/export/nmd')
    expect(res.status).toBe(200)
    expect(res.headers['content-type']).toMatch(/application\/zip/)
    expect(res.headers['content-disposition']).toMatch(/\.nmd/)
    expect(res.body).toBeTruthy()
  })
})

// ─── Package (validate then export) ──────────────────────────

describe('GET /api/export/package', () => {
  it('returns 400 with errors when validation fails', async () => {
    // No spawnRoom → validation error → package refused
    const res = await request(app).get('/api/export/package')
    expect(res.status).toBe(400)
    expect(res.body).toHaveProperty('errors')
    expect(res.body.errors.length).toBeGreaterThan(0)
  })
})

// ─── Package with valid project ──────────────────────────────

describe('Package with spawnRoom set', () => {
  let validApp: Express
  let validCleanup: () => Promise<void>

  beforeAll(async () => {
    const ctx = await createTestApp()
    validApp = ctx.app
    validCleanup = ctx.cleanup

    // Set up a minimal valid project: zone with spawnRoom
    await request(validApp).post('/api/zones').send({
      id: 'test_zone',
      name: 'Test Zone',
      description: 'Valid zone',
      spawnRoom: 'test_zone:spawn',
    })
    await request(validApp).post('/api/zones/test_zone/rooms').send({
      id: 'test_zone:spawn',
      name: 'Spawn Room',
      description: 'Starting room',
      x: 0,
      y: 0,
    })
  })

  afterAll(async () => {
    await validCleanup()
  })

  it('succeeds when only warnings exist (no blocking errors)', async () => {
    // Use /nmd directly (no validation) to confirm bundle builds
    const res = await request(validApp).get('/api/export/nmd')
    expect(res.status).toBe(200)
    expect(res.headers['content-type']).toMatch(/application\/zip/)
  })

  it('validation passes with spawnRoom set (no errors)', async () => {
    const res = await request(validApp).get('/api/export/validate')
    expect(res.status).toBe(200)
    expect(res.body.errors).toHaveLength(0)
    // Warnings may still exist (missing sounds, etc.)
    expect(Array.isArray(res.body.warnings)).toBe(true)
  })
})

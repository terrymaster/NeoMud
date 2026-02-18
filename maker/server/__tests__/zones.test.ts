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

describe('Zone CRUD', () => {
  it('GET /api/zones returns empty list', async () => {
    const res = await request(app).get('/api/zones')
    expect(res.status).toBe(200)
    expect(res.body).toEqual([])
  })

  it('POST /api/zones creates a zone', async () => {
    const res = await request(app).post('/api/zones').send({
      id: 'forest',
      name: 'Dark Forest',
      description: 'A spooky forest',
    })
    expect(res.status).toBe(200)
    expect(res.body.id).toBe('forest')
    expect(res.body.name).toBe('Dark Forest')
    expect(res.body.safe).toBe(true) // default
  })

  it('GET /api/zones lists created zone', async () => {
    const res = await request(app).get('/api/zones')
    expect(res.body).toHaveLength(1)
    expect(res.body[0].id).toBe('forest')
  })

  it('GET /api/zones/:id returns zone with rooms', async () => {
    const res = await request(app).get('/api/zones/forest')
    expect(res.status).toBe(200)
    expect(res.body.id).toBe('forest')
    expect(res.body.rooms).toEqual([])
  })

  it('GET /api/zones/:id returns 404 for missing', async () => {
    const res = await request(app).get('/api/zones/nonexistent')
    expect(res.status).toBe(404)
  })

  it('PUT /api/zones/:id updates zone', async () => {
    const res = await request(app)
      .put('/api/zones/forest')
      .send({ name: 'Updated Forest', safe: false })
    expect(res.status).toBe(200)
    expect(res.body.name).toBe('Updated Forest')
    expect(res.body.safe).toBe(false)
  })
})

describe('Room CRUD', () => {
  it('POST /api/zones/:zoneId/rooms creates a room', async () => {
    const res = await request(app).post('/api/zones/forest/rooms').send({
      id: 'clearing',
      name: 'Forest Clearing',
      description: 'A sunlit clearing',
      x: 0,
      y: 0,
    })
    expect(res.status).toBe(200)
    expect(res.body.id).toBe('forest:clearing')
    expect(res.body.name).toBe('Forest Clearing')
  })

  it('POST creates a second room', async () => {
    const res = await request(app).post('/api/zones/forest/rooms').send({
      id: 'cave',
      name: 'Dark Cave',
      description: 'A dark cave entrance',
      x: 1,
      y: 0,
    })
    expect(res.status).toBe(200)
    expect(res.body.id).toBe('forest:cave')
  })

  it('GET /api/zones/:zoneId/rooms lists rooms', async () => {
    const res = await request(app).get('/api/zones/forest/rooms')
    expect(res.status).toBe(200)
    expect(res.body).toHaveLength(2)
  })

  it('GET /api/zones/:zoneId/rooms/:id returns room', async () => {
    const res = await request(app).get('/api/zones/forest/rooms/clearing')
    expect(res.status).toBe(200)
    expect(res.body.name).toBe('Forest Clearing')
  })

  it('PUT /api/zones/:zoneId/rooms/:id updates room', async () => {
    const res = await request(app)
      .put('/api/zones/forest/rooms/clearing')
      .send({ name: 'Updated Clearing' })
    expect(res.status).toBe(200)
    expect(res.body.name).toBe('Updated Clearing')
  })
})

describe('Exit CRUD', () => {
  it('POST /api/rooms/:id/exits creates exit + reverse', async () => {
    const res = await request(app).post('/api/rooms/forest:clearing/exits').send({
      direction: 'EAST',
      toRoomId: 'forest:cave',
    })
    expect(res.status).toBe(200)
    expect(res.body.direction).toBe('EAST')
    expect(res.body.toRoomId).toBe('forest:cave')

    // Verify reverse exit was created
    const zone = await request(app).get('/api/zones/forest')
    const cave = zone.body.rooms.find((r: any) => r.id === 'forest:cave')
    const westExit = cave.exits.find((e: any) => e.direction === 'WEST')
    expect(westExit).toBeDefined()
    expect(westExit.toRoomId).toBe('forest:clearing')
  })

  it('DELETE /api/rooms/:id/exits/:dir deletes exit + reverse', async () => {
    const res = await request(app).delete('/api/rooms/forest:clearing/exits/EAST')
    expect(res.status).toBe(200)

    // Verify both exits are gone
    const zone = await request(app).get('/api/zones/forest')
    const clearing = zone.body.rooms.find((r: any) => r.id === 'forest:clearing')
    expect(clearing.exits).toHaveLength(0)
    const cave = zone.body.rooms.find((r: any) => r.id === 'forest:cave')
    expect(cave.exits).toHaveLength(0)
  })

  it('DELETE /api/rooms/:id/exits/:dir returns 404 for missing', async () => {
    const res = await request(app).delete('/api/rooms/forest:clearing/exits/NORTH')
    expect(res.status).toBe(404)
  })
})

describe('Zone BGM prompt fields', () => {
  it('POST /api/zones persists bgmPrompt and bgmDuration', async () => {
    const res = await request(app).post('/api/zones').send({
      id: 'bgm_test_zone',
      name: 'BGM Test Zone',
      description: 'Testing BGM fields',
      bgmPrompt: 'Medieval town ambiance',
      bgmDuration: 60,
    })
    expect(res.status).toBe(200)
    expect(res.body.bgmPrompt).toBe('Medieval town ambiance')
    expect(res.body.bgmDuration).toBe(60)
  })

  it('GET /api/zones/:id returns bgmPrompt and bgmDuration', async () => {
    const res = await request(app).get('/api/zones/bgm_test_zone')
    expect(res.status).toBe(200)
    expect(res.body.bgmPrompt).toBe('Medieval town ambiance')
    expect(res.body.bgmDuration).toBe(60)
  })

  it('PUT /api/zones/:id updates bgmPrompt and bgmDuration', async () => {
    const res = await request(app)
      .put('/api/zones/bgm_test_zone')
      .send({ bgmPrompt: 'Updated prompt', bgmDuration: 90 })
    expect(res.status).toBe(200)
    expect(res.body.bgmPrompt).toBe('Updated prompt')
    expect(res.body.bgmDuration).toBe(90)
  })

  it('Zone bgmPrompt defaults to empty string when not provided', async () => {
    const res = await request(app).post('/api/zones').send({
      id: 'bgm_default_zone',
      name: 'Default BGM Zone',
      description: 'No BGM fields provided',
    })
    expect(res.status).toBe(200)
    expect(res.body.bgmPrompt).toBe('')
    expect(res.body.bgmDuration).toBe(120)
  })
})

describe('Room BGM prompt fields', () => {
  it('POST /api/zones/:zoneId/rooms persists room bgmPrompt and bgmDuration', async () => {
    const res = await request(app).post('/api/zones/bgm_test_zone/rooms').send({
      id: 'bgm_room',
      name: 'BGM Room',
      description: 'A room with BGM prompt',
      x: 0,
      y: 0,
      bgmPrompt: 'Spooky cave echoes',
      bgmDuration: 30,
    })
    expect(res.status).toBe(200)
    expect(res.body.bgmPrompt).toBe('Spooky cave echoes')
    expect(res.body.bgmDuration).toBe(30)
  })

  it('GET /api/zones/:zoneId/rooms/:id returns room bgm prompt fields', async () => {
    const res = await request(app).get('/api/zones/bgm_test_zone/rooms/bgm_room')
    expect(res.status).toBe(200)
    expect(res.body.bgmPrompt).toBe('Spooky cave echoes')
    expect(res.body.bgmDuration).toBe(30)
  })

  it('PUT /api/zones/:zoneId/rooms/:id updates room bgm prompt fields', async () => {
    const res = await request(app)
      .put('/api/zones/bgm_test_zone/rooms/bgm_room')
      .send({ bgmPrompt: 'Updated room prompt', bgmDuration: 45 })
    expect(res.status).toBe(200)
    expect(res.body.bgmPrompt).toBe('Updated room prompt')
    expect(res.body.bgmDuration).toBe(45)
  })

  it('Room bgmPrompt defaults to empty and bgmDuration to 0', async () => {
    const res = await request(app).post('/api/zones/bgm_test_zone/rooms').send({
      id: 'default_room',
      name: 'Default Room',
      description: 'No BGM fields',
      x: 1,
      y: 0,
    })
    expect(res.status).toBe(200)
    expect(res.body.bgmPrompt).toBe('')
    expect(res.body.bgmDuration).toBe(0)
  })
})

describe('Room + Zone deletion', () => {
  it('DELETE room', async () => {
    const res = await request(app).delete('/api/zones/forest/rooms/cave')
    expect(res.status).toBe(200)
    const rooms = await request(app).get('/api/zones/forest/rooms')
    expect(rooms.body).toHaveLength(1)
  })

  it('DELETE zone cascades rooms', async () => {
    await request(app).delete('/api/zones/bgm_test_zone')
    await request(app).delete('/api/zones/bgm_default_zone')
    const res = await request(app).delete('/api/zones/forest')
    expect(res.status).toBe(200)
    const zones = await request(app).get('/api/zones')
    expect(zones.body).toHaveLength(0)
  })
})

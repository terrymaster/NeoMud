import { describe, it, expect, beforeAll, afterAll } from 'vitest'
import request from 'supertest'
import type { Express } from 'express'
import { createTestApp, createReadOnlyTestApp } from './helpers.js'

// ─── Project Management ────────────────────────────────────────

describe('Project management', () => {
  let app: Express
  let projectName: string
  let cleanup: () => Promise<void>

  beforeAll(async () => {
    const ctx = await createTestApp()
    app = ctx.app
    projectName = ctx.projectName
    cleanup = ctx.cleanup
  })

  afterAll(async () => {
    await cleanup()
  })

  it('GET /api/projects lists projects including the test project', async () => {
    const res = await request(app).get('/api/projects')
    expect(res.status).toBe(200)
    expect(res.body.projects).toBeDefined()
    expect(Array.isArray(res.body.projects)).toBe(true)
    const found = res.body.projects.find((p: any) => p.name === projectName)
    expect(found).toBeDefined()
  })

  it('GET /api/projects shows active project', async () => {
    const res = await request(app).get('/api/projects')
    expect(res.body.active).toBe(projectName)
  })

  it('POST /api/projects creates a new project', async () => {
    const newName = `proj_create_test_${Date.now()}`
    const res = await request(app).post('/api/projects').send({ name: newName })
    expect(res.status).toBe(200)
    expect(res.body.name).toBe(newName)

    // Clean up: switch back to our test project and delete the new one
    await request(app).post(`/api/projects/${projectName}/open`)
    await request(app).delete(`/api/projects/${newName}`)
  })

  it('POST /api/projects without name returns 400', async () => {
    const res = await request(app).post('/api/projects').send({})
    expect(res.status).toBe(400)
  })

  it('POST /api/projects/:name/open opens a project', async () => {
    const res = await request(app).post(`/api/projects/${projectName}/open`)
    expect(res.status).toBe(200)
    expect(res.body.name).toBe(projectName)
  })

  it('POST /api/projects/:name/fork forks a project', async () => {
    const forkName = `fork_${Date.now()}`
    const res = await request(app)
      .post(`/api/projects/${projectName}/fork`)
      .send({ newName: forkName })
    expect(res.status).toBe(200)
    expect(res.body.name).toBe(forkName)

    // Verify the fork exists
    await request(app).post(`/api/projects/${projectName}/open`)
    const list = await request(app).get('/api/projects')
    const found = list.body.projects.find((p: any) => p.name === forkName)
    expect(found).toBeDefined()

    // Clean up
    await request(app).delete(`/api/projects/${forkName}`)
  })

  it('DELETE /api/projects/:name deletes a project', async () => {
    const tempName = `del_test_${Date.now()}`
    await request(app).post('/api/projects').send({ name: tempName })
    await request(app).post(`/api/projects/${projectName}/open`)

    const res = await request(app).delete(`/api/projects/${tempName}`)
    expect(res.status).toBe(200)
  })
})

// ─── Read-Only Guards ──────────────────────────────────────────

describe('Read-only project guards', () => {
  let app: Express
  let cleanup: () => Promise<void>

  beforeAll(async () => {
    const ctx = await createReadOnlyTestApp()
    app = ctx.app
    cleanup = ctx.cleanup
  })

  afterAll(async () => {
    await cleanup()
  })

  it('POST /api/zones returns 403 on read-only project', async () => {
    const res = await request(app).post('/api/zones').send({
      id: 'test', name: 'Test', description: '',
    })
    expect(res.status).toBe(403)
  })

  it('POST /api/items returns 403', async () => {
    const res = await request(app).post('/api/items').send({
      id: 'x', name: 'X', description: '', type: 'MISC',
    })
    expect(res.status).toBe(403)
  })

  it('POST /api/npcs returns 403', async () => {
    const res = await request(app).post('/api/npcs').send({
      id: 'x', name: 'X', description: '', zoneId: 'z', startRoomId: 'z:r', behaviorType: 'idle',
    })
    expect(res.status).toBe(403)
  })

  it('POST /api/character-classes returns 403', async () => {
    const res = await request(app).post('/api/character-classes').send({
      id: 'x', name: 'X', description: '', minimumStats: '{}',
    })
    expect(res.status).toBe(403)
  })

  it('POST /api/races returns 403', async () => {
    const res = await request(app).post('/api/races').send({
      id: 'x', name: 'X', description: '', statModifiers: '{}',
    })
    expect(res.status).toBe(403)
  })

  it('POST /api/skills returns 403', async () => {
    const res = await request(app).post('/api/skills').send({
      id: 'x', name: 'X', description: '', category: '', primaryStat: '', secondaryStat: '',
    })
    expect(res.status).toBe(403)
  })

  it('POST /api/spells returns 403', async () => {
    const res = await request(app).post('/api/spells').send({
      id: 'x', name: 'X', description: '', school: '', spellType: '', manaCost: 0,
    })
    expect(res.status).toBe(403)
  })

  it('POST /api/loot-tables returns 403', async () => {
    const res = await request(app).post('/api/loot-tables').send({
      id: 'x', items: '[]',
    })
    expect(res.status).toBe(403)
  })
})

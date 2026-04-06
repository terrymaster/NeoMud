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

// ─── Upload ──────────────────────────────────────────────────

describe('POST /api/asset-mgmt/upload', () => {
  it('uploads a file successfully', async () => {
    const res = await request(app)
      .post('/api/asset-mgmt/upload')
      .field('assetPath', 'images/test/hello.webp')
      .attach('file', Buffer.from('fake-image-data'), 'hello.webp')
    expect(res.status).toBe(200)
    expect(res.body.ok).toBe(true)
    expect(res.body.assetPath).toBe('images/test/hello.webp')
  })

  it('rejects missing assetPath', async () => {
    const res = await request(app)
      .post('/api/asset-mgmt/upload')
      .attach('file', Buffer.from('data'), 'hello.webp')
    expect(res.status).toBe(400)
    expect(res.body.error).toMatch(/assetPath.*required/i)
  })

  it('rejects missing file', async () => {
    const res = await request(app)
      .post('/api/asset-mgmt/upload')
      .field('assetPath', 'images/test.webp')
    expect(res.status).toBe(400)
    expect(res.body.error).toMatch(/required/i)
  })

  it('rejects disallowed file extensions', async () => {
    const res = await request(app)
      .post('/api/asset-mgmt/upload')
      .field('assetPath', 'images/test.exe')
      .attach('file', Buffer.from('bad'), 'test.exe')
    expect(res.status).toBe(500) // multer throws Error which becomes 500
  })

  it('rejects path traversal attempts', async () => {
    const res = await request(app)
      .post('/api/asset-mgmt/upload')
      .field('assetPath', '../../../etc/passwd')
      .attach('file', Buffer.from('data'), 'passwd.json')
    expect(res.status).toBe(400)
    expect(res.body.error).toMatch(/[Ii]nvalid asset path/)
  })
})

// ─── History ─────────────────────────────────────────────────

describe('GET /api/asset-mgmt/history', () => {
  it('returns depth 0 for new file', async () => {
    const res = await request(app).get('/api/asset-mgmt/history?path=images/nonexistent.webp')
    expect(res.status).toBe(200)
    expect(res.body.depth).toBe(0)
  })

  it('requires path parameter', async () => {
    const res = await request(app).get('/api/asset-mgmt/history')
    expect(res.status).toBe(400)
    expect(res.body.error).toMatch(/path.*required/i)
  })

  it('increments depth after multiple uploads', async () => {
    // Upload version 1
    await request(app)
      .post('/api/asset-mgmt/upload')
      .field('assetPath', 'images/hist_test.webp')
      .attach('file', Buffer.from('version-1'), 'hist_test.webp')

    // Upload version 2 (creates backup of v1)
    await request(app)
      .post('/api/asset-mgmt/upload')
      .field('assetPath', 'images/hist_test.webp')
      .attach('file', Buffer.from('version-2'), 'hist_test.webp')

    const res = await request(app).get('/api/asset-mgmt/history?path=images/hist_test.webp')
    expect(res.status).toBe(200)
    expect(res.body.depth).toBeGreaterThanOrEqual(1)
  })
})

// ─── Undo ────────────────────────────────────────────────────

describe('POST /api/asset-mgmt/undo', () => {
  it('requires assetPath', async () => {
    const res = await request(app).post('/api/asset-mgmt/undo').send({})
    expect(res.status).toBe(400)
    expect(res.body.error).toMatch(/assetPath.*required/i)
  })

  it('returns 404 when no history available', async () => {
    const res = await request(app)
      .post('/api/asset-mgmt/undo')
      .send({ assetPath: 'images/no_history.webp' })
    expect(res.status).toBe(404)
    expect(res.body.error).toMatch(/[Nn]o history/)
  })

  it('restores previous version', async () => {
    // Upload twice to create history
    await request(app)
      .post('/api/asset-mgmt/upload')
      .field('assetPath', 'images/undo_test.webp')
      .attach('file', Buffer.from('original'), 'undo_test.webp')
    await request(app)
      .post('/api/asset-mgmt/upload')
      .field('assetPath', 'images/undo_test.webp')
      .attach('file', Buffer.from('modified'), 'undo_test.webp')

    const res = await request(app)
      .post('/api/asset-mgmt/undo')
      .send({ assetPath: 'images/undo_test.webp' })
    expect(res.status).toBe(200)
    expect(res.body.ok).toBe(true)
  })
})

// ─── Clear ───────────────────────────────────────────────────

describe('POST /api/asset-mgmt/clear', () => {
  it('requires assetPath', async () => {
    const res = await request(app).post('/api/asset-mgmt/clear').send({})
    expect(res.status).toBe(400)
    expect(res.body.error).toMatch(/assetPath.*required/i)
  })

  it('clears an existing file', async () => {
    // Upload a file first
    await request(app)
      .post('/api/asset-mgmt/upload')
      .field('assetPath', 'images/clear_test.webp')
      .attach('file', Buffer.from('to-be-cleared'), 'clear_test.webp')

    const res = await request(app)
      .post('/api/asset-mgmt/clear')
      .send({ assetPath: 'images/clear_test.webp' })
    expect(res.status).toBe(200)
    expect(res.body.ok).toBe(true)
  })

  it('succeeds even for non-existent file', async () => {
    const res = await request(app)
      .post('/api/asset-mgmt/clear')
      .send({ assetPath: 'images/doesnt_exist.webp' })
    expect(res.status).toBe(200)
    expect(res.body.ok).toBe(true)
  })
})

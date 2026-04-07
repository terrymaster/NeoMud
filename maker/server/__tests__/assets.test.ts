import { describe, it, expect, beforeAll, afterAll } from 'vitest'
import request from 'supertest'
import type express from 'express'
import { createTestApp, createReadOnlyTestApp } from './helpers.js'

describe('Asset management — read-only guards', () => {
  let app: express.Express
  let cleanup: () => Promise<void>

  beforeAll(async () => {
    const ctx = await createReadOnlyTestApp()
    app = ctx.app
    cleanup = ctx.cleanup
  })

  afterAll(async () => {
    await cleanup()
  })

  it('POST /upload returns 403 on read-only project', async () => {
    const res = await request(app)
      .post('/api/asset-mgmt/upload')
      .attach('file', Buffer.from('fake-png'), 'test.png')
      .field('assetPath', 'images/test.png')
    expect(res.status).toBe(403)
    expect(res.body.error).toMatch(/read-only/i)
  })

  it('POST /undo returns 403 on read-only project', async () => {
    const res = await request(app)
      .post('/api/asset-mgmt/undo')
      .send({ assetPath: 'images/test.png' })
    expect(res.status).toBe(403)
    expect(res.body.error).toMatch(/read-only/i)
  })

  it('POST /clear returns 403 on read-only project', async () => {
    const res = await request(app)
      .post('/api/asset-mgmt/clear')
      .send({ assetPath: 'images/test.png' })
    expect(res.status).toBe(403)
    expect(res.body.error).toMatch(/read-only/i)
  })
})

describe('Asset management — writable project', () => {
  let app: express.Express
  let cleanup: () => Promise<void>

  beforeAll(async () => {
    const ctx = await createTestApp()
    app = ctx.app
    cleanup = ctx.cleanup
  })

  afterAll(async () => {
    await cleanup()
  })

  it('POST /upload succeeds with valid file and assetPath', async () => {
    const res = await request(app)
      .post('/api/asset-mgmt/upload')
      .attach('file', Buffer.from('{"test": true}'), 'test.json')
      .field('assetPath', 'data/test.json')
    expect(res.status).toBe(200)
    expect(res.body.ok).toBe(true)
    expect(res.body.assetPath).toBe('data/test.json')
  })

  it('POST /upload rejects missing file', async () => {
    const res = await request(app)
      .post('/api/asset-mgmt/upload')
      .field('assetPath', 'data/test.json')
    expect(res.status).toBe(400)
  })

  it('POST /upload rejects missing assetPath', async () => {
    const res = await request(app)
      .post('/api/asset-mgmt/upload')
      .attach('file', Buffer.from('test-data'), 'test.json')
    expect(res.status).toBe(400)
  })

  it('POST /upload rejects path traversal', async () => {
    const res = await request(app)
      .post('/api/asset-mgmt/upload')
      .attach('file', Buffer.from('test-data'), 'test.json')
      .field('assetPath', '../../../etc/passwd')
    expect(res.status).toBe(400)
  })

  it('POST /undo returns 404 when no history exists', async () => {
    const res = await request(app)
      .post('/api/asset-mgmt/undo')
      .send({ assetPath: 'images/nonexistent.png' })
    expect(res.status).toBe(404)
  })

  it('POST /clear succeeds on nonexistent asset (no-op)', async () => {
    const res = await request(app)
      .post('/api/asset-mgmt/clear')
      .send({ assetPath: 'images/nonexistent.png' })
    expect(res.status).toBe(200)
    expect(res.body.ok).toBe(true)
  })

  it('GET /history returns depth 0 for unknown asset', async () => {
    const res = await request(app)
      .get('/api/asset-mgmt/history')
      .query({ path: 'images/nonexistent.png' })
    expect(res.status).toBe(200)
    expect(res.body.depth).toBe(0)
  })

  it('POST /upload rejects file with mismatched extension and content', async () => {
    // Text data disguised as .png
    const res = await request(app)
      .post('/api/asset-mgmt/upload')
      .attach('file', Buffer.from('This is plain text, not a PNG'), 'fake.png')
      .field('assetPath', 'images/fake.png')
    expect(res.status).toBe(400)
    expect(res.body.error).toMatch(/does not match/)
  })

  it('POST /upload accepts genuine PNG magic bytes', async () => {
    // Minimal PNG header (magic bytes only — enough for file-type detection)
    const pngHeader = Buffer.from([
      0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, // PNG signature
      0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52, // IHDR chunk
      0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01, // 1x1
      0x08, 0x02, 0x00, 0x00, 0x00, 0x90, 0x77, 0x53, // bit depth, color, etc.
      0xDE, 0x00, 0x00, 0x00, 0x0C, 0x49, 0x44, 0x41, // IDAT
      0x54, 0x08, 0xD7, 0x63, 0xF8, 0xCF, 0xC0, 0x00,
      0x00, 0x00, 0x02, 0x00, 0x01, 0xE2, 0x21, 0xBC,
      0x33, 0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, // IEND
      0x44, 0xAE, 0x42, 0x60, 0x82,
    ])
    const res = await request(app)
      .post('/api/asset-mgmt/upload')
      .attach('file', pngHeader, 'valid.png')
      .field('assetPath', 'images/valid.png')
    expect(res.status).toBe(200)
    expect(res.body.ok).toBe(true)
  })

  it('POST /upload accepts valid JSON file', async () => {
    const res = await request(app)
      .post('/api/asset-mgmt/upload')
      .attach('file', Buffer.from('{"key": "value"}'), 'data.json')
      .field('assetPath', 'data/data.json')
    expect(res.status).toBe(200)
    expect(res.body.ok).toBe(true)
  })

  it('POST /upload rejects invalid JSON file', async () => {
    const res = await request(app)
      .post('/api/asset-mgmt/upload')
      .attach('file', Buffer.from('not valid json {{{'), 'bad.json')
      .field('assetPath', 'data/bad.json')
    expect(res.status).toBe(400)
    expect(res.body.error).toMatch(/Invalid JSON/)
  })

  it('upload → undo round-trip works', async () => {
    const assetPath = 'data/roundtrip.json'

    // Upload v1
    await request(app)
      .post('/api/asset-mgmt/upload')
      .attach('file', Buffer.from('{"v":1}'), 'roundtrip.json')
      .field('assetPath', assetPath)
      .expect(200)

    // Upload v2 (creates history of v1)
    await request(app)
      .post('/api/asset-mgmt/upload')
      .attach('file', Buffer.from('{"v":2}'), 'roundtrip.json')
      .field('assetPath', assetPath)
      .expect(200)

    // History should be 1
    const histRes = await request(app)
      .get('/api/asset-mgmt/history')
      .query({ path: assetPath })
    expect(histRes.body.depth).toBe(1)

    // Undo should succeed
    const undoRes = await request(app)
      .post('/api/asset-mgmt/undo')
      .send({ assetPath })
    expect(undoRes.status).toBe(200)
    expect(undoRes.body.ok).toBe(true)
  })
})

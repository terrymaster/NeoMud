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

  // Create a zone and room for interactable tests
  await request(app).post('/api/zones').send({
    id: 'dungeon',
    name: 'Test Dungeon',
    description: 'A dungeon for testing interactables',
  })
  await request(app).post('/api/zones/dungeon/rooms').send({
    id: 'entrance',
    name: 'Dungeon Entrance',
    description: 'A dark entrance',
    x: 0,
    y: 0,
  })
})

afterAll(async () => {
  await cleanup()
})

describe('Room interactables persistence', () => {
  const lever = {
    id: 'lever_1',
    label: 'Rusty Lever',
    description: 'You pull the lever and hear a grinding sound.',
    failureMessage: '',
    icon: '',
    actionType: 'EXIT_OPEN',
    actionData: { direction: 'NORTH' },
    difficulty: 0,
    difficultyCheck: '',
    perceptionDC: 0,
    cooldownTicks: 10,
    resetTicks: 20,
    sound: 'lever_pull',
  }

  const difficultDoor = {
    id: 'heavy_door',
    label: 'Heavy Stone Door',
    description: 'You force the door open with a mighty heave!',
    failureMessage: "The door won't budge. You're not strong enough.",
    icon: '',
    actionType: 'EXIT_OPEN',
    actionData: { direction: 'WEST' },
    difficulty: 25,
    difficultyCheck: 'STRENGTH',
    perceptionDC: 0,
    cooldownTicks: 0,
    resetTicks: 0,
    sound: '',
  }

  it('PUT room with interactables JSON persists correctly', async () => {
    const interactables = JSON.stringify([lever])
    const res = await request(app)
      .put('/api/zones/dungeon/rooms/entrance')
      .send({ interactables })
    expect(res.status).toBe(200)
    expect(res.body.interactables).toBe(interactables)
  })

  it('GET room returns interactables JSON', async () => {
    const res = await request(app).get('/api/zones/dungeon/rooms/entrance')
    expect(res.status).toBe(200)
    const parsed = JSON.parse(res.body.interactables)
    expect(parsed).toHaveLength(1)
    expect(parsed[0].id).toBe('lever_1')
    expect(parsed[0].label).toBe('Rusty Lever')
    expect(parsed[0].actionType).toBe('EXIT_OPEN')
    expect(parsed[0].cooldownTicks).toBe(10)
  })

  it('interactable with difficulty fields round-trips', async () => {
    const interactables = JSON.stringify([lever, difficultDoor])
    const res = await request(app)
      .put('/api/zones/dungeon/rooms/entrance')
      .send({ interactables })
    expect(res.status).toBe(200)

    const get = await request(app).get('/api/zones/dungeon/rooms/entrance')
    const parsed = JSON.parse(get.body.interactables)
    expect(parsed).toHaveLength(2)

    const door = parsed.find((f: any) => f.id === 'heavy_door')
    expect(door.difficulty).toBe(25)
    expect(door.difficultyCheck).toBe('STRENGTH')
    expect(door.failureMessage).toBe("The door won't budge. You're not strong enough.")
  })

  it('interactables persist through zone GET', async () => {
    const res = await request(app).get('/api/zones/dungeon')
    expect(res.status).toBe(200)
    const room = res.body.rooms.find((r: any) => r.id === 'dungeon:entrance')
    expect(room).toBeDefined()
    const parsed = JSON.parse(room.interactables)
    expect(parsed).toHaveLength(2)
  })

  it('empty interactables JSON persists as empty array', async () => {
    const res = await request(app)
      .put('/api/zones/dungeon/rooms/entrance')
      .send({ interactables: '[]' })
    expect(res.status).toBe(200)

    const get = await request(app).get('/api/zones/dungeon/rooms/entrance')
    const parsed = JSON.parse(get.body.interactables)
    expect(parsed).toEqual([])
  })

  it('interactable with all action types persists', async () => {
    const allTypes = [
      { ...lever, id: 'exit_open', actionType: 'EXIT_OPEN', actionData: { direction: 'NORTH' } },
      { ...lever, id: 'treasure', actionType: 'TREASURE_DROP', actionData: { lootTableId: 'chest_loot' } },
      { ...lever, id: 'spawn', actionType: 'MONSTER_SPAWN', actionData: { npcId: 'wolf', count: '3' } },
      { ...lever, id: 'effect', actionType: 'ROOM_EFFECT', actionData: { effectType: 'HEAL', value: '50', durationTicks: '0' } },
      { ...lever, id: 'teleport', actionType: 'TELEPORT', actionData: { targetRoomId: 'dungeon:boss', message: 'Whoosh!' } },
    ]
    const interactables = JSON.stringify(allTypes)
    const res = await request(app)
      .put('/api/zones/dungeon/rooms/entrance')
      .send({ interactables })
    expect(res.status).toBe(200)

    const get = await request(app).get('/api/zones/dungeon/rooms/entrance')
    const parsed = JSON.parse(get.body.interactables)
    expect(parsed).toHaveLength(5)
    expect(parsed.map((f: any) => f.actionType)).toEqual([
      'EXIT_OPEN', 'TREASURE_DROP', 'MONSTER_SPAWN', 'ROOM_EFFECT', 'TELEPORT',
    ])
  })

  it('all four stat check types persist', async () => {
    const checks = ['STRENGTH', 'AGILITY', 'INTELLECT', 'WILLPOWER']
    const interactables = checks.map((check, i) => ({
      ...difficultDoor,
      id: `check_${i}`,
      difficulty: 20 + i,
      difficultyCheck: check,
      failureMessage: `Failed ${check} check`,
    }))
    const res = await request(app)
      .put('/api/zones/dungeon/rooms/entrance')
      .send({ interactables: JSON.stringify(interactables) })
    expect(res.status).toBe(200)

    const get = await request(app).get('/api/zones/dungeon/rooms/entrance')
    const parsed = JSON.parse(get.body.interactables)
    for (let i = 0; i < checks.length; i++) {
      expect(parsed[i].difficultyCheck).toBe(checks[i])
      expect(parsed[i].difficulty).toBe(20 + i)
      expect(parsed[i].failureMessage).toBe(`Failed ${checks[i]} check`)
    }
  })
})

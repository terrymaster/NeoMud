import { Router, Request, Response, NextFunction } from 'express'
import { db, isReadOnly } from '../db.js'

export const zonesRouter = Router()

const DIRECTION_OPPOSITES: Record<string, string> = {
  NORTH: 'SOUTH',
  SOUTH: 'NORTH',
  EAST: 'WEST',
  WEST: 'EAST',
  NORTHEAST: 'SOUTHWEST',
  SOUTHWEST: 'NORTHEAST',
  NORTHWEST: 'SOUTHEAST',
  SOUTHEAST: 'NORTHWEST',
  UP: 'DOWN',
  DOWN: 'UP',
}

function rejectIfReadOnly(_req: Request, res: Response, next: NextFunction) {
  if (isReadOnly()) {
    res.status(403).json({ error: 'Project is read-only. Fork it to make changes.' })
    return
  }
  next()
}

// ─── Zone routes ───────────────────────────────────────────────

// GET /zones — list all zones with room count
zonesRouter.get('/zones', async (_req, res) => {
  try {
    const zones = await db().zone.findMany({
      include: { _count: { select: { rooms: true } } },
    })
    res.json(zones)
  } catch (err: any) {
    res.status(500).json({ error: err.message })
  }
})

// POST /zones — create zone
zonesRouter.post('/zones', rejectIfReadOnly, async (req, res) => {
  try {
    const {
      id, name, description,
      safe, bgm,
      spawnRoom, spawnMaxEntities, spawnMaxPerRoom, spawnRateTicks,
    } = req.body
    const zone = await db().zone.create({
      data: {
        id, name, description,
        safe: safe ?? true,
        bgm: bgm ?? '',
        spawnRoom: spawnRoom ?? null,
        spawnMaxEntities: spawnMaxEntities ?? 0,
        spawnMaxPerRoom: spawnMaxPerRoom ?? 0,
        spawnRateTicks: spawnRateTicks ?? 0,
      },
    })
    res.json(zone)
  } catch (err: any) {
    res.status(500).json({ error: err.message })
  }
})

// GET /zones/:id — get zone with rooms and exits
zonesRouter.get('/zones/:id', async (req, res) => {
  try {
    const zone = await db().zone.findUnique({
      where: { id: req.params.id },
      include: { rooms: { include: { exits: true } } },
    })
    if (!zone) {
      res.status(404).json({ error: 'Zone not found' })
      return
    }
    res.json(zone)
  } catch (err: any) {
    res.status(500).json({ error: err.message })
  }
})

// PUT /zones/:id — update zone
zonesRouter.put('/zones/:id', rejectIfReadOnly, async (req, res) => {
  try {
    const zone = await db().zone.update({
      where: { id: req.params.id },
      data: req.body,
    })
    res.json(zone)
  } catch (err: any) {
    res.status(500).json({ error: err.message })
  }
})

// DELETE /zones/:id — delete zone (cascades rooms/exits)
zonesRouter.delete('/zones/:id', rejectIfReadOnly, async (req, res) => {
  try {
    await db().zone.delete({ where: { id: req.params.id } })
    res.json({ ok: true })
  } catch (err: any) {
    res.status(500).json({ error: err.message })
  }
})

// ─── Room routes ───────────────────────────────────────────────

// GET /zones/:zoneId/rooms — list rooms in zone
zonesRouter.get('/zones/:zoneId/rooms', async (req, res) => {
  try {
    const rooms = await db().room.findMany({
      where: { zoneId: req.params.zoneId },
      include: { exits: true },
    })
    res.json(rooms)
  } catch (err: any) {
    res.status(500).json({ error: err.message })
  }
})

// POST /zones/:zoneId/rooms — create room
zonesRouter.post('/zones/:zoneId/rooms', rejectIfReadOnly, async (req, res) => {
  try {
    const { zoneId } = req.params
    const {
      id, name, description, x, y,
      backgroundImage, healPerTick, bgm, departSound,
    } = req.body
    const fullId = `${zoneId}:${id}`
    const room = await db().room.create({
      data: {
        id: fullId,
        zoneId,
        name,
        description,
        x,
        y,
        backgroundImage: backgroundImage ?? '',
        healPerTick: healPerTick ?? 0,
        bgm: bgm ?? '',
        departSound: departSound ?? '',
      },
    })
    res.json(room)
  } catch (err: any) {
    res.status(500).json({ error: err.message })
  }
})

// GET /zones/:zoneId/rooms/:id — get room with exits
zonesRouter.get('/zones/:zoneId/rooms/:id', async (req, res) => {
  try {
    const fullId = `${req.params.zoneId}:${req.params.id}`
    const room = await db().room.findUnique({
      where: { id: fullId },
      include: { exits: true },
    })
    if (!room) {
      res.status(404).json({ error: 'Room not found' })
      return
    }
    res.json(room)
  } catch (err: any) {
    res.status(500).json({ error: err.message })
  }
})

// PUT /zones/:zoneId/rooms/:id — update room
zonesRouter.put('/zones/:zoneId/rooms/:id', rejectIfReadOnly, async (req, res) => {
  try {
    const fullId = `${req.params.zoneId}:${req.params.id}`
    const room = await db().room.update({
      where: { id: fullId },
      data: req.body,
    })
    res.json(room)
  } catch (err: any) {
    res.status(500).json({ error: err.message })
  }
})

// DELETE /zones/:zoneId/rooms/:id — delete room
zonesRouter.delete('/zones/:zoneId/rooms/:id', rejectIfReadOnly, async (req, res) => {
  try {
    const fullId = `${req.params.zoneId}:${req.params.id}`
    await db().room.delete({ where: { id: fullId } })
    res.json({ ok: true })
  } catch (err: any) {
    res.status(500).json({ error: err.message })
  }
})

// ─── Exit routes ───────────────────────────────────────────────

// POST /rooms/:id/exits — create exit + reverse exit
zonesRouter.post('/rooms/:id/exits', rejectIfReadOnly, async (req, res) => {
  try {
    const fromRoomId = req.params.id
    const { direction, toRoomId } = req.body
    const opposite = DIRECTION_OPPOSITES[direction]

    const exit = await db().exit.create({
      data: { fromRoomId, direction, toRoomId },
    })

    // Create reverse exit if an opposite direction exists
    if (opposite) {
      await db().exit.create({
        data: { fromRoomId: toRoomId, direction: opposite, toRoomId: fromRoomId },
      })
    }

    res.json(exit)
  } catch (err: any) {
    res.status(500).json({ error: err.message })
  }
})

// DELETE /rooms/:id/exits/:dir — delete exit + reverse exit
zonesRouter.delete('/rooms/:id/exits/:dir', rejectIfReadOnly, async (req, res) => {
  try {
    const fromRoomId = req.params.id
    const direction = req.params.dir
    const opposite = DIRECTION_OPPOSITES[direction]

    // Find the forward exit so we know the target room for reverse deletion
    const forwardExit = await db().exit.findUnique({
      where: { fromRoomId_direction: { fromRoomId, direction } },
    })

    if (!forwardExit) {
      res.status(404).json({ error: 'Exit not found' })
      return
    }

    // Delete forward exit
    await db().exit.delete({
      where: { fromRoomId_direction: { fromRoomId, direction } },
    })

    // Delete reverse exit if it exists
    if (opposite) {
      await db().exit.deleteMany({
        where: {
          fromRoomId: forwardExit.toRoomId,
          direction: opposite,
          toRoomId: fromRoomId,
        },
      })
    }

    res.json({ ok: true })
  } catch (err: any) {
    res.status(500).json({ error: err.message })
  }
})

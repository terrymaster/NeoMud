package com.neomud.server.world

import com.neomud.shared.model.Direction
import com.neomud.shared.model.MapRoom
import com.neomud.shared.model.Room
import com.neomud.shared.model.RoomId

class WorldGraph {
    private val rooms = HashMap<RoomId, Room>()

    var defaultSpawnRoom: RoomId = ""
        private set

    fun addRoom(room: Room) {
        rooms[room.id] = room
        if (defaultSpawnRoom.isEmpty()) {
            defaultSpawnRoom = room.id
        }
    }

    fun getRoom(roomId: RoomId): Room? = rooms[roomId]

    fun getRoomsNear(centerRoomId: RoomId, radius: Int = 3): List<MapRoom> {
        val center = rooms[centerRoomId] ?: return emptyList()
        val visited = mutableSetOf<RoomId>()
        val result = mutableListOf<MapRoom>()
        val queue = ArrayDeque<Pair<RoomId, Int>>()

        queue.add(centerRoomId to 0)
        visited.add(centerRoomId)

        while (queue.isNotEmpty()) {
            val (currentId, depth) = queue.removeFirst()
            val room = rooms[currentId] ?: continue

            result.add(
                MapRoom(
                    id = room.id,
                    name = room.name,
                    x = room.x,
                    y = room.y,
                    exits = room.exits,
                    backgroundImage = room.backgroundImage
                )
            )

            if (depth < radius) {
                for ((_, targetId) in room.exits) {
                    if (targetId !in visited) {
                        visited.add(targetId)
                        queue.add(targetId to depth + 1)
                    }
                }
            }
        }

        return result
    }

    fun setDefaultSpawn(roomId: RoomId) {
        if (rooms.containsKey(roomId)) {
            defaultSpawnRoom = roomId
        }
    }

    fun getRoomIdsInZone(zoneId: String): List<RoomId> =
        rooms.values.filter { it.zoneId == zoneId }.map { it.id }

    val roomCount: Int get() = rooms.size

    fun getAllRooms(): List<Room> = rooms.values.toList()

    // TODO: Consider a global lock refresh timer to re-lock exits periodically
    fun unlockExit(roomId: RoomId, direction: Direction) {
        val room = rooms[roomId] ?: return
        rooms[roomId] = room.copy(lockedExits = room.lockedExits - direction)
    }
}

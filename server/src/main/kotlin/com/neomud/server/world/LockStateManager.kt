package com.neomud.server.world

import com.neomud.shared.model.Direction
import com.neomud.shared.model.RoomId

class LockStateManager {
    private val unlockedExits = mutableSetOf<Pair<RoomId, Direction>>()

    fun isUnlocked(roomId: RoomId, direction: Direction): Boolean =
        (roomId to direction) in unlockedExits

    fun unlock(roomId: RoomId, direction: Direction) {
        unlockedExits.add(roomId to direction)
    }
}

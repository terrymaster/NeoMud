package com.neomud.server.game

import com.neomud.server.session.PlayerSession
import com.neomud.server.world.WorldGraph
import com.neomud.shared.model.Room

object RoomFilter {
    fun forPlayer(room: Room, session: PlayerSession, worldGraph: WorldGraph): Room {
        val hiddenDefs = worldGraph.getHiddenExitDefs(room.id)
        if (hiddenDefs.isEmpty()) return room

        val visibleExits = room.exits.filter { (dir, _) ->
            dir !in hiddenDefs || session.hasDiscoveredExit(room.id, dir)
        }
        val visibleLocks = room.lockedExits.filter { (dir, _) ->
            dir !in hiddenDefs || session.hasDiscoveredExit(room.id, dir)
        }
        return room.copy(exits = visibleExits, lockedExits = visibleLocks)
    }
}

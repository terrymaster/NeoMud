package com.neomud.server.game

import com.neomud.server.session.PlayerSession
import com.neomud.server.world.WorldGraph
import com.neomud.shared.model.Room

object RoomFilter {
    fun forPlayer(room: Room, session: PlayerSession, worldGraph: WorldGraph): Room {
        val hiddenDefs = worldGraph.getHiddenExitDefs(room.id)

        val visibleExits = if (hiddenDefs.isEmpty()) room.exits else room.exits.filter { (dir, _) ->
            dir !in hiddenDefs || session.hasDiscoveredExit(room.id, dir)
        }
        // Only show locks the player has discovered (bumped into or tried to pick)
        val visibleLocks = room.lockedExits.filter { (dir, _) ->
            (dir !in hiddenDefs || session.hasDiscoveredExit(room.id, dir)) &&
                session.hasDiscoveredLock(room.id, dir)
        }

        val visibleInteractables = room.interactables.filter { feat ->
            feat.perceptionDC <= 0 || session.hasDiscoveredInteractable(room.id, feat.id)
        }

        return room.copy(
            exits = visibleExits,
            lockedExits = visibleLocks,
            interactables = visibleInteractables
        )
    }
}

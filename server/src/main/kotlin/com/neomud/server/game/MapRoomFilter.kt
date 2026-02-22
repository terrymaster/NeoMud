package com.neomud.server.game

import com.neomud.server.game.npc.NpcManager
import com.neomud.server.session.PlayerSession
import com.neomud.server.session.SessionManager
import com.neomud.server.world.WorldGraph
import com.neomud.shared.model.MapRoom

object MapRoomFilter {
    fun enrichForPlayer(
        mapRooms: List<MapRoom>,
        session: PlayerSession,
        worldGraph: WorldGraph,
        sessionManager: SessionManager,
        npcManager: NpcManager
    ): List<MapRoom> = mapRooms.map { mapRoom ->
        val room = worldGraph.getRoom(mapRoom.id)
        val hiddenDefs = worldGraph.getHiddenExitDefs(mapRoom.id)

        // Filter out undiscovered hidden exits (just like RoomFilter does for RoomInfo)
        val visibleExits = if (hiddenDefs.isEmpty()) mapRoom.exits else mapRoom.exits.filter { (dir, _) ->
            dir !in hiddenDefs || session.hasDiscoveredExit(mapRoom.id, dir)
        }

        // Which visible exits are locked? (only those the player has discovered)
        val locked = room?.lockedExits?.keys?.filter {
            it in visibleExits && session.hasDiscoveredLock(mapRoom.id, it)
        }?.toSet() ?: emptySet()

        // Which visible exits are discovered-hidden?
        val hidden = hiddenDefs.keys.filter { session.hasDiscoveredExit(mapRoom.id, it) && it in visibleExits }.toSet()

        mapRoom.copy(
            exits = visibleExits,
            hasPlayers = sessionManager.getPlayerNamesInRoom(mapRoom.id).isNotEmpty(),
            hasNpcs = npcManager.getNpcsInRoom(mapRoom.id).isNotEmpty(),
            lockedExits = locked,
            hiddenExits = hidden
        )
    }
}

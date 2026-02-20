package com.neomud.server.game.commands


import com.neomud.server.game.npc.NpcManager
import com.neomud.server.session.PlayerSession
import com.neomud.server.world.WorldGraph
import com.neomud.shared.model.RoomId
import com.neomud.shared.protocol.ServerMessage

class TrackCommand(
    private val npcManager: NpcManager,
    private val worldGraph: WorldGraph
) {
    suspend fun execute(session: PlayerSession) {
        val roomId = session.currentRoomId ?: return
        val player = session.player ?: return

        val cooldown = session.skillCooldowns["TRACK"]
        if (cooldown != null && cooldown > 0) {
            session.send(ServerMessage.SystemMessage("Track is on cooldown ($cooldown ticks remaining)."))
            return
        }

        session.skillCooldowns["TRACK"] = 4

        // Skill check
        val effStats = session.effectiveStats()
        val roll = (1..20).random()
        val check = effStats.willpower + effStats.intellect / 2 + roll
        if (check < 13) {
            session.send(ServerMessage.SystemMessage("You fail to find any tracks."))
            return
        }

        // BFS from current room, depth 5, find nearest room with living NPCs
        val result = bfsForNpcs(roomId, maxDepth = 5)
        if (result == null) {
            session.send(ServerMessage.SystemMessage("You don't detect anything nearby."))
        } else {
            session.send(ServerMessage.SystemMessage("You sense creatures to the ${result.name}."))
        }

        // Hidden exit detection — TRACK gets a bonus (+5) over passive perception
        val hiddenDefs = worldGraph.getHiddenExitDefs(roomId)
        if (hiddenDefs.isNotEmpty()) {
            val trackRoll = check + 5 // reuse the TRACK roll with a bonus
            var found = false
            for ((dir, data) in hiddenDefs) {
                if (session.hasDiscoveredExit(roomId, dir)) continue
                if (trackRoll >= data.perceptionDC) {
                    session.discoverExit(roomId, dir)
                    worldGraph.revealHiddenExit(roomId, dir)
                    session.send(ServerMessage.SystemMessage("Your tracking skills reveal a hidden passage to the ${dir.name.lowercase()}!"))
                    found = true
                }
            }
            if (found) {
                // Re-send room info so client updates the D-pad
                val room = worldGraph.getRoom(roomId)
                if (room != null) {
                    val filteredRoom = com.neomud.server.game.RoomFilter.forPlayer(room, session, worldGraph)
                    session.send(ServerMessage.RoomInfo(
                        filteredRoom,
                        emptyList(), // players list will be re-sent on next look
                        emptyList()
                    ))
                }
            }
        }
    }

    private fun bfsForNpcs(startRoomId: RoomId, maxDepth: Int): com.neomud.shared.model.Direction? {
        val visited = mutableSetOf(startRoomId)
        // Queue entries: (roomId, depth, firstDirection)
        val queue = ArrayDeque<Triple<RoomId, Int, com.neomud.shared.model.Direction>>()

        val startRoom = worldGraph.getRoom(startRoomId) ?: return null
        for ((dir, targetId) in startRoom.exits) {
            if (targetId !in visited) {
                visited.add(targetId)
                queue.add(Triple(targetId, 1, dir))
            }
        }

        while (queue.isNotEmpty()) {
            val (currentId, depth, firstDir) = queue.removeFirst()

            // Check if this room has living NPCs
            if (npcManager.getLivingNpcsInRoom(currentId).isNotEmpty()) {
                return firstDir
            }

            if (depth < maxDepth) {
                val room = worldGraph.getRoom(currentId) ?: continue
                for ((_, targetId) in room.exits) {
                    if (targetId !in visited) {
                        visited.add(targetId)
                        queue.add(Triple(targetId, depth + 1, firstDir))
                    }
                }
            }
        }

        return null
    }
}

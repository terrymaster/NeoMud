package com.neomud.server.game.commands

import com.neomud.server.game.combat.CombatUtils
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
        val effStats = CombatUtils.effectiveStats(player.stats, session.activeEffects.toList())
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
            return
        }

        session.send(ServerMessage.SystemMessage("You sense creatures to the ${result.name}."))
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

package com.neomud.server.game.commands

import com.neomud.server.game.MovementTrailManager
import com.neomud.server.session.PlayerSession
import com.neomud.server.world.WorldGraph
import com.neomud.shared.protocol.ServerMessage

class TrackCommand(
    private val movementTrailManager: MovementTrailManager,
    private val worldGraph: WorldGraph
) {
    suspend fun execute(session: PlayerSession, targetId: String? = null) {
        val roomId = session.currentRoomId ?: return
        val player = session.player ?: return

        val cooldown = session.skillCooldowns["TRACK"]
        if (cooldown != null && cooldown > 0) {
            session.send(ServerMessage.SystemMessage("Track is on cooldown ($cooldown ticks remaining)."))
            return
        }

        session.skillCooldowns["TRACK"] = 4

        // Skill check: willpower + agility/2 + level/2 + d20
        val effStats = session.effectiveStats()
        val roll = (1..20).random()
        val check = effStats.willpower + effStats.agility / 2 + player.level / 2 + roll

        // Get trails in current room (optionally filtered by target)
        val trails = movementTrailManager.getTrails(roomId, targetId)

        if (trails.isEmpty()) {
            if (check >= 13) {
                session.send(ServerMessage.TrackResult(
                    success = false,
                    message = "You don't find any tracks here."
                ))
            } else {
                session.send(ServerMessage.TrackResult(
                    success = false,
                    message = "You fail to find any tracks."
                ))
            }
            // Hidden exit detection — TRACK gets a bonus (+5) over passive perception
            checkHiddenExits(session, roomId, check)
            return
        }

        // Use freshest trail
        val freshest = trails.first()
        val penalty = movementTrailManager.stalenessPenalty(freshest)
        val difficulty = 13 + penalty

        if (check >= difficulty) {
            val dirName = freshest.direction.name.lowercase()
            session.send(ServerMessage.TrackResult(
                success = true,
                direction = freshest.direction,
                targetName = freshest.entityName,
                message = "You find tracks left by ${freshest.entityName} leading $dirName."
            ))
        } else {
            session.send(ServerMessage.TrackResult(
                success = false,
                message = "You find faint tracks but cannot make them out."
            ))
        }

        // Hidden exit detection — TRACK gets a bonus (+5) over passive perception
        checkHiddenExits(session, roomId, check)
    }

    private suspend fun checkHiddenExits(session: PlayerSession, roomId: String, check: Int) {
        val hiddenDefs = worldGraph.getHiddenExitDefs(roomId)
        if (hiddenDefs.isEmpty()) return

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
                    emptyList(),
                    emptyList()
                ))
            }
        }
    }
}

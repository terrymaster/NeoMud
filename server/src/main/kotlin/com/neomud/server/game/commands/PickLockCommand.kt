package com.neomud.server.game.commands

import com.neomud.server.game.MeditationUtils
import com.neomud.server.game.StealthUtils

import com.neomud.server.session.PlayerSession
import com.neomud.server.session.SessionManager
import com.neomud.server.world.WorldGraph
import com.neomud.shared.model.Direction
import com.neomud.shared.protocol.ServerMessage

// TODO: Consider lockable containers/chests as future pickable targets
class PickLockCommand(
    private val worldGraph: WorldGraph,
    private val sessionManager: SessionManager
) {
    suspend fun execute(session: PlayerSession, targetId: String? = null): Boolean {
        val roomId = session.currentRoomId ?: return false
        val player = session.player ?: return false

        // Picking a lock breaks meditation and stealth
        MeditationUtils.breakMeditation(session, "You stop meditating.")
        StealthUtils.breakStealth(session, sessionManager, "Picking a lock reveals your presence!")

        val cooldown = session.skillCooldowns["PICK_LOCK"]
        if (cooldown != null && cooldown > 0) {
            session.send(ServerMessage.SystemMessage("Pick Lock is on cooldown ($cooldown ticks remaining)."))
            return false
        }

        val room = worldGraph.getRoom(roomId) ?: return false

        val lockedExits = room.lockedExits

        if (lockedExits.isEmpty()) {
            session.send(ServerMessage.SystemMessage("You don't see anything locked here."))
            return false
        }

        // Resolve target
        val direction: Direction
        val difficulty: Int

        if (targetId != null) {
            // Parse "exit:DIRECTION" format
            if (!targetId.startsWith("exit:")) {
                session.send(ServerMessage.SystemMessage("Invalid lock target."))
                return false
            }
            val dirName = targetId.removePrefix("exit:")
            val parsedDir = try { Direction.valueOf(dirName) } catch (_: IllegalArgumentException) {
                session.send(ServerMessage.SystemMessage("Invalid direction: $dirName"))
                return false
            }
            val diff = lockedExits[parsedDir]
            if (diff == null) {
                session.send(ServerMessage.SystemMessage("The door to the ${parsedDir.name} is not locked."))
                return false
            }
            direction = parsedDir
            difficulty = diff
        } else if (lockedExits.size == 1) {
            // Auto-pick the only target
            val entry = lockedExits.entries.first()
            direction = entry.key
            difficulty = entry.value
        } else {
            // Multiple targets — list them for the player
            val listing = lockedExits.keys.joinToString(", ") { "exit:${it.name}" }
            session.send(ServerMessage.SystemMessage("Multiple locked exits: $listing. Specify a target."))
            return false
        }

        session.skillCooldowns["PICK_LOCK"] = 3

        val effStats = session.effectiveStats()
        val roll = (1..20).random()
        val check = effStats.agility + effStats.intellect / 2 + roll

        if (check >= difficulty) {
            worldGraph.unlockExit(roomId, direction)
            session.send(ServerMessage.SystemMessage("You pick the lock on the door to the ${direction.name}."))
            return true
        } else {
            session.send(ServerMessage.SystemMessage("You fail to pick the lock."))
            return false
        }
    }
}

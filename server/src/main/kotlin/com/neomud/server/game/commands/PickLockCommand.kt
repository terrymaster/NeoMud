package com.neomud.server.game.commands

import com.neomud.server.game.MeditationUtils
import com.neomud.server.game.StealthUtils

import com.neomud.server.game.RoomFilter
import com.neomud.server.session.PlayerSession
import com.neomud.server.session.SessionManager
import com.neomud.server.game.npc.NpcManager
import com.neomud.server.world.WorldGraph
import com.neomud.shared.model.Direction
import com.neomud.shared.protocol.ServerMessage

// TODO: Consider lockable containers/chests as future pickable targets
class PickLockCommand(
    private val worldGraph: WorldGraph,
    private val sessionManager: SessionManager,
    private val npcManager: NpcManager
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

        // Build list of pickable locks: regular locked exits + discovered hidden+locked exits
        // Exclude unpickable exits (interaction-only locks)
        val hiddenDefs = worldGraph.getHiddenExitDefs(roomId)
        val unpickable = room.unpickableExits
        val pickableExits = room.lockedExits.filter { (dir, _) ->
            // Regular locked exit, or hidden+locked that player has discovered
            dir !in hiddenDefs || session.hasDiscoveredExit(roomId, dir)
        }.filter { (dir, _) -> dir !in unpickable }

        if (pickableExits.isEmpty()) {
            session.send(ServerMessage.SystemMessage("You don't see anything locked here."))
            return false
        }

        // Build list of pickable interactables (features with difficulty checks)
        val interactableDefs = worldGraph.getInteractableDefs(roomId)
        val pickableFeatures = interactableDefs.filter { it.difficulty > 0 && it.difficultyCheck.isNotEmpty() }

        // Resolve target
        if (targetId != null && targetId.startsWith("feature:")) {
            // Picking an interactable lock
            val featureId = targetId.removePrefix("feature:")
            val feat = pickableFeatures.find { it.id == featureId }
            if (feat == null) {
                session.send(ServerMessage.SystemMessage("You don't see anything like that to pick."))
                return false
            }

            session.skillCooldowns["PICK_LOCK"] = 3

            val effStats = session.effectiveStats()
            val roll = (1..20).random()
            val check = effStats.agility + effStats.intellect / 2 + roll

            if (check >= feat.difficulty) {
                session.send(ServerMessage.SystemMessage("You successfully pick the lock on the ${feat.label}."))
                // Trigger the interactable's action (e.g. EXIT_OPEN)
                return true
            } else {
                val failMsg = feat.failureMessage.ifEmpty { "You fail to pick the lock on the ${feat.label}." }
                session.send(ServerMessage.SystemMessage(failMsg))
                return false
            }
        }

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
            if (parsedDir in unpickable && parsedDir in room.lockedExits) {
                session.send(ServerMessage.SystemMessage("This lock cannot be picked — it must be opened another way."))
                return false
            }
            val diff = pickableExits[parsedDir]
            if (diff == null) {
                session.send(ServerMessage.SystemMessage("${parsedDir.lockedExitPhrase.replaceFirstChar { it.uppercase() }} is not locked."))
                return false
            }
            direction = parsedDir
            difficulty = diff
        } else {
            // No target specified — discover all locks for the client
            for (dir in pickableExits.keys) {
                session.discoverLock(roomId, dir)
            }

            val totalTargets = pickableExits.size + pickableFeatures.size
            if (totalTargets == 1) {
                // Only one target exists — auto-pick it
                if (pickableExits.size == 1) {
                    val entry = pickableExits.entries.first()
                    direction = entry.key
                    difficulty = entry.value
                } else {
                    val feat = pickableFeatures.first()
                    session.skillCooldowns["PICK_LOCK"] = 3
                    val effStats = session.effectiveStats()
                    val roll = (1..20).random()
                    val check = effStats.agility + effStats.intellect / 2 + roll
                    if (check >= feat.difficulty) {
                        session.send(ServerMessage.SystemMessage("You successfully pick the lock on the ${feat.label}."))
                        return true
                    } else {
                        val failMsg = feat.failureMessage.ifEmpty { "You fail to pick the lock on the ${feat.label}." }
                        session.send(ServerMessage.SystemMessage(failMsg))
                        return false
                    }
                }
            } else {
                // Multiple targets — resend RoomInfo with newly discovered locks and prompt
                val filteredRoom = RoomFilter.forPlayer(room, session, worldGraph)
                val playersInRoom = sessionManager.getVisiblePlayerInfosInRoom(roomId)
                    .filter { it.name != player.name }
                val npcsInRoom = npcManager.getNpcsInRoom(roomId)
                session.send(ServerMessage.RoomInfo(filteredRoom, playersInRoom, npcsInRoom))
                session.send(ServerMessage.SystemMessage("You notice several locks here. Choose a target."))
                return false
            }
        }

        // Mark lock as discovered so direction pad and map show it
        session.discoverLock(roomId, direction)

        session.skillCooldowns["PICK_LOCK"] = 3

        val effStats = session.effectiveStats()
        val roll = (1..20).random()
        val check = effStats.agility + effStats.intellect / 2 + roll

        if (check >= difficulty) {
            worldGraph.unlockExit(roomId, direction)
            session.send(ServerMessage.SystemMessage("You pick the lock on ${direction.lockedExitPhrase}."))
            return true
        } else {
            session.send(ServerMessage.SystemMessage("You fail to pick the lock."))
            return false
        }
    }
}

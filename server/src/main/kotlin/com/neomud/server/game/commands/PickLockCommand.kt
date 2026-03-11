package com.neomud.server.game.commands

import com.neomud.server.game.GameConfig
import com.neomud.server.game.MeditationUtils
import com.neomud.server.game.RestUtils
import com.neomud.server.game.StealthUtils

import com.neomud.server.game.RoomFilter
import com.neomud.server.session.PendingSkill
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
    /**
     * Validates prerequisites and queues a PendingSkill.PickLock for tick resolution.
     * Discovery/prompting for multiple targets remains immediate (interactive UI flow).
     */
    suspend fun execute(session: PlayerSession, targetId: String? = null) {
        val roomId = session.currentRoomId ?: return
        val player = session.player ?: return

        // Picking a lock breaks meditation, rest, and stealth
        MeditationUtils.breakMeditation(session, "You stop meditating.")
        RestUtils.breakRest(session, "You stop resting.")
        StealthUtils.breakStealth(session, sessionManager, "Picking a lock reveals your presence!")

        val cooldown = session.skillCooldowns["PICK_LOCK"]
        if (cooldown != null && cooldown > 0) {
            session.send(ServerMessage.SystemMessage("Pick Lock is on cooldown ($cooldown ticks remaining)."))
            return
        }

        val room = worldGraph.getRoom(roomId) ?: return

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
            return
        }

        // Build list of pickable interactables (features with difficulty checks)
        val interactableDefs = worldGraph.getInteractableDefs(roomId)
        val pickableFeatures = interactableDefs.filter { it.difficulty > 0 && it.difficultyCheck.isNotEmpty() }

        // Resolve target
        if (targetId != null && targetId.startsWith("feature:")) {
            val featureId = targetId.removePrefix("feature:")
            val feat = pickableFeatures.find { it.id == featureId }
            if (feat == null) {
                session.send(ServerMessage.SystemMessage("You don't see anything like that to pick."))
                return
            }
            session.pendingSkill = PendingSkill.PickLock(targetId)
            return
        }

        if (targetId != null) {
            // Parse "exit:DIRECTION" format
            if (!targetId.startsWith("exit:")) {
                session.send(ServerMessage.SystemMessage("Invalid lock target."))
                return
            }
            val dirName = targetId.removePrefix("exit:")
            val parsedDir = try { Direction.valueOf(dirName) } catch (_: IllegalArgumentException) {
                session.send(ServerMessage.SystemMessage("Invalid direction: $dirName"))
                return
            }
            if (parsedDir in unpickable && parsedDir in room.lockedExits) {
                session.send(ServerMessage.SystemMessage("This lock cannot be picked — it must be opened another way."))
                return
            }
            val diff = pickableExits[parsedDir]
            if (diff == null) {
                session.send(ServerMessage.SystemMessage("${parsedDir.lockedExitPhrase.replaceFirstChar { it.uppercase() }} is not locked."))
                return
            }
            // Mark lock as discovered so direction pad and map show it
            session.discoverLock(roomId, parsedDir)
            session.pendingSkill = PendingSkill.PickLock(targetId)
            return
        }

        // No target specified — discover all locks for the client
        for (dir in pickableExits.keys) {
            session.discoverLock(roomId, dir)
        }

        val totalTargets = pickableExits.size + pickableFeatures.size
        if (totalTargets == 1) {
            // Only one target exists — auto-pick it
            if (pickableExits.size == 1) {
                val entry = pickableExits.entries.first()
                session.discoverLock(roomId, entry.key)
                session.pendingSkill = PendingSkill.PickLock("exit:${entry.key.name}")
            } else {
                val feat = pickableFeatures.first()
                session.pendingSkill = PendingSkill.PickLock("feature:${feat.id}")
            }
        } else {
            // Multiple targets — resend RoomInfo with newly discovered locks and prompt
            val filteredRoom = RoomFilter.forPlayer(room, session, worldGraph)
            val playersInRoom = sessionManager.getVisiblePlayerInfosInRoom(roomId)
                .filter { it.name != player.name }
            val npcsInRoom = npcManager.getNpcsInRoom(roomId)
            session.send(ServerMessage.RoomInfo(filteredRoom, playersInRoom, npcsInRoom))
            session.send(ServerMessage.SystemMessage("You notice several locks here. Choose a target."))
        }
    }
}

package com.neomud.server.game.commands

import com.neomud.server.game.npc.NpcManager
import com.neomud.server.session.PendingSkill
import com.neomud.server.session.PlayerSession
import com.neomud.shared.model.Direction
import com.neomud.shared.protocol.ServerMessage

class KickCommand(
    private val npcManager: NpcManager
) {
    suspend fun execute(session: PlayerSession, targetId: String?) {
        val roomId = session.currentRoomId ?: return
        session.playerName ?: return
        session.player ?: return

        val cooldown = session.skillCooldowns["KICK"]
        if (cooldown != null && cooldown > 0) {
            session.send(ServerMessage.SystemMessage("Kick is on cooldown ($cooldown ticks remaining)."))
            return
        }

        // Parse compound targetId format: "npcId:DIRECTION"
        val resolvedInput = targetId ?: session.selectedTargetId
        val npcId: String?
        val kickDirection: Direction?

        if (resolvedInput != null && resolvedInput.contains(':')) {
            val lastColon = resolvedInput.lastIndexOf(':')
            npcId = resolvedInput.substring(0, lastColon)
            kickDirection = try {
                Direction.valueOf(resolvedInput.substring(lastColon + 1).uppercase())
            } catch (_: IllegalArgumentException) {
                session.send(ServerMessage.SystemMessage("Invalid kick direction."))
                return
            }
        } else {
            npcId = resolvedInput
            kickDirection = null
        }

        val target = if (npcId != null) {
            val npc = npcManager.getNpcState(npcId)
            if (npc != null && npc.currentRoomId == roomId && npc.hostile) npc else null
        } else {
            npcManager.getLivingHostileNpcsInRoom(roomId).firstOrNull()
        }

        if (target == null) {
            session.send(ServerMessage.SystemMessage("No valid target for kick."))
            return
        }

        if (kickDirection == null) {
            session.send(ServerMessage.SystemMessage("You must choose a direction to kick ${target.name}!"))
            return
        }

        // Queue the skill for next tick
        session.pendingSkill = PendingSkill.Kick(target.id, kickDirection)

        // Kick clears any readied spell (one-off skill)
        session.readiedSpellId = null

        if (!session.attackMode) {
            session.attackMode = true
            session.selectedTargetId = target.id
            session.send(ServerMessage.AttackModeUpdate(true))
        }

        // Aggressive action breaks grace period
        session.combatGraceTicks = 0
    }
}

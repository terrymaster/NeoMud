package com.neomud.server.game.commands

import com.neomud.server.game.npc.NpcManager
import com.neomud.server.session.PendingSkill
import com.neomud.server.session.PlayerSession
import com.neomud.shared.protocol.ServerMessage

class BashCommand(
    private val npcManager: NpcManager
) {
    suspend fun execute(session: PlayerSession, targetId: String?) {
        val roomId = session.currentRoomId ?: return
        val playerName = session.playerName ?: return
        session.player ?: return

        val cooldown = session.skillCooldowns["BASH"]
        if (cooldown != null && cooldown > 0) {
            session.send(ServerMessage.SystemMessage("Bash is on cooldown ($cooldown ticks remaining)."))
            return
        }

        // Resolve target — validate NPC exists, is hostile, and in room
        val resolvedTargetId = targetId ?: session.selectedTargetId
        val target = if (resolvedTargetId != null) {
            val npc = npcManager.getNpcState(resolvedTargetId)
            if (npc != null && npc.currentRoomId == roomId && npc.hostile) npc else null
        } else {
            npcManager.getLivingHostileNpcsInRoom(roomId).firstOrNull()
        }

        if (target == null) {
            session.send(ServerMessage.SystemMessage("No valid target for bash."))
            return
        }

        // Queue the skill for next tick
        session.pendingSkill = PendingSkill.Bash(target.id)

        // Bash clears any readied spell (one-off skill)
        session.readiedSpellId = null

        // Enter attack mode if not already
        if (!session.attackMode) {
            session.attackMode = true
            session.selectedTargetId = target.id
            session.send(ServerMessage.AttackModeUpdate(true))
        }

        // Aggressive action breaks grace period
        session.combatGraceTicks = 0
    }
}

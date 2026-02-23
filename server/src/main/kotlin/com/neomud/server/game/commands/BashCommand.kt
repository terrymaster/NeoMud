package com.neomud.server.game.commands


import com.neomud.server.game.GameConfig
import com.neomud.server.game.npc.NpcManager
import com.neomud.server.session.PlayerSession
import com.neomud.server.session.SessionManager
import com.neomud.shared.protocol.ServerMessage

class BashCommand(
    private val npcManager: NpcManager,
    private val sessionManager: SessionManager,
    private val killHandler: SkillKillHandler
) {
    suspend fun execute(session: PlayerSession, targetId: String?) {
        val roomId = session.currentRoomId ?: return
        val playerName = session.playerName ?: return
        val player = session.player ?: return

        val cooldown = session.skillCooldowns["BASH"]
        if (cooldown != null && cooldown > 0) {
            session.send(ServerMessage.SystemMessage("Bash is on cooldown ($cooldown ticks remaining)."))
            return
        }

        // Resolve target — auto-enter attack mode if hostiles present
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

        // Enter attack mode if not already
        if (!session.attackMode) {
            session.attackMode = true
            session.selectedTargetId = target.id
            session.send(ServerMessage.AttackModeUpdate(true))
        }

        // Track engagement for pursuit
        target.engagedPlayerIds.add(playerName)

        // Aggressive action breaks grace period
        session.combatGraceTicks = 0

        val effStats = session.effectiveStats()
        val damage = effStats.strength + (1..GameConfig.Skills.BASH_DAMAGE_RANGE).random()
        target.currentHp -= damage

        val stunned = (1..100).random() <= GameConfig.Skills.BASH_STUN_CHANCE
        if (stunned) {
            target.stunTicks = GameConfig.Skills.BASH_STUN_TICKS
        }

        session.skillCooldowns["BASH"] = GameConfig.Skills.BASH_COOLDOWN_TICKS

        val message = if (stunned) {
            "You bash ${target.name} for $damage damage, stunning them!"
        } else {
            "You bash ${target.name} for $damage damage!"
        }
        session.send(ServerMessage.SystemMessage(message))

        // Broadcast hit
        sessionManager.broadcastToRoom(
            roomId,
            ServerMessage.CombatHit(
                attackerName = playerName,
                defenderName = target.name,
                damage = damage,
                defenderHp = target.currentHp.coerceAtLeast(0),
                defenderMaxHp = target.maxHp,
                isPlayerDefender = false
            )
        )

        if (target.currentHp <= 0) {
            killHandler.handleNpcKill(target, playerName, roomId, session)
        }
    }
}

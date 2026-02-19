package com.neomud.server.game.commands


import com.neomud.server.game.npc.NpcManager
import com.neomud.server.session.PlayerSession
import com.neomud.server.session.SessionManager
import com.neomud.shared.protocol.ServerMessage

class KickCommand(
    private val npcManager: NpcManager,
    private val sessionManager: SessionManager,
    private val killHandler: SkillKillHandler
) {
    suspend fun execute(session: PlayerSession, targetId: String?) {
        val roomId = session.currentRoomId ?: return
        val playerName = session.playerName ?: return
        val player = session.player ?: return

        val cooldown = session.skillCooldowns["KICK"]
        if (cooldown != null && cooldown > 0) {
            session.send(ServerMessage.SystemMessage("Kick is on cooldown ($cooldown ticks remaining)."))
            return
        }

        val resolvedTargetId = targetId ?: session.selectedTargetId
        val target = if (resolvedTargetId != null) {
            val npc = npcManager.getNpcState(resolvedTargetId)
            if (npc != null && npc.currentRoomId == roomId && npc.hostile) npc else null
        } else {
            npcManager.getLivingHostileNpcsInRoom(roomId).firstOrNull()
        }

        if (target == null) {
            session.send(ServerMessage.SystemMessage("No valid target for kick."))
            return
        }

        if (!session.attackMode) {
            session.attackMode = true
            session.selectedTargetId = target.id
            session.send(ServerMessage.AttackModeUpdate(true))
        }

        val effStats = session.effectiveStats()
        val damage = effStats.agility / 2 + effStats.strength / 2 + 5 + (1..4).random()
        target.currentHp -= damage

        session.skillCooldowns["KICK"] = 2

        session.send(ServerMessage.SystemMessage("You kick ${target.name} for $damage damage!"))

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

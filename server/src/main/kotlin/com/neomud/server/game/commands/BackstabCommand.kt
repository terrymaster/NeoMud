package com.neomud.server.game.commands

import com.neomud.server.game.combat.CombatUtils
import com.neomud.server.game.npc.NpcManager
import com.neomud.server.session.PlayerSession
import com.neomud.server.session.SessionManager
import com.neomud.shared.protocol.ServerMessage

class BackstabCommand(
    private val npcManager: NpcManager,
    private val sessionManager: SessionManager,
    private val killHandler: SkillKillHandler
) {
    suspend fun execute(session: PlayerSession, targetId: String?) {
        val roomId = session.currentRoomId ?: return
        val playerName = session.playerName ?: return
        val player = session.player ?: return

        if (!session.isHidden) {
            session.send(ServerMessage.SystemMessage("You must be hidden to backstab!"))
            return
        }

        val cooldown = session.skillCooldowns["BACKSTAB"]
        if (cooldown != null && cooldown > 0) {
            session.send(ServerMessage.SystemMessage("Backstab is on cooldown ($cooldown ticks remaining)."))
            return
        }

        // Resolve target
        val resolvedTargetId = targetId ?: session.selectedTargetId
        val target = if (resolvedTargetId != null) {
            val npc = npcManager.getNpcState(resolvedTargetId)
            if (npc != null && npc.currentRoomId == roomId && npc.hostile) npc else null
        } else {
            npcManager.getLivingHostileNpcsInRoom(roomId).firstOrNull()
        }

        if (target == null) {
            session.send(ServerMessage.SystemMessage("No valid target for backstab."))
            return
        }

        // Break stealth
        session.isHidden = false
        session.send(ServerMessage.HideModeUpdate(false, "You strike from the shadows!"))
        sessionManager.broadcastToRoom(
            roomId,
            ServerMessage.PlayerEntered(playerName, roomId, session.toPlayerInfo()),
            exclude = playerName
        )

        // Calculate backstab damage: weapon base * 3 multiplier (using buffed stats)
        val effStats = CombatUtils.effectiveStats(player.stats, session.activeEffects.toList())
        val baseDamage = effStats.strength + effStats.agility / 2 + (1..6).random()
        val damage = baseDamage * 3
        target.currentHp -= damage

        session.skillCooldowns["BACKSTAB"] = 4

        // Start attack mode and set target
        session.attackMode = true
        session.selectedTargetId = target.id
        session.send(ServerMessage.AttackModeUpdate(true))

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

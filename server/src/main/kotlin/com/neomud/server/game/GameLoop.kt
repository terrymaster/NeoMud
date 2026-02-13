package com.neomud.server.game

import com.neomud.server.game.npc.NpcManager
import com.neomud.server.session.SessionManager
import com.neomud.shared.model.ActiveEffect
import com.neomud.shared.model.EffectType
import com.neomud.shared.protocol.ServerMessage
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory

class GameLoop(
    private val sessionManager: SessionManager,
    private val npcManager: NpcManager
) {
    private val logger = LoggerFactory.getLogger(GameLoop::class.java)

    suspend fun run() {
        logger.info("Game loop started (1.5s ticks)")
        while (true) {
            delay(1500)
            tick()
        }
    }

    private suspend fun tick() {
        // 1. NPC behavior
        val npcEvents = npcManager.tick()
        for (event in npcEvents) {
            // Broadcast NPC left old room
            if (event.fromRoomId != null && event.direction != null) {
                sessionManager.broadcastToRoom(
                    event.fromRoomId,
                    ServerMessage.NpcLeft(event.npcName, event.fromRoomId, event.direction)
                )
            }
            // Broadcast NPC entered new room
            if (event.toRoomId != null) {
                sessionManager.broadcastToRoom(
                    event.toRoomId,
                    ServerMessage.NpcEntered(event.npcName, event.toRoomId)
                )
            }
        }

        // 2. Process active effects on all authenticated players
        for (session in sessionManager.getAllAuthenticatedSessions()) {
            val effects = session.activeEffects.toList()
            val expired = mutableListOf<ActiveEffect>()

            for (effect in effects) {
                val player = session.player ?: continue
                var newHp = player.currentHp
                val message: String

                when (effect.type) {
                    EffectType.POISON -> {
                        newHp = (newHp - effect.magnitude).coerceAtLeast(1)
                        message = "Poison courses through your veins! (-${effect.magnitude} HP)"
                    }
                    EffectType.HEAL_OVER_TIME -> {
                        newHp = (newHp + effect.magnitude).coerceAtMost(player.maxHp)
                        message = "You feel a warm healing glow. (+${effect.magnitude} HP)"
                    }
                    else -> {
                        message = "${effect.name} continues to affect you."
                    }
                }

                session.player = player.copy(currentHp = newHp)

                try {
                    session.send(ServerMessage.EffectTick(effect.name, message, newHp))
                } catch (_: Exception) { /* session closing */ }

                val updated = effect.copy(remainingTicks = effect.remainingTicks - 1)
                if (updated.remainingTicks <= 0) {
                    expired.add(effect)
                } else {
                    val idx = session.activeEffects.indexOf(effect)
                    if (idx >= 0) session.activeEffects[idx] = updated
                }
            }

            session.activeEffects.removeAll(expired)
        }
    }
}

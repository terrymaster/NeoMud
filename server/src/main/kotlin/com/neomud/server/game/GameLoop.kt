package com.neomud.server.game

import com.neomud.server.game.combat.CombatEvent
import com.neomud.server.game.combat.CombatManager
import com.neomud.server.game.inventory.LootService
import com.neomud.server.game.npc.NpcManager
import com.neomud.server.session.SessionManager
import com.neomud.server.world.LootTableCatalog
import com.neomud.server.world.WorldGraph
import com.neomud.shared.model.ActiveEffect
import com.neomud.shared.model.EffectType
import com.neomud.shared.protocol.ServerMessage
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory

class GameLoop(
    private val sessionManager: SessionManager,
    private val npcManager: NpcManager,
    private val combatManager: CombatManager,
    private val worldGraph: WorldGraph,
    private val lootService: LootService,
    private val lootTableCatalog: LootTableCatalog
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
                    ServerMessage.NpcLeft(event.npcName, event.fromRoomId, event.direction, event.npcId)
                )
            }
            // Broadcast NPC entered new room
            if (event.toRoomId != null) {
                sessionManager.broadcastToRoom(
                    event.toRoomId,
                    ServerMessage.NpcEntered(
                        event.npcName, event.toRoomId,
                        event.npcId, event.hostile, event.currentHp, event.maxHp
                    )
                )
            }
        }

        // 2. Combat
        val combatEvents = combatManager.processCombatTick()
        for (event in combatEvents) {
            when (event) {
                is CombatEvent.Hit -> {
                    sessionManager.broadcastToRoom(
                        event.roomId,
                        ServerMessage.CombatHit(
                            event.attackerName, event.defenderName, event.damage,
                            event.defenderHp, event.defenderMaxHp, event.isPlayerDefender
                        )
                    )
                }
                is CombatEvent.NpcKilled -> {
                    npcManager.markDead(event.npcId)
                    sessionManager.broadcastToRoom(
                        event.roomId,
                        ServerMessage.NpcDied(event.npcId, event.npcName, event.killerName, event.roomId)
                    )

                    // Roll and award loot to killer
                    val lootTable = lootTableCatalog.getLootTable(event.npcId)
                    if (lootTable.isNotEmpty()) {
                        val lootedItems = lootService.rollLoot(lootTable)
                        if (lootedItems.isNotEmpty()) {
                            lootService.awardLoot(event.killerName, lootedItems)
                            val killerSession = sessionManager.getSession(event.killerName)
                            if (killerSession != null) {
                                try {
                                    killerSession.send(ServerMessage.LootReceived(event.npcName, lootedItems))
                                } catch (_: Exception) { }
                            }
                        }
                    }

                    // Auto-disable attack mode for players with no remaining targets
                    for (session in sessionManager.getSessionsInRoom(event.roomId)) {
                        if (session.attackMode) {
                            val remaining = npcManager.getLivingHostileNpcsInRoom(event.roomId)
                            if (remaining.isEmpty()) {
                                session.attackMode = false
                                session.selectedTargetId = null
                                try {
                                    session.send(ServerMessage.AttackModeUpdate(false))
                                } catch (_: Exception) { }
                            } else if (session.selectedTargetId == event.npcId) {
                                session.selectedTargetId = null
                            }
                        }
                    }
                }
                is CombatEvent.PlayerKilled -> {
                    val session = event.playerSession
                    val playerName = session.playerName ?: continue
                    val oldRoomId = session.currentRoomId ?: continue

                    // Send death message to player
                    try {
                        session.send(ServerMessage.PlayerDied(event.killerName, event.respawnRoomId, event.respawnHp))
                    } catch (_: Exception) { }

                    // Disable attack mode
                    session.attackMode = false
                    session.selectedTargetId = null

                    // Broadcast leave from death room
                    sessionManager.broadcastToRoom(
                        oldRoomId,
                        ServerMessage.PlayerLeft(playerName, oldRoomId, com.neomud.shared.model.Direction.NORTH),
                        exclude = playerName
                    )

                    // Respawn
                    session.currentRoomId = event.respawnRoomId
                    session.player = session.player?.copy(
                        currentHp = event.respawnHp,
                        currentRoomId = event.respawnRoomId
                    )

                    // Broadcast enter to spawn room
                    sessionManager.broadcastToRoom(
                        event.respawnRoomId,
                        ServerMessage.PlayerEntered(playerName, event.respawnRoomId),
                        exclude = playerName
                    )

                    // Send room info and map to respawned player
                    val room = worldGraph.getRoom(event.respawnRoomId)
                    if (room != null) {
                        val playersInRoom = sessionManager.getPlayerNamesInRoom(event.respawnRoomId)
                            .filter { it != playerName }
                        val npcsInRoom = npcManager.getNpcsInRoom(event.respawnRoomId)
                        try {
                            session.send(ServerMessage.RoomInfo(room, playersInRoom, npcsInRoom))
                            val mapRooms = worldGraph.getRoomsNear(event.respawnRoomId).map { mapRoom ->
                                mapRoom.copy(
                                    hasPlayers = sessionManager.getPlayerNamesInRoom(mapRoom.id).isNotEmpty(),
                                    hasNpcs = npcManager.getNpcsInRoom(mapRoom.id).isNotEmpty()
                                )
                            }
                            session.send(ServerMessage.MapData(mapRooms, event.respawnRoomId))
                        } catch (_: Exception) { }
                    }
                }
            }
        }

        // 3. Process active effects on all authenticated players
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

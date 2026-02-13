package com.neomud.server.game.combat

import com.neomud.server.game.inventory.EquipmentService
import com.neomud.server.game.npc.NpcManager
import com.neomud.server.game.npc.NpcState
import com.neomud.server.session.PlayerSession
import com.neomud.server.session.SessionManager
import com.neomud.server.world.WorldGraph
import com.neomud.shared.model.RoomId
import org.slf4j.LoggerFactory

sealed class CombatEvent {
    data class Hit(
        val attackerName: String,
        val defenderName: String,
        val damage: Int,
        val defenderHp: Int,
        val defenderMaxHp: Int,
        val isPlayerDefender: Boolean,
        val roomId: RoomId
    ) : CombatEvent()

    data class NpcKilled(
        val npcId: String,
        val npcName: String,
        val killerName: String,
        val roomId: RoomId
    ) : CombatEvent()

    data class PlayerKilled(
        val playerSession: PlayerSession,
        val killerName: String,
        val respawnRoomId: RoomId,
        val respawnHp: Int,
        val respawnMp: Int
    ) : CombatEvent()
}

class CombatManager(
    private val npcManager: NpcManager,
    private val sessionManager: SessionManager,
    private val worldGraph: WorldGraph,
    private val equipmentService: EquipmentService
) {
    private val logger = LoggerFactory.getLogger(CombatManager::class.java)

    fun processCombatTick(): List<CombatEvent> {
        val events = mutableListOf<CombatEvent>()

        // Phase 1: Player attacks
        for (session in sessionManager.getAllAuthenticatedSessions()) {
            if (!session.attackMode) continue
            val roomId = session.currentRoomId ?: continue
            val player = session.player ?: continue

            // Resolve target
            val target = resolveTarget(session, roomId)
            if (target == null) {
                // No valid targets — auto-disable attack mode
                session.attackMode = false
                session.selectedTargetId = null
                continue
            }

            // Calculate damage with equipment bonuses
            val bonuses = equipmentService.getCombatBonuses(player.name)
            val damage = if (bonuses.weaponDamageRange > 0) {
                // Has weapon: strength + damageBonus + random(1..weaponDamageRange)
                player.stats.strength + bonuses.totalDamageBonus + (1..bonuses.weaponDamageRange).random()
            } else {
                // Unarmed fallback: strength + random(1..3)
                player.stats.strength + (1..3).random()
            }
            target.currentHp -= damage

            events.add(CombatEvent.Hit(
                attackerName = player.name,
                defenderName = target.name,
                damage = damage,
                defenderHp = target.currentHp.coerceAtLeast(0),
                defenderMaxHp = target.maxHp,
                isPlayerDefender = false,
                roomId = roomId
            ))

            if (target.currentHp <= 0) {
                events.add(CombatEvent.NpcKilled(
                    npcId = target.id,
                    npcName = target.name,
                    killerName = player.name,
                    roomId = roomId
                ))
                logger.info("${player.name} killed ${target.name} in $roomId")
            }
        }

        // Phase 2: NPC retaliation
        // Collect rooms with authenticated players
        val playersByRoom = mutableMapOf<RoomId, MutableList<PlayerSession>>()
        for (session in sessionManager.getAllAuthenticatedSessions()) {
            val roomId = session.currentRoomId ?: continue
            playersByRoom.getOrPut(roomId) { mutableListOf() }.add(session)
        }

        for ((roomId, playersInRoom) in playersByRoom) {
            val hostiles = npcManager.getLivingHostileNpcsInRoom(roomId)
            for (npc in hostiles) {
                val targetSession = playersInRoom.randomOrNull() ?: continue
                val targetPlayer = targetSession.player ?: continue

                // NPC damage reduced by player's armor, minimum 1
                val playerBonuses = equipmentService.getCombatBonuses(targetPlayer.name)
                val damage = (npc.damage - playerBonuses.totalArmorValue).coerceAtLeast(1)
                val newHp = (targetPlayer.currentHp - damage).coerceAtLeast(0)
                targetSession.player = targetPlayer.copy(currentHp = newHp)

                events.add(CombatEvent.Hit(
                    attackerName = npc.name,
                    defenderName = targetPlayer.name,
                    damage = damage,
                    defenderHp = newHp,
                    defenderMaxHp = targetPlayer.maxHp,
                    isPlayerDefender = true,
                    roomId = roomId
                ))

                if (newHp <= 0) {
                    events.add(CombatEvent.PlayerKilled(
                        playerSession = targetSession,
                        killerName = npc.name,
                        respawnRoomId = worldGraph.defaultSpawnRoom,
                        respawnHp = targetPlayer.maxHp,
                        respawnMp = targetPlayer.maxMp
                    ))
                    logger.info("${npc.name} killed ${targetPlayer.name} in $roomId")
                }
            }
        }

        return events
    }

    private fun resolveTarget(session: PlayerSession, roomId: RoomId): NpcState? {
        // Try selected target first
        val selectedId = session.selectedTargetId
        if (selectedId != null) {
            val selected = npcManager.getNpcState(selectedId)
            if (selected != null && selected.currentRoomId == roomId && selected.hostile) {
                return selected
            }
        }
        // Fall back to random hostile in room
        return npcManager.getLivingHostileNpcsInRoom(roomId).randomOrNull()
    }
}

package com.neomud.server.game.combat

import com.neomud.server.game.inventory.EquipmentService
import com.neomud.server.game.npc.NpcManager
import com.neomud.server.game.npc.NpcState
import com.neomud.server.game.progression.ThresholdBonuses
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
        val roomId: RoomId,
        val isBackstab: Boolean = false,
        val isMiss: Boolean = false,
        val isDodge: Boolean = false,
        val defenderId: String = ""
    ) : CombatEvent()

    data class NpcKilled(
        val npcId: String,
        val npcName: String,
        val killerName: String,
        val roomId: RoomId,
        val npcLevel: Int = 1,
        val xpReward: Long = 0,
        val templateId: String = ""
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

            // Check if this is a backstab (first attack from stealth)
            val isBackstab = session.isHidden

            // Break stealth on attack
            if (session.isHidden) {
                session.isHidden = false
            }

            // Calculate effective stats with active buffs
            val effStats = CombatUtils.effectiveStats(player.stats, session.activeEffects.toList())
            val bonuses = equipmentService.getCombatBonuses(player.name)
            val thresholds = ThresholdBonuses.compute(effStats)

            // To-hit roll: player accuracy vs NPC defense
            val accuracy = CombatUtils.computePlayerAccuracy(effStats, thresholds, player.level, bonuses)
            val npcDefense = CombatUtils.computeNpcDefense(target)

            if (!CombatUtils.rollToHit(accuracy, npcDefense)) {
                // Miss
                events.add(CombatEvent.Hit(
                    attackerName = player.name,
                    defenderName = target.name,
                    damage = 0,
                    defenderHp = target.currentHp,
                    defenderMaxHp = target.maxHp,
                    isPlayerDefender = false,
                    roomId = roomId,
                    isMiss = true,
                    defenderId = target.id
                ))
                continue
            }

            // Evasion roll: NPC dodge chance
            val npcEvasion = CombatUtils.npcEvasion(target)
            if (npcEvasion > 0 && CombatUtils.rollEvasion(npcEvasion)) {
                events.add(CombatEvent.Hit(
                    attackerName = player.name,
                    defenderName = target.name,
                    damage = 0,
                    defenderHp = target.currentHp,
                    defenderMaxHp = target.maxHp,
                    isPlayerDefender = false,
                    roomId = roomId,
                    isDodge = true,
                    defenderId = target.id
                ))
                continue
            }

            // Calculate damage with equipment bonuses + stat thresholds
            var damage = if (bonuses.weaponDamageRange > 0) {
                effStats.strength + bonuses.totalDamageBonus + thresholds.meleeDamageBonus + (1..bonuses.weaponDamageRange).random()
            } else {
                effStats.strength + thresholds.meleeDamageBonus + (1..3).random()
            }

            // Crit check
            if (thresholds.critChance > 0 && Math.random() < thresholds.critChance) {
                damage = (damage * 1.5).toInt()
                logger.info("${player.name} crits for $damage damage!")
            }

            // Backstab: 3x damage multiplier
            if (isBackstab) {
                damage *= 3
                logger.info("${player.name} backstabs ${target.name} for $damage damage in $roomId")
            }

            target.currentHp -= damage

            events.add(CombatEvent.Hit(
                attackerName = player.name,
                defenderName = target.name,
                damage = damage,
                defenderHp = target.currentHp.coerceAtLeast(0),
                defenderMaxHp = target.maxHp,
                isPlayerDefender = false,
                roomId = roomId,
                isBackstab = isBackstab,
                defenderId = target.id
            ))

            if (target.currentHp <= 0) {
                events.add(CombatEvent.NpcKilled(
                    npcId = target.id,
                    npcName = target.name,
                    killerName = player.name,
                    roomId = roomId,
                    npcLevel = target.level,
                    xpReward = target.xpReward,
                    templateId = target.templateId
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
                val visiblePlayers = playersInRoom.filter { !it.isHidden && (it.player?.currentHp ?: 0) > 0 }
                val targetSession = visiblePlayers.randomOrNull() ?: continue
                val targetPlayer = targetSession.player ?: continue

                // Effective stats with buffs
                val effStats = CombatUtils.effectiveStats(targetPlayer.stats, targetSession.activeEffects.toList())
                val playerBonuses = equipmentService.getCombatBonuses(targetPlayer.name)
                val playerThresholds = ThresholdBonuses.compute(effStats)

                // To-hit roll: NPC accuracy vs player defense
                val npcAccuracy = CombatUtils.computeNpcAccuracy(npc)
                val playerDefense = CombatUtils.computePlayerDefense(effStats, playerBonuses, targetPlayer.level)

                if (!CombatUtils.rollToHit(npcAccuracy, playerDefense)) {
                    // NPC misses
                    events.add(CombatEvent.Hit(
                        attackerName = npc.name,
                        defenderName = targetPlayer.name,
                        damage = 0,
                        defenderHp = targetPlayer.currentHp,
                        defenderMaxHp = targetPlayer.maxHp,
                        isPlayerDefender = true,
                        roomId = roomId,
                        isMiss = true,
                        defenderId = targetPlayer.name
                    ))
                    continue
                }

                // Evasion roll: player dodge chance
                val playerEvasion = CombatUtils.playerEvasion(playerThresholds, targetSession.activeEffects.toList())
                if (playerEvasion > 0 && CombatUtils.rollEvasion(playerEvasion)) {
                    events.add(CombatEvent.Hit(
                        attackerName = npc.name,
                        defenderName = targetPlayer.name,
                        damage = 0,
                        defenderHp = targetPlayer.currentHp,
                        defenderMaxHp = targetPlayer.maxHp,
                        isPlayerDefender = true,
                        roomId = roomId,
                        isDodge = true,
                        defenderId = targetPlayer.name
                    ))
                    continue
                }

                // NPC damage with variance, reduced by player's armor, minimum 1
                val variance = maxOf(npc.damage / 3, 1)
                val rawDamage = npc.damage + (1..variance).random()
                val damage = (rawDamage - playerBonuses.totalArmorValue).coerceAtLeast(1)
                val newHp = (targetPlayer.currentHp - damage).coerceAtLeast(0)
                targetSession.player = targetPlayer.copy(currentHp = newHp)

                events.add(CombatEvent.Hit(
                    attackerName = npc.name,
                    defenderName = targetPlayer.name,
                    damage = damage,
                    defenderHp = newHp,
                    defenderMaxHp = targetPlayer.maxHp,
                    isPlayerDefender = true,
                    roomId = roomId,
                    defenderId = targetPlayer.name
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

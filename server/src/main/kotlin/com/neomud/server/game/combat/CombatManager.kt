package com.neomud.server.game.combat

import com.neomud.server.game.GameConfig
import com.neomud.server.game.MeditationUtils
import com.neomud.server.game.MovementTrailManager
import com.neomud.server.game.commands.SpellCommand
import com.neomud.server.game.inventory.EquipmentService
import com.neomud.server.game.npc.NpcManager
import com.neomud.server.game.npc.NpcState
import com.neomud.server.game.progression.ThresholdBonuses
import com.neomud.server.session.PendingSkill
import com.neomud.server.session.PlayerSession
import com.neomud.server.session.SessionManager
import com.neomud.server.world.SkillCatalog
import com.neomud.server.world.SpellCatalog
import com.neomud.server.world.WorldGraph
import com.neomud.shared.model.Direction
import com.neomud.shared.model.RoomId
import com.neomud.shared.protocol.ServerMessage
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
        val isParry: Boolean = false,
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

    data class NpcKnockedBack(
        val npcId: String,
        val npcName: String,
        val fromRoomId: RoomId,
        val toRoomId: RoomId,
        val direction: Direction,
        val kickerName: String,
        val templateId: String,
        val hostile: Boolean,
        val npcCurrentHp: Int,
        val npcMaxHp: Int
    ) : CombatEvent()
}

class CombatManager(
    private val npcManager: NpcManager,
    private val sessionManager: SessionManager,
    private val worldGraph: WorldGraph,
    private val equipmentService: EquipmentService,
    private val skillCatalog: SkillCatalog,
    private val spellCommand: SpellCommand? = null,
    private val spellCatalog: SpellCatalog? = null,
    private val movementTrailManager: MovementTrailManager? = null
) {
    private val logger = LoggerFactory.getLogger(CombatManager::class.java)

    private sealed class Combatant {
        abstract val agility: Int
        abstract val roomId: RoomId

        data class PlayerCombatant(
            val session: PlayerSession,
            override val agility: Int,
            override val roomId: RoomId
        ) : Combatant()

        data class NpcCombatant(
            val npc: NpcState,
            override val agility: Int,
            override val roomId: RoomId
        ) : Combatant()
    }

    suspend fun processCombatTick(): List<CombatEvent> {
        val events = mutableListOf<CombatEvent>()

        // Build unified combatant list
        val combatants = mutableListOf<Combatant>()

        // Collect players in attack mode
        for (session in sessionManager.getAllAuthenticatedSessions()) {
            if (!session.attackMode) continue
            val roomId = session.currentRoomId ?: continue
            session.player ?: continue
            combatants.add(Combatant.PlayerCombatant(session, session.effectiveStats().agility, roomId))
        }

        // Collect rooms with visible players for NPC retaliation
        val playersByRoom = mutableMapOf<RoomId, MutableList<PlayerSession>>()
        for (session in sessionManager.getAllAuthenticatedSessions()) {
            val roomId = session.currentRoomId ?: continue
            playersByRoom.getOrPut(roomId) { mutableListOf() }.add(session)
        }

        for ((roomId, _) in playersByRoom) {
            for (npc in npcManager.getLivingHostileNpcsInRoom(roomId)) {
                combatants.add(Combatant.NpcCombatant(npc, npc.agility, roomId))
            }
        }

        // Sort by agility descending; shuffle first for random tiebreaking
        combatants.shuffle()
        combatants.sortByDescending { it.agility }

        // Resolve each combatant's action in initiative order
        for (combatant in combatants) {
            when (combatant) {
                is Combatant.PlayerCombatant -> {
                    val session = combatant.session
                    val player = session.player ?: continue
                    if (player.currentHp <= 0) continue
                    if (!session.attackMode) continue

                    val roomId = combatant.roomId

                    // Priority 1: Pending bash
                    val pending = session.pendingSkill
                    if (pending is PendingSkill.Bash) {
                        session.pendingSkill = null
                        resolveBash(session, roomId, pending.targetId, events)
                        continue
                    }

                    // Priority 2: Pending kick
                    if (pending is PendingSkill.Kick) {
                        session.pendingSkill = null
                        resolveKick(session, roomId, pending.targetId, pending.direction, events)
                        continue
                    }

                    // Priority 3: Auto-cast readied spell
                    val readiedSpell = session.readiedSpellId
                    if (readiedSpell != null && spellCommand != null) {
                        val spellTarget = resolveTarget(session, roomId)
                        if (spellTarget == null) {
                            session.attackMode = false
                            session.selectedTargetId = null
                            session.readiedSpellId = null
                            continue
                        }
                        if (spellTarget.currentHp <= 0) continue
                        spellCommand.autoCast(session, readiedSpell, spellTarget.id, roomId)

                        // If the spell killed the target, emit NpcKilled so GameLoop handles loot, XP, and attack-mode-disable
                        if (spellTarget.currentHp <= 0) {
                            events.add(CombatEvent.NpcKilled(
                                npcId = spellTarget.id,
                                npcName = spellTarget.name,
                                killerName = player.name,
                                roomId = roomId,
                                npcLevel = spellTarget.level,
                                xpReward = spellTarget.xpReward,
                                templateId = spellTarget.templateId
                            ))
                        }
                        continue
                    }

                    // Priority 4: Melee attack
                    val target = resolveTarget(session, roomId)
                    if (target == null) {
                        session.attackMode = false
                        session.selectedTargetId = null
                        continue
                    }

                    // Skip if target already dead this tick
                    if (target.currentHp <= 0) continue

                    val isBackstab = session.isHidden
                    if (session.isHidden) {
                        session.isHidden = false
                    }

                    val effStats = session.effectiveStats()
                    val bonuses = equipmentService.getCombatBonuses(player.name)
                    val thresholds = ThresholdBonuses.compute(effStats)

                    val accuracy = CombatUtils.computePlayerAccuracy(effStats, thresholds, player.level, bonuses)
                    val npcDefense = CombatUtils.computeNpcDefense(target)

                    if (!CombatUtils.rollToHit(accuracy, npcDefense)) {
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

                    var damage = if (bonuses.weaponDamageRange > 0) {
                        effStats.strength + bonuses.totalDamageBonus + thresholds.meleeDamageBonus + (1..bonuses.weaponDamageRange).random()
                    } else {
                        effStats.strength + thresholds.meleeDamageBonus + (1..GameConfig.Combat.UNARMED_DAMAGE_RANGE).random()
                    }

                    if (thresholds.critChance > 0 && Math.random() < thresholds.critChance) {
                        damage = (damage * GameConfig.Combat.CRIT_DAMAGE_MULTIPLIER).toInt()
                        logger.info("${player.name} crits for $damage damage!")
                    }

                    if (isBackstab) {
                        damage *= GameConfig.Combat.BACKSTAB_DAMAGE_MULTIPLIER
                        logger.info("${player.name} backstabs ${target.name} for $damage damage in $roomId")
                    }

                    target.currentHp -= damage

                    // Track engagement for pursuit
                    target.engagedPlayerIds.add(player.name)

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

                is Combatant.NpcCombatant -> {
                    val npc = combatant.npc
                    if (npc.currentHp <= 0) continue

                    // Stunned NPCs skip their attack
                    if (npc.stunTicks > 0) {
                        npc.stunTicks--
                        continue
                    }

                    val roomId = combatant.roomId
                    val playersInRoom = playersByRoom[roomId] ?: continue
                    val visiblePlayers = playersInRoom.filter { !it.isHidden && !it.godMode && (it.player?.currentHp ?: 0) > 0 && it.combatGraceTicks <= 0 }
                    val targetSession = visiblePlayers.randomOrNull() ?: continue
                    val targetPlayer = targetSession.player ?: continue

                    val effStats = targetSession.effectiveStats()
                    val playerBonuses = equipmentService.getCombatBonuses(targetPlayer.name)

                    val npcAccuracy = CombatUtils.computeNpcAccuracy(npc)
                    val playerDefense = CombatUtils.computePlayerDefense(effStats, playerBonuses, targetPlayer.level)

                    if (!CombatUtils.rollToHit(npcAccuracy, playerDefense)) {
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

                    // Dodge check: class-gated, AGI-scaled full avoidance
                    val hasDodge = skillCatalog.getSkill("DODGE")?.let { skill ->
                        skill.classRestrictions.isEmpty() || targetPlayer.characterClass in skill.classRestrictions
                    } ?: false
                    val playerEvasion = if (hasDodge) CombatUtils.playerEvasion(effStats) else 0.0
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

                    // Parry check: class-gated, STR-scaled partial damage reduction
                    val hasParry = skillCatalog.getSkill("PARRY")?.let { skill ->
                        skill.classRestrictions.isEmpty() || targetPlayer.characterClass in skill.classRestrictions
                    } ?: false
                    val isParry = if (hasParry) {
                        val parryChance = CombatUtils.playerParry(effStats)
                        parryChance > 0 && CombatUtils.rollParry(parryChance)
                    } else false

                    val variance = maxOf(npc.damage / GameConfig.Combat.NPC_VARIANCE_DIVISOR, 1)
                    val rawDamage = npc.damage + (1..variance).random()
                    val parryReduction = if (isParry) CombatUtils.parryReduction(effStats) else 0
                    val damage = (rawDamage - playerBonuses.totalArmorValue - parryReduction).coerceAtLeast(1)
                    val newHp = (targetPlayer.currentHp - damage).coerceAtLeast(0)
                    targetSession.player = targetPlayer.copy(currentHp = newHp)

                    // Taking damage breaks meditation
                    if (targetSession.isMeditating) {
                        MeditationUtils.breakMeditation(targetSession, "You are hit and lose concentration!")
                    }

                    events.add(CombatEvent.Hit(
                        attackerName = npc.name,
                        defenderName = targetPlayer.name,
                        damage = damage,
                        defenderHp = newHp,
                        defenderMaxHp = targetPlayer.maxHp,
                        isPlayerDefender = true,
                        roomId = roomId,
                        isParry = isParry,
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
        }

        return events
    }

    private suspend fun resolveBash(
        session: PlayerSession,
        roomId: RoomId,
        targetId: String?,
        events: MutableList<CombatEvent>
    ) {
        val player = session.player ?: return
        val playerName = session.playerName ?: return

        // Re-resolve target (may have died/moved since queue time)
        val target = if (targetId != null) {
            val npc = npcManager.getNpcState(targetId)
            if (npc != null && npc.currentRoomId == roomId && npc.hostile && npc.currentHp > 0) npc else null
        } else null

        if (target == null) {
            try { session.send(ServerMessage.SystemMessage("No valid target for bash.")) } catch (_: Exception) {}
            return
        }

        val effStats = session.effectiveStats()
        val damage = effStats.strength + (1..GameConfig.Skills.BASH_DAMAGE_RANGE).random()
        target.currentHp -= damage

        val stunned = (1..100).random() <= GameConfig.Skills.BASH_STUN_CHANCE
        if (stunned) {
            target.stunTicks = GameConfig.Skills.BASH_STUN_TICKS
        }

        session.skillCooldowns["BASH"] = GameConfig.Skills.BASH_COOLDOWN_TICKS

        // Track engagement for pursuit
        target.engagedPlayerIds.add(playerName)

        // Broadcast skill effect to room (3rd person flavor text + NPC HP)
        val effectMsg = if (stunned) {
            "$playerName bashes ${target.name} for $damage damage, stunning them!"
        } else {
            "$playerName bashes ${target.name} for $damage damage!"
        }
        sessionManager.broadcastToRoom(roomId, ServerMessage.SkillEffect(
            userName = playerName,
            skillName = "BASH",
            targetName = target.name,
            targetId = target.id,
            damage = damage,
            targetHp = target.currentHp.coerceAtLeast(0),
            targetMaxHp = target.maxHp,
            message = effectMsg
        ))

        if (target.currentHp <= 0) {
            events.add(CombatEvent.NpcKilled(
                npcId = target.id,
                npcName = target.name,
                killerName = playerName,
                roomId = roomId,
                npcLevel = target.level,
                xpReward = target.xpReward,
                templateId = target.templateId
            ))
            logger.info("$playerName bash-killed ${target.name} in $roomId")
        }
    }

    private suspend fun resolveKick(
        session: PlayerSession,
        roomId: RoomId,
        targetId: String?,
        kickDirection: Direction,
        events: MutableList<CombatEvent>
    ) {
        val player = session.player ?: return
        val playerName = session.playerName ?: return

        // Re-resolve target
        val target = if (targetId != null) {
            val npc = npcManager.getNpcState(targetId)
            if (npc != null && npc.currentRoomId == roomId && npc.hostile && npc.currentHp > 0) npc else null
        } else null

        if (target == null) {
            try { session.send(ServerMessage.SystemMessage("No valid target for kick.")) } catch (_: Exception) {}
            return
        }

        val effStats = session.effectiveStats()
        val damage = effStats.strength / 4 + effStats.agility / 4 + (1..GameConfig.Skills.KICK_DAMAGE_RANGE).random()
        target.currentHp -= damage

        session.skillCooldowns["KICK"] = GameConfig.Skills.KICK_COOLDOWN_TICKS

        // Track engagement
        target.engagedPlayerIds.add(playerName)

        // Check for kill before knockback
        if (target.currentHp <= 0) {
            val effectMsg = "$playerName kicks ${target.name} for $damage damage, finishing them off!"
            sessionManager.broadcastToRoom(roomId, ServerMessage.SkillEffect(
                userName = playerName,
                skillName = "KICK",
                targetName = target.name,
                targetId = target.id,
                damage = damage,
                targetHp = 0,
                targetMaxHp = target.maxHp,
                message = effectMsg
            ))
            events.add(CombatEvent.NpcKilled(
                npcId = target.id,
                npcName = target.name,
                killerName = playerName,
                roomId = roomId,
                npcLevel = target.level,
                xpReward = target.xpReward,
                templateId = target.templateId
            ))
            logger.info("$playerName kick-killed ${target.name} in $roomId")
            return
        }

        // Validate knockback direction
        val currentRoom = worldGraph.getRoom(roomId)
        val targetRoomId = currentRoom?.exits?.get(kickDirection)
        val isLocked = currentRoom?.lockedExits?.containsKey(kickDirection) == true
        val isUp = kickDirection == Direction.UP

        if (targetRoomId != null && !isLocked && !isUp) {
            // Successful knockback
            val effectMsg = "$playerName kicks ${target.name} for $damage damage, sending them flying ${kickDirection.name.lowercase()}!"
            sessionManager.broadcastToRoom(roomId, ServerMessage.SkillEffect(
                userName = playerName,
                skillName = "KICK",
                targetName = target.name,
                targetId = target.id,
                damage = damage,
                targetHp = target.currentHp.coerceAtLeast(0),
                targetMaxHp = target.maxHp,
                message = effectMsg
            ))

            // Move the NPC
            val moveEvent = npcManager.moveNpc(target.id, targetRoomId)
            if (moveEvent != null) {
                target.stunTicks = GameConfig.Skills.KICK_KNOCKBACK_STUN_TICKS

                events.add(CombatEvent.NpcKnockedBack(
                    npcId = target.id,
                    npcName = target.name,
                    fromRoomId = roomId,
                    toRoomId = targetRoomId,
                    direction = kickDirection,
                    kickerName = playerName,
                    templateId = target.templateId,
                    hostile = target.hostile,
                    npcCurrentHp = target.currentHp,
                    npcMaxHp = target.maxHp
                ))

                // Trigger pursuit
                val trailMgr = movementTrailManager
                if (trailMgr != null) {
                    npcManager.engagePursuit(target.id, playerName, trailMgr, sessionManager)
                }
            }
        } else {
            // Wall slam — damage applies but NPC stays
            val reason = when {
                isUp -> "into the ceiling"
                isLocked -> "against the locked door"
                else -> "against the wall"
            }
            val effectMsg = "$playerName kicks ${target.name} for $damage damage — they slam $reason!"
            sessionManager.broadcastToRoom(roomId, ServerMessage.SkillEffect(
                userName = playerName,
                skillName = "KICK",
                targetName = target.name,
                targetId = target.id,
                damage = damage,
                targetHp = target.currentHp.coerceAtLeast(0),
                targetMaxHp = target.maxHp,
                message = effectMsg
            ))
        }
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

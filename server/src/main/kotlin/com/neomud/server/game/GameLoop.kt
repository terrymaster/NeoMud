package com.neomud.server.game

import com.neomud.server.game.combat.CombatEvent
import com.neomud.server.game.combat.CombatManager
import com.neomud.server.game.inventory.LootService
import com.neomud.server.game.inventory.RoomItemManager
import com.neomud.server.game.npc.NpcManager
import com.neomud.server.game.npc.NpcState
import com.neomud.server.game.progression.XpCalculator
import com.neomud.server.game.skills.SkillCheck
import com.neomud.server.persistence.repository.PlayerRepository
import com.neomud.server.session.PendingSkill
import com.neomud.server.session.PlayerSession
import com.neomud.server.session.SessionManager
import com.neomud.server.world.ClassCatalog
import com.neomud.server.world.LootTableCatalog
import com.neomud.server.world.SkillCatalog
import com.neomud.server.world.WorldGraph
import com.neomud.shared.model.ActiveEffect
import com.neomud.shared.model.GroundItem
import com.neomud.shared.model.RoomId
import com.neomud.shared.protocol.ServerMessage
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory

class GameLoop(
    private val sessionManager: SessionManager,
    private val npcManager: NpcManager,
    private val combatManager: CombatManager,
    private val worldGraph: WorldGraph,
    private val lootService: LootService,
    private val lootTableCatalog: LootTableCatalog,
    private val roomItemManager: RoomItemManager,
    private val playerRepository: PlayerRepository,
    private val skillCatalog: SkillCatalog,
    private val classCatalog: ClassCatalog,
    private val movementTrailManager: MovementTrailManager? = null
) {
    private val logger = LoggerFactory.getLogger(GameLoop::class.java)

    suspend fun run() {
        logger.info("Game loop started (${GameConfig.Tick.INTERVAL_MS}ms ticks)")
        while (true) {
            delay(GameConfig.Tick.INTERVAL_MS)
            tick()
        }
    }

    private suspend fun tick() = GameStateLock.withLock {
        // 0. Safety net: force-respawn any player stuck at 0 HP
        for (session in sessionManager.getAllAuthenticatedSessions()) {
            val player = session.player ?: continue
            if (player.currentHp <= 0) {
                val playerName = session.playerName ?: continue
                val respawnRoomId = worldGraph.defaultSpawnRoom
                logger.warn("Safety respawn: $playerName had ${player.currentHp} HP, forcing respawn")

                session.attackMode = false
                session.selectedTargetId = null
                session.readiedSpellId = null
                session.pendingSkill = null
                session.isHidden = false
                session.isMeditating = false
                session.isResting = false
                session.activeEffects.clear()

                val oldRoomId = session.currentRoomId
                session.currentRoomId = respawnRoomId
                session.player = player.copy(
                    currentHp = player.maxHp,
                    currentMp = player.maxMp,
                    currentRoomId = respawnRoomId
                )

                try {
                    session.send(ServerMessage.PlayerDied("unknown", respawnRoomId, player.maxHp, player.maxMp))
                    if (oldRoomId != null && oldRoomId != respawnRoomId) {
                        sessionManager.broadcastToRoom(
                            oldRoomId,
                            ServerMessage.PlayerLeft(playerName, oldRoomId, com.neomud.shared.model.Direction.NORTH),
                            exclude = playerName
                        )
                    }
                    sessionManager.broadcastToRoom(
                        respawnRoomId,
                        ServerMessage.PlayerEntered(playerName, respawnRoomId, session.toPlayerInfo()),
                        exclude = playerName
                    )
                    val room = worldGraph.getRoom(respawnRoomId)
                    if (room != null) {
                        val playersInRoom = sessionManager.getVisiblePlayerInfosInRoom(respawnRoomId).filter { it.name != playerName }
                        val npcsInRoom = npcManager.getNpcsInRoom(respawnRoomId)
                        session.send(ServerMessage.RoomInfo(room, playersInRoom, npcsInRoom))
                    }
                    session.player?.let { p -> playerRepository.savePlayerState(p) }
                } catch (_: Exception) { }
            }
        }

        // 1. NPC behavior
        // Collect rooms with visible players so hostile NPCs stay to fight
        val roomsWithVisiblePlayers = sessionManager.getAllAuthenticatedSessions()
            .filter { !it.isHidden && it.combatGraceTicks <= 0 && (it.player?.currentHp ?: 0) > 0 }
            .mapNotNull { it.currentRoomId }
            .toSet()
        val npcEvents = npcManager.tick(roomsWithVisiblePlayers)
        for (event in npcEvents) {
            // Record NPC movement trail
            if (event.fromRoomId != null && event.direction != null) {
                movementTrailManager?.recordTrail(event.fromRoomId, TrailEntry(
                    event.npcName, event.npcId, event.direction, System.currentTimeMillis(), isPlayer = false
                ))
            }
            // Broadcast NPC left old room
            if (event.fromRoomId != null && event.direction != null) {
                sessionManager.broadcastToRoom(
                    event.fromRoomId,
                    ServerMessage.NpcLeft(event.npcName, event.fromRoomId, event.direction, event.npcId)
                )
                // Auto-disable attack mode for players who lost their target
                for (session in sessionManager.getSessionsInRoom(event.fromRoomId)) {
                    if (session.attackMode) {
                        if (session.selectedTargetId == event.npcId) {
                            session.selectedTargetId = null
                        }
                        val remaining = npcManager.getLivingHostileNpcsInRoom(event.fromRoomId)
                        if (remaining.isEmpty()) {
                            session.attackMode = false
                            session.selectedTargetId = null
                            session.readiedSpellId = null
                            try {
                                session.send(ServerMessage.AttackModeUpdate(false))
                            } catch (_: Exception) { }
                        }
                    }
                }
            }
            // Broadcast NPC entered new room
            val isSpawn = event.fromRoomId == null
            if (event.toRoomId != null) {
                sessionManager.broadcastToRoom(
                    event.toRoomId,
                    ServerMessage.NpcEntered(
                        event.npcName, event.toRoomId,
                        event.npcId, event.hostile, event.currentHp, event.maxHp,
                        spawned = isSpawn,
                        templateId = event.templateId
                    )
                )

                // Any NPC entering a room does a perception check against hidden players
                val npc = npcManager.getNpcState(event.npcId)
                if (npc != null) {
                    npcPerceptionScan(npc, event.toRoomId)
                }
            }
        }

        // 1b. Decrement combat grace ticks
        for (session in sessionManager.getAllAuthenticatedSessions()) {
            if (session.combatGraceTicks > 0) {
                session.combatGraceTicks--
            }
        }

        // 1c. Resolve non-combat pending skills
        for (session in sessionManager.getAllAuthenticatedSessions()) {
            when (val skill = session.pendingSkill) {
                is PendingSkill.Meditate -> {
                    session.pendingSkill = null
                    resolveMeditate(session)
                }
                is PendingSkill.Rest -> {
                    session.pendingSkill = null
                    resolveRest(session)
                }
                is PendingSkill.Track -> {
                    session.pendingSkill = null
                    resolveTrack(session, skill.targetId)
                }
                else -> {} // Bash/Kick handled by CombatManager
            }
        }

        // 2. Combat
        val combatEvents = combatManager.processCombatTick()
        for (event in combatEvents) {
            when (event) {
                is CombatEvent.Hit -> {
                    // If this was a backstab, notify the attacker their stealth broke
                    if (event.isBackstab) {
                        val attackerSession = sessionManager.getSession(event.attackerName)
                        if (attackerSession != null) {
                            try {
                                attackerSession.send(ServerMessage.StealthUpdate(false, "You strike from the shadows!"))
                            } catch (_: Exception) { }
                            // Reveal to other players
                            sessionManager.broadcastToRoom(
                                event.roomId,
                                ServerMessage.PlayerEntered(event.attackerName, event.roomId, attackerSession.toPlayerInfo()),
                                exclude = event.attackerName
                            )
                        }
                    }
                    sessionManager.broadcastToRoom(
                        event.roomId,
                        ServerMessage.CombatHit(
                            attackerName = event.attackerName,
                            defenderName = event.defenderName,
                            damage = event.damage,
                            defenderHp = event.defenderHp,
                            defenderMaxHp = event.defenderMaxHp,
                            isPlayerDefender = event.isPlayerDefender,
                            isBackstab = event.isBackstab,
                            isMiss = event.isMiss,
                            isDodge = event.isDodge,
                            isParry = event.isParry,
                            defenderId = event.defenderId
                        )
                    )
                }
                is CombatEvent.NpcKilled -> {
                    if (!npcManager.markDead(event.npcId)) continue
                    sessionManager.broadcastToRoom(
                        event.roomId,
                        ServerMessage.NpcDied(event.npcId, event.npcName, event.killerName, event.roomId)
                    )

                    // Roll loot and drop on ground (use templateId for spawned copies)
                    val lootKey = event.templateId.ifEmpty { event.npcId }
                    val lootTable = lootTableCatalog.getLootTable(lootKey)
                    val coinDrop = lootTableCatalog.getCoinDrop(lootKey)
                    val lootedItems = lootService.rollLoot(lootTable)
                    val coins = lootService.rollCoins(coinDrop)

                    if (lootedItems.isNotEmpty() || !coins.isEmpty()) {
                        // Place on ground
                        if (lootedItems.isNotEmpty()) {
                            roomItemManager.addItems(
                                event.roomId,
                                lootedItems.map { GroundItem(it.itemId, it.quantity) }
                            )
                        }
                        roomItemManager.addCoins(event.roomId, coins)

                        // Broadcast loot dropped
                        sessionManager.broadcastToRoom(
                            event.roomId,
                            ServerMessage.LootDropped(event.npcName, lootedItems, coins)
                        )

                        // Broadcast updated ground state
                        val groundItems = roomItemManager.getGroundItems(event.roomId)
                        val groundCoins = roomItemManager.getGroundCoins(event.roomId)
                        sessionManager.broadcastToRoom(
                            event.roomId,
                            ServerMessage.RoomItemsUpdate(groundItems, groundCoins)
                        )
                    }

                    // Award XP to killer
                    if (event.xpReward > 0) {
                        val killerSession = sessionManager.getSession(event.killerName)
                        val killerPlayer = killerSession?.player
                        if (killerSession != null && killerPlayer != null) {
                            val xpGained = XpCalculator.xpForKill(event.npcLevel, killerPlayer.level, event.xpReward)
                            val newXp = killerPlayer.currentXp + xpGained
                            killerSession.player = killerPlayer.copy(currentXp = newXp)
                            try {
                                killerSession.send(ServerMessage.XpGained(xpGained, newXp, killerPlayer.xpToNextLevel))
                                if (XpCalculator.isReadyToLevel(newXp, killerPlayer.xpToNextLevel, killerPlayer.level)) {
                                    killerSession.send(ServerMessage.SystemMessage("You have enough experience to level up! Visit a trainer."))
                                }
                                playerRepository.savePlayerState(killerSession.player!!)
                            } catch (_: Exception) { }
                        }
                    }

                    // Auto-disable attack mode for players with no remaining targets
                    for (session in sessionManager.getSessionsInRoom(event.roomId)) {
                        if (session.attackMode) {
                            val remaining = npcManager.getLivingHostileNpcsInRoom(event.roomId)
                            if (remaining.isEmpty()) {
                                session.attackMode = false
                                session.selectedTargetId = null
                                session.readiedSpellId = null
                                try {
                                    session.send(ServerMessage.AttackModeUpdate(false))
                                } catch (_: Exception) { }
                            } else if (session.selectedTargetId == event.npcId) {
                                session.selectedTargetId = null
                            }
                        }
                    }
                }
                is CombatEvent.NpcKnockedBack -> {
                    // Broadcast NPC left old room
                    sessionManager.broadcastToRoom(
                        event.fromRoomId,
                        ServerMessage.NpcLeft(event.npcName, event.fromRoomId, event.direction, event.npcId)
                    )

                    // Auto-disable attack mode for players who lost their target in old room
                    for (session in sessionManager.getSessionsInRoom(event.fromRoomId)) {
                        if (session.attackMode && session.selectedTargetId == event.npcId) {
                            session.selectedTargetId = null
                            val remaining = npcManager.getLivingHostileNpcsInRoom(event.fromRoomId)
                            if (remaining.isEmpty()) {
                                session.attackMode = false
                                session.selectedTargetId = null
                                session.readiedSpellId = null
                                try { session.send(ServerMessage.AttackModeUpdate(false)) } catch (_: Exception) {}
                            }
                        }
                    }

                    // Broadcast NPC entered new room
                    sessionManager.broadcastToRoom(
                        event.toRoomId,
                        ServerMessage.NpcEntered(
                            event.npcName, event.toRoomId,
                            event.npcId, event.hostile, event.npcCurrentHp, event.npcMaxHp,
                            spawned = false,
                            templateId = event.templateId
                        )
                    )

                    // Update map data for players in both rooms
                    updateMapForPlayersInRoom(event.fromRoomId)
                    updateMapForPlayersInRoom(event.toRoomId)
                }
                is CombatEvent.PlayerKilled -> {
                    val session = event.playerSession
                    val playerName = session.playerName ?: continue
                    val oldRoomId = session.currentRoomId ?: continue

                    // Send death message to player
                    try {
                        session.send(ServerMessage.PlayerDied(event.killerName, event.respawnRoomId, event.respawnHp, event.respawnMp))
                    } catch (_: Exception) { }

                    // Disable attack mode, stealth, meditation, rest, readied spell, and pending skill
                    session.attackMode = false
                    session.selectedTargetId = null
                    session.readiedSpellId = null
                    session.pendingSkill = null
                    session.isHidden = false
                    session.isMeditating = false
                    session.isResting = false

                    // Broadcast leave from death room
                    sessionManager.broadcastToRoom(
                        oldRoomId,
                        ServerMessage.PlayerLeft(playerName, oldRoomId, com.neomud.shared.model.Direction.NORTH),
                        exclude = playerName
                    )

                    // XP penalty on death
                    val player = session.player
                    val xpPenalty = if (player != null) (player.currentXp * GameConfig.Progression.DEATH_XP_LOSS_PERCENT).toLong() else 0L

                    // Respawn
                    session.currentRoomId = event.respawnRoomId
                    session.player = session.player?.copy(
                        currentHp = event.respawnHp,
                        currentMp = event.respawnMp,
                        currentRoomId = event.respawnRoomId,
                        currentXp = ((player?.currentXp ?: 0L) - xpPenalty).coerceAtLeast(0L)
                    )
                    session.activeEffects.clear()

                    // Broadcast enter to spawn room
                    sessionManager.broadcastToRoom(
                        event.respawnRoomId,
                        ServerMessage.PlayerEntered(playerName, event.respawnRoomId, session.toPlayerInfo()),
                        exclude = playerName
                    )

                    // Send room info and map to respawned player
                    val room = worldGraph.getRoom(event.respawnRoomId)
                    if (room != null) {
                        val playersInRoom = sessionManager.getVisiblePlayerInfosInRoom(event.respawnRoomId)
                            .filter { it.name != playerName }
                        val npcsInRoom = npcManager.getNpcsInRoom(event.respawnRoomId)
                        try {
                            session.send(ServerMessage.RoomInfo(room, playersInRoom, npcsInRoom))
                            val mapRooms = MapRoomFilter.enrichForPlayer(
                                worldGraph.getRoomsNear(event.respawnRoomId), session, worldGraph, sessionManager, npcManager
                            )
                            session.send(ServerMessage.MapData(mapRooms, event.respawnRoomId))

                            // Send ground items for respawn room
                            val groundItems = roomItemManager.getGroundItems(event.respawnRoomId)
                            val groundCoins = roomItemManager.getGroundCoins(event.respawnRoomId)
                            session.send(ServerMessage.RoomItemsUpdate(groundItems, groundCoins))
                        } catch (_: Exception) { }
                    }

                    // Persist death state
                    session.player?.let { p ->
                        try { playerRepository.savePlayerState(p) } catch (_: Exception) { }
                    }
                }
            }
        }

        // 3. Tick down skill cooldowns
        for (session in sessionManager.getAllAuthenticatedSessions()) {
            val iter = session.skillCooldowns.iterator()
            while (iter.hasNext()) {
                val entry = iter.next()
                entry.setValue(entry.value - 1)
                if (entry.value <= 0) iter.remove()
            }
        }

        // 3b. Tick exit reset timers (lock re-lock, hidden re-hide)
        val resetEvents = worldGraph.tickResetTimers()
        for (event in resetEvents) {
            when (event) {
                is com.neomud.server.world.ExitResetEvent.Relocked -> {
                    sessionManager.broadcastToRoom(event.roomId,
                        ServerMessage.SystemMessage("You hear a click as the ${event.direction.name.lowercase()} door locks."))
                    resendRoomInfoToPlayersInRoom(event.roomId)
                }
                is com.neomud.server.world.ExitResetEvent.Rehidden -> {
                    for (session in sessionManager.getSessionsInRoom(event.roomId)) {
                        session.forgetExit(event.roomId, event.direction)
                    }
                    sessionManager.broadcastToRoom(event.roomId,
                        ServerMessage.SystemMessage("The passage to the ${event.direction.name.lowercase()} seems to vanish..."))
                    resendRoomInfoToPlayersInRoom(event.roomId)
                }
            }
        }

        // 3c. Tick interactable reset timers
        val interactableResets = worldGraph.tickInteractableTimers()
        for (event in interactableResets) {
            sessionManager.broadcastToRoom(event.roomId,
                ServerMessage.SystemMessage("The ${event.featureLabel} resets..."))
            resendRoomInfoToPlayersInRoom(event.roomId)
        }

        // 3d. Tick per-player interactable cooldowns
        for (session in sessionManager.getAllAuthenticatedSessions()) {
            val iter = session.interactableCooldowns.iterator()
            while (iter.hasNext()) {
                val entry = iter.next()
                entry.setValue(entry.value - 1)
                if (entry.value <= 0) iter.remove()
            }
        }

        // 4. NPC perception scans for hidden players
        npcPerceptionPhase()

        // 4b. Player perception scans for hidden players
        playerPerceptionPhase()

        // 4c. Meditation MP restoration
        meditationPhase()

        // 4d. Rest HP restoration
        restPhase()

        // 5. Process active effects on all authenticated players
        for (session in sessionManager.getAllAuthenticatedSessions()) {
            val effects = session.activeEffects.toList()
            if (effects.isEmpty()) continue
            val expired = mutableListOf<ActiveEffect>()

            for (effect in effects) {
                val player = session.player ?: continue

                val result = EffectApplicator.applyEffect(effect.type.name, effect.magnitude, "", player)
                if (result != null) {
                    session.player = player.copy(currentHp = result.newHp, currentMp = result.newMp)
                    try {
                        session.send(ServerMessage.EffectTick(effect.name, result.message, result.newHp, newMp = result.newMp))
                    } catch (_: Exception) { /* session closing */ }
                } else {
                    // Stat buff effects or no-op effects
                    val message = "${effect.name} continues to affect you."
                    try {
                        session.send(ServerMessage.EffectTick(effect.name, message, player.currentHp))
                    } catch (_: Exception) { /* session closing */ }
                }

                val updated = effect.copy(remainingTicks = effect.remainingTicks - 1)
                if (updated.remainingTicks <= 0) {
                    expired.add(effect)
                } else {
                    val idx = session.activeEffects.indexOf(effect)
                    if (idx >= 0) session.activeEffects[idx] = updated
                }
            }

            session.activeEffects.removeAll(expired)

            // Send updated active effects list to client
            try {
                session.send(ServerMessage.ActiveEffectsUpdate(session.activeEffects.toList()))
            } catch (_: Exception) { /* session closing */ }
        }

        // 6. Room effects
        for (session in sessionManager.getAllAuthenticatedSessions()) {
            val player = session.player ?: continue
            val roomId = session.currentRoomId ?: continue
            val room = worldGraph.getRoom(roomId) ?: continue
            for (effect in room.effects) {
                val p = session.player ?: continue
                val result = EffectApplicator.applyEffect(effect.type, effect.value, effect.message, p) ?: continue
                session.player = p.copy(currentHp = result.newHp, currentMp = result.newMp)
                val effectName = effect.type.lowercase().replaceFirstChar { it.uppercase() } + " Aura"
                try {
                    session.send(ServerMessage.EffectTick(effectName, result.message, result.newHp, effect.sound, result.newMp))
                } catch (_: Exception) { /* session closing */ }
            }
        }

        // 7. Prune stale movement trails
        movementTrailManager?.pruneStale()
    }

    /**
     * Periodic scan: each NPC in a room with hidden players rolls perception.
     * Only one check per NPC per tick to avoid spam.
     */
    private suspend fun npcPerceptionPhase() {
        // Collect rooms that have hidden players
        val hiddenByRoom = mutableMapOf<RoomId, MutableList<PlayerSession>>()
        for (session in sessionManager.getAllAuthenticatedSessions()) {
            if (session.isHidden) {
                val roomId = session.currentRoomId ?: continue
                hiddenByRoom.getOrPut(roomId) { mutableListOf() }.add(session)
            }
        }

        for ((roomId, hiddenPlayers) in hiddenByRoom) {
            val npcsInRoom = npcManager.getLivingNpcsInRoom(roomId)
            for (npc in npcsInRoom) {
                for (session in hiddenPlayers) {
                    if (!session.isHidden) continue // may have been revealed by a prior NPC this tick
                    npcPerceptionCheck(npc, session, roomId)
                }
            }
        }
    }

    /**
     * Immediate scan when an NPC enters a room - checks all hidden players there.
     */
    private suspend fun npcPerceptionScan(npc: NpcState, roomId: RoomId) {
        val hiddenPlayers = sessionManager.getSessionsInRoom(roomId).filter { it.isHidden }
        for (session in hiddenPlayers) {
            npcPerceptionCheck(npc, session, roomId)
        }
    }

    /**
     * NPC perception vs player stealth.
     * NPC roll: perception + level + d20
     * Player passive stealth DC: AGI + WIL/2 + level/2 + 10
     */
    private suspend fun npcPerceptionCheck(npc: NpcState, session: PlayerSession, roomId: RoomId) {
        val player = session.player ?: return
        val playerName = session.playerName ?: return

        val npcRoll = npc.perception + npc.level + (1..GameConfig.Stealth.PERCEPTION_DICE_SIZE).random()
        val effStats = session.effectiveStats()
        val stealthDc = effStats.agility + effStats.willpower / GameConfig.Stealth.DC_WIL_DIVISOR + player.level / GameConfig.Stealth.DC_LEVEL_DIVISOR + GameConfig.Stealth.DC_BASE

        if (npcRoll >= stealthDc) {
            // Detected!
            session.isHidden = false
            try {
                session.send(ServerMessage.StealthUpdate(false, "${npc.name} spots you lurking in the shadows!"))
            } catch (_: Exception) { }

            // Reveal to other players in the room
            sessionManager.broadcastToRoom(
                roomId,
                ServerMessage.PlayerEntered(playerName, roomId, session.toPlayerInfo()),
                exclude = playerName
            )
        }
    }

    private suspend fun resendRoomInfoToPlayersInRoom(roomId: RoomId) {
        val room = worldGraph.getRoom(roomId) ?: return
        for (session in sessionManager.getSessionsInRoom(roomId)) {
            val playerName = session.playerName ?: continue
            val filteredRoom = RoomFilter.forPlayer(room, session, worldGraph)
            val playersInRoom = sessionManager.getVisiblePlayerInfosInRoom(roomId)
                .filter { it.name != playerName }
            val npcsInRoom = npcManager.getNpcsInRoom(roomId)
            try {
                session.send(ServerMessage.RoomInfo(filteredRoom, playersInRoom, npcsInRoom))
                val mapRooms = MapRoomFilter.enrichForPlayer(
                    worldGraph.getRoomsNear(roomId), session, worldGraph, sessionManager, npcManager
                )
                session.send(ServerMessage.MapData(mapRooms, roomId))
            } catch (_: Exception) { }
        }
    }

    private suspend fun resolveRest(session: PlayerSession) {
        val player = session.player ?: return

        // Break stealth if hidden
        StealthUtils.breakStealth(session, sessionManager, "Resting reveals your presence!")

        // Skill check: health + willpower/2 + level/2 + d20 vs difficulty
        val skillDef = skillCatalog.getSkill("REST")
        if (skillDef == null) {
            try { session.send(ServerMessage.SystemMessage("Rest skill not found.")) } catch (_: Exception) {}
            return
        }

        // Cooldown applies regardless of pass/fail
        session.skillCooldowns["REST"] = skillDef.cooldownTicks

        val effStats = session.effectiveStats()
        val result = SkillCheck.check(skillDef, effStats, player.level)

        if (!result.success) {
            try { session.send(ServerMessage.RestUpdate(false, "You fail to settle into a restful state. (roll: ${result.roll})")) } catch (_: Exception) {}
            return
        }

        // Break meditation if meditating (mutual exclusion)
        MeditationUtils.breakMeditation(session, "You stop meditating to rest.")

        session.isResting = true
        try { session.send(ServerMessage.RestUpdate(true, "You settle into a restful state. (roll: ${result.roll})")) } catch (_: Exception) {}
    }

    private suspend fun resolveMeditate(session: PlayerSession) {
        val player = session.player ?: return

        // Break stealth if hidden
        StealthUtils.breakStealth(session, sessionManager, "Meditating reveals your presence!")

        // Skill check: WIL + INT/2 + level/2 + d20 vs difficulty
        val skillDef = skillCatalog.getSkill("MEDITATE")
        if (skillDef == null) {
            try { session.send(ServerMessage.SystemMessage("Meditate skill not found.")) } catch (_: Exception) {}
            return
        }

        // Cooldown applies regardless of pass/fail
        session.skillCooldowns["MEDITATE"] = skillDef.cooldownTicks

        val effStats = session.effectiveStats()
        val result = SkillCheck.check(skillDef, effStats, player.level)

        if (!result.success) {
            try { session.send(ServerMessage.MeditateUpdate(false, "You fail to focus your mind. (roll: ${result.roll})")) } catch (_: Exception) {}
            return
        }

        session.isMeditating = true
        try { session.send(ServerMessage.MeditateUpdate(true, "You enter a meditative state. (roll: ${result.roll})")) } catch (_: Exception) {}
    }

    private suspend fun resolveTrack(session: PlayerSession, targetId: String?) {
        val roomId = session.currentRoomId ?: return
        val player = session.player ?: return
        val trailMgr = movementTrailManager ?: return

        val trackSkill = skillCatalog.getSkill("TRACK")
        session.skillCooldowns["TRACK"] = trackSkill?.cooldownTicks ?: 4

        // Skill check: willpower + agility/2 + level/2 + d20
        val effStats = session.effectiveStats()
        val roll = (1..GameConfig.Stealth.PERCEPTION_DICE_SIZE).random()
        val check = effStats.willpower + effStats.agility / GameConfig.Stealth.DC_WIL_DIVISOR + player.level / GameConfig.Stealth.DC_LEVEL_DIVISOR + roll

        // Get trails in current room (optionally filtered by target)
        val trails = trailMgr.getTrails(roomId, targetId)

        val trackBaseDifficulty = GameConfig.Trails.TRACK_BASE_DIFFICULTY
        if (trails.isEmpty()) {
            if (check >= trackBaseDifficulty) {
                try { session.send(ServerMessage.TrackResult(success = false, message = "You don't find any tracks here.")) } catch (_: Exception) {}
            } else {
                try { session.send(ServerMessage.TrackResult(success = false, message = "You fail to find any tracks.")) } catch (_: Exception) {}
            }
            checkHiddenExits(session, roomId, check)
            return
        }

        // Use freshest trail
        val freshest = trails.first()
        val penalty = trailMgr.stalenessPenalty(freshest)
        val difficulty = trackBaseDifficulty + penalty

        if (check >= difficulty) {
            val dirName = freshest.direction.name.lowercase()
            try { session.send(ServerMessage.TrackResult(success = true, direction = freshest.direction, targetName = freshest.entityName, message = "You find tracks left by ${freshest.entityName} leading $dirName.")) } catch (_: Exception) {}
        } else {
            try { session.send(ServerMessage.TrackResult(success = false, message = "You find faint tracks but cannot make them out.")) } catch (_: Exception) {}
        }

        checkHiddenExits(session, roomId, check)
    }

    private suspend fun checkHiddenExits(session: PlayerSession, roomId: String, check: Int) {
        val hiddenDefs = worldGraph.getHiddenExitDefs(roomId)
        if (hiddenDefs.isEmpty()) return

        val trackRoll = check + GameConfig.Trails.TRACK_HIDDEN_EXIT_BONUS
        var found = false
        for ((dir, data) in hiddenDefs) {
            if (session.hasDiscoveredExit(roomId, dir)) continue
            if (trackRoll >= data.perceptionDC) {
                session.discoverExit(roomId, dir)
                worldGraph.revealHiddenExit(roomId, dir)
                try { session.send(ServerMessage.SystemMessage("Your tracking skills reveal a hidden passage to the ${dir.name.lowercase()}!")) } catch (_: Exception) {}
                found = true
            }
        }
        if (found) {
            val room = worldGraph.getRoom(roomId)
            if (room != null) {
                val filteredRoom = RoomFilter.forPlayer(room, session, worldGraph)
                try { session.send(ServerMessage.RoomInfo(filteredRoom, emptyList(), emptyList())) } catch (_: Exception) {}
            }
        }
    }

    private suspend fun updateMapForPlayersInRoom(roomId: RoomId) {
        for (s in sessionManager.getSessionsInRoom(roomId)) {
            try {
                val mapRooms = MapRoomFilter.enrichForPlayer(
                    worldGraph.getRoomsNear(roomId), s, worldGraph, sessionManager, npcManager
                )
                s.send(ServerMessage.MapData(mapRooms, roomId))
            } catch (_: Exception) {}
        }
    }

    private suspend fun meditationPhase() {
        for (session in sessionManager.getAllAuthenticatedSessions()) {
            if (!session.isMeditating) continue
            val player = session.player ?: continue

            val effStats = session.effectiveStats()
            val restore = maxOf(effStats.willpower / GameConfig.Meditation.WIL_DIVISOR + GameConfig.Meditation.RESTORE_BASE, 1)
            val newMp = minOf(player.currentMp + restore, player.maxMp)
            val restored = newMp - player.currentMp
            session.player = player.copy(currentMp = newMp)

            try {
                // SpellCastResult updates client MP state and logs the message
                session.send(ServerMessage.SpellCastResult(
                    success = true,
                    spellName = "Meditate",
                    message = "You meditate and restore $restored mana.",
                    newMp = newMp
                ))

                if (newMp >= player.maxMp) {
                    MeditationUtils.breakMeditation(session, "Your mana is fully restored.")
                }

                playerRepository.savePlayerState(session.player!!)
            } catch (_: Exception) { }
        }
    }

    private suspend fun restPhase() {
        for (session in sessionManager.getAllAuthenticatedSessions()) {
            if (!session.isResting) continue
            val player = session.player ?: continue

            val effStats = session.effectiveStats()
            val restore = maxOf(effStats.health / GameConfig.Rest.HEALTH_DIVISOR + GameConfig.Rest.RESTORE_BASE, 1)
            val newHp = minOf(player.currentHp + restore, player.maxHp)
            val restored = newHp - player.currentHp
            session.player = player.copy(currentHp = newHp)

            try {
                session.send(ServerMessage.SpellCastResult(
                    success = true,
                    spellName = "Rest",
                    message = "You rest and restore $restored health.",
                    newMp = player.currentMp,
                    newHp = newHp
                ))

                if (newHp >= player.maxHp) {
                    RestUtils.breakRest(session, "Your health is fully restored.")
                }

                playerRepository.savePlayerState(session.player!!)
            } catch (_: Exception) { }
        }
    }

    /**
     * Periodic scan: non-hidden players in rooms with hidden players roll perception.
     * Observer roll: WIL + INT/2 + level/2 + perceptionBonus + d20
     * vs player stealth DC: AGI + WIL/2 + level/2 + 10
     */
    private suspend fun playerPerceptionPhase() {
        val hiddenByRoom = mutableMapOf<RoomId, MutableList<PlayerSession>>()
        for (session in sessionManager.getAllAuthenticatedSessions()) {
            if (session.isHidden) {
                val roomId = session.currentRoomId ?: continue
                hiddenByRoom.getOrPut(roomId) { mutableListOf() }.add(session)
            }
        }

        for ((roomId, hiddenPlayers) in hiddenByRoom) {
            val observers = sessionManager.getSessionsInRoom(roomId).filter { !it.isHidden }
            for (observer in observers) {
                val observerPlayer = observer.player ?: continue
                val obsStats = observer.effectiveStats()
                val bonus = StealthUtils.perceptionBonus(observerPlayer.characterClass, classCatalog)
                val observerRoll = obsStats.willpower + obsStats.intellect / GameConfig.Stealth.PERCEPTION_INT_DIVISOR + observerPlayer.level / GameConfig.Stealth.PERCEPTION_LEVEL_DIVISOR + bonus + (1..GameConfig.Stealth.PERCEPTION_DICE_SIZE).random()

                for (hiddenSession in hiddenPlayers) {
                    if (!hiddenSession.isHidden) continue
                    val hiddenPlayer = hiddenSession.player ?: continue
                    val hiddenStats = hiddenSession.effectiveStats()
                    val stealthDc = hiddenStats.agility + hiddenStats.willpower / GameConfig.Stealth.DC_WIL_DIVISOR + hiddenPlayer.level / GameConfig.Stealth.DC_LEVEL_DIVISOR + GameConfig.Stealth.DC_BASE

                    if (observerRoll >= stealthDc) {
                        StealthUtils.breakStealth(hiddenSession, sessionManager, "${observerPlayer.name} spots you lurking in the shadows!")
                        try {
                            observer.send(ServerMessage.SystemMessage("You spot ${hiddenPlayer.name} lurking in the shadows!"))
                        } catch (_: Exception) { }
                    }
                }
            }
        }
    }
}

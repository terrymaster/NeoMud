package com.neomud.server.game

import com.neomud.server.game.combat.CombatEvent
import com.neomud.server.game.combat.CombatManager

import com.neomud.server.game.inventory.LootService
import com.neomud.server.game.inventory.RoomItemManager
import com.neomud.server.game.npc.NpcManager
import com.neomud.server.game.progression.XpCalculator
import com.neomud.server.persistence.repository.PlayerRepository
import com.neomud.server.session.SessionManager
import com.neomud.server.world.ClassCatalog
import com.neomud.server.world.LootTableCatalog
import com.neomud.server.world.SkillCatalog
import com.neomud.server.world.WorldGraph
import com.neomud.server.game.npc.NpcState
import com.neomud.server.session.PlayerSession
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
    private val classCatalog: ClassCatalog
) {
    private val logger = LoggerFactory.getLogger(GameLoop::class.java)

    suspend fun run() {
        logger.info("Game loop started (1.5s ticks)")
        while (true) {
            delay(1500)
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
                session.isHidden = false
                session.isMeditating = false
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
        val npcEvents = npcManager.tick()
        for (event in npcEvents) {
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
                        session.send(ServerMessage.PlayerDied(event.killerName, event.respawnRoomId, event.respawnHp, event.respawnMp))
                    } catch (_: Exception) { }

                    // Disable attack mode, stealth, and meditation
                    session.attackMode = false
                    session.selectedTargetId = null
                    session.isHidden = false
                    session.isMeditating = false

                    // Broadcast leave from death room
                    sessionManager.broadcastToRoom(
                        oldRoomId,
                        ServerMessage.PlayerLeft(playerName, oldRoomId, com.neomud.shared.model.Direction.NORTH),
                        exclude = playerName
                    )

                    // XP penalty: lose 10% of current XP on death
                    val player = session.player
                    val xpPenalty = if (player != null) (player.currentXp * 0.10).toLong() else 0L

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
                            val mapRooms = worldGraph.getRoomsNear(event.respawnRoomId).map { mapRoom ->
                                mapRoom.copy(
                                    hasPlayers = sessionManager.getPlayerNamesInRoom(mapRoom.id).isNotEmpty(),
                                    hasNpcs = npcManager.getNpcsInRoom(mapRoom.id).isNotEmpty()
                                )
                            }
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

        // 4. NPC perception scans for hidden players
        npcPerceptionPhase()

        // 4b. Player perception scans for hidden players
        playerPerceptionPhase()

        // 4c. Meditation MP restoration
        meditationPhase()

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

        val npcRoll = npc.perception + npc.level + (1..20).random()
        val effStats = session.effectiveStats()
        val stealthDc = effStats.agility + effStats.willpower / 2 + player.level / 2 + 10

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
                val mapRooms = worldGraph.getRoomsNear(roomId).map { mapRoom ->
                    mapRoom.copy(
                        hasPlayers = sessionManager.getPlayerNamesInRoom(mapRoom.id).isNotEmpty(),
                        hasNpcs = npcManager.getNpcsInRoom(mapRoom.id).isNotEmpty()
                    )
                }
                session.send(ServerMessage.MapData(mapRooms, roomId))
            } catch (_: Exception) { }
        }
    }

    private suspend fun meditationPhase() {
        for (session in sessionManager.getAllAuthenticatedSessions()) {
            if (!session.isMeditating) continue
            val player = session.player ?: continue

            val effStats = session.effectiveStats()
            val restore = maxOf(effStats.willpower / 10 + 2, 1)
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
                val observerRoll = obsStats.willpower + obsStats.intellect / 2 + observerPlayer.level / 2 + bonus + (1..20).random()

                for (hiddenSession in hiddenPlayers) {
                    if (!hiddenSession.isHidden) continue
                    val hiddenPlayer = hiddenSession.player ?: continue
                    val hiddenStats = hiddenSession.effectiveStats()
                    val stealthDc = hiddenStats.agility + hiddenStats.willpower / 2 + hiddenPlayer.level / 2 + 10

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

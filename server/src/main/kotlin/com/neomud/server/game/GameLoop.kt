package com.neomud.server.game

import com.neomud.server.game.combat.CombatEvent
import com.neomud.server.game.combat.CombatManager
import com.neomud.server.game.combat.CombatUtils
import com.neomud.server.game.inventory.LootService
import com.neomud.server.game.inventory.RoomItemManager
import com.neomud.server.game.npc.NpcManager
import com.neomud.server.game.progression.XpCalculator
import com.neomud.server.persistence.repository.PlayerRepository
import com.neomud.server.session.SessionManager
import com.neomud.server.world.LootTableCatalog
import com.neomud.server.world.WorldGraph
import com.neomud.server.game.npc.NpcState
import com.neomud.server.session.PlayerSession
import com.neomud.shared.model.ActiveEffect
import com.neomud.shared.model.EffectType
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
    private val playerRepository: PlayerRepository
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
                        ServerMessage.PlayerEntered(playerName, respawnRoomId),
                        exclude = playerName
                    )
                    val room = worldGraph.getRoom(respawnRoomId)
                    if (room != null) {
                        val playersInRoom = sessionManager.getVisiblePlayerNamesInRoom(respawnRoomId).filter { it != playerName }
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
                                attackerSession.send(ServerMessage.HideModeUpdate(false, "You strike from the shadows!"))
                            } catch (_: Exception) { }
                            // Reveal to other players
                            sessionManager.broadcastToRoom(
                                event.roomId,
                                ServerMessage.PlayerEntered(event.attackerName, event.roomId),
                                exclude = event.attackerName
                            )
                        }
                    }
                    sessionManager.broadcastToRoom(
                        event.roomId,
                        ServerMessage.CombatHit(
                            event.attackerName, event.defenderName, event.damage,
                            event.defenderHp, event.defenderMaxHp, event.isPlayerDefender,
                            event.isBackstab, event.isMiss, event.isDodge,
                            event.defenderId
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

                    // Disable attack mode and stealth
                    session.attackMode = false
                    session.selectedTargetId = null
                    session.isHidden = false

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
                        ServerMessage.PlayerEntered(playerName, event.respawnRoomId),
                        exclude = playerName
                    )

                    // Send room info and map to respawned player
                    val room = worldGraph.getRoom(event.respawnRoomId)
                    if (room != null) {
                        val playersInRoom = sessionManager.getVisiblePlayerNamesInRoom(event.respawnRoomId)
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

        // 4. NPC perception scans for hidden players
        npcPerceptionPhase()

        // 5. Process active effects on all authenticated players
        for (session in sessionManager.getAllAuthenticatedSessions()) {
            val effects = session.activeEffects.toList()
            if (effects.isEmpty()) continue
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

            // Send updated active effects list to client
            try {
                session.send(ServerMessage.ActiveEffectsUpdate(session.activeEffects.toList()))
            } catch (_: Exception) { /* session closing */ }
        }

        // 6. Room healing aura
        for (session in sessionManager.getAllAuthenticatedSessions()) {
            val player = session.player ?: continue
            val roomId = session.currentRoomId ?: continue
            val room = worldGraph.getRoom(roomId) ?: continue
            if (room.healPerTick > 0 && player.currentHp < player.maxHp) {
                val healed = minOf(room.healPerTick, player.maxHp - player.currentHp)
                val newHp = player.currentHp + healed
                session.player = player.copy(currentHp = newHp)
                try {
                    session.send(ServerMessage.EffectTick("Healing Aura", "The temple's aura soothes your wounds. (+$healed HP)", newHp))
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
     * Player passive stealth DC: DEX + INT/2 + level/2 + 10
     */
    private suspend fun npcPerceptionCheck(npc: NpcState, session: PlayerSession, roomId: RoomId) {
        val player = session.player ?: return
        val playerName = session.playerName ?: return

        val npcRoll = npc.perception + npc.level + (1..20).random()
        val effStats = CombatUtils.effectiveStats(player.stats, session.activeEffects.toList())
        val stealthDc = effStats.agility + effStats.intellect / 2 + player.level / 2 + 10

        if (npcRoll >= stealthDc) {
            // Detected!
            session.isHidden = false
            try {
                session.send(ServerMessage.HideModeUpdate(false, "${npc.name} spots you lurking in the shadows!"))
            } catch (_: Exception) { }

            // Reveal to other players in the room
            sessionManager.broadcastToRoom(
                roomId,
                ServerMessage.PlayerEntered(playerName, roomId),
                exclude = playerName
            )
        }
    }
}

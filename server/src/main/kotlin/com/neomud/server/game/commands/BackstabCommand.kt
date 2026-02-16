package com.neomud.server.game.commands

import com.neomud.server.game.combat.CombatUtils
import com.neomud.server.game.inventory.LootService
import com.neomud.server.game.inventory.RoomItemManager
import com.neomud.server.game.npc.NpcManager
import com.neomud.server.game.npc.NpcState
import com.neomud.server.game.progression.XpCalculator
import com.neomud.server.persistence.repository.PlayerRepository
import com.neomud.server.session.PlayerSession
import com.neomud.server.session.SessionManager
import com.neomud.server.world.LootTableCatalog
import com.neomud.shared.model.GroundItem
import com.neomud.shared.protocol.ServerMessage

class BackstabCommand(
    private val npcManager: NpcManager,
    private val sessionManager: SessionManager,
    private val playerRepository: PlayerRepository,
    private val lootService: LootService,
    private val lootTableCatalog: LootTableCatalog,
    private val roomItemManager: RoomItemManager
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
            ServerMessage.PlayerEntered(playerName, roomId),
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
            handleNpcKill(target, playerName, roomId, session)
        }
    }

    private suspend fun handleNpcKill(target: NpcState, killerName: String, roomId: String, session: PlayerSession) {
        if (!npcManager.markDead(target.id)) return
        sessionManager.broadcastToRoom(
            roomId,
            ServerMessage.NpcDied(target.id, target.name, killerName, roomId)
        )

        // Roll loot and drop on ground
        val lootKey = target.templateId.ifEmpty { target.id }
        val lootTable = lootTableCatalog.getLootTable(lootKey)
        val coinDrop = lootTableCatalog.getCoinDrop(lootKey)
        val lootedItems = lootService.rollLoot(lootTable)
        val coins = lootService.rollCoins(coinDrop)

        if (lootedItems.isNotEmpty() || !coins.isEmpty()) {
            if (lootedItems.isNotEmpty()) {
                roomItemManager.addItems(
                    roomId,
                    lootedItems.map { GroundItem(it.itemId, it.quantity) }
                )
            }
            roomItemManager.addCoins(roomId, coins)

            sessionManager.broadcastToRoom(
                roomId,
                ServerMessage.LootDropped(target.name, lootedItems, coins)
            )

            val groundItems = roomItemManager.getGroundItems(roomId)
            val groundCoins = roomItemManager.getGroundCoins(roomId)
            sessionManager.broadcastToRoom(
                roomId,
                ServerMessage.RoomItemsUpdate(groundItems, groundCoins)
            )
        }

        // Award XP
        if (target.xpReward > 0) {
            val player = session.player ?: return
            val xpGained = XpCalculator.xpForKill(target.level, player.level, target.xpReward)
            val newXp = player.currentXp + xpGained
            session.player = player.copy(currentXp = newXp)
            try {
                session.send(ServerMessage.XpGained(xpGained, newXp, player.xpToNextLevel))
                if (XpCalculator.isReadyToLevel(newXp, player.xpToNextLevel, player.level)) {
                    session.send(ServerMessage.SystemMessage("You have enough experience to level up! Visit a trainer."))
                }
                playerRepository.savePlayerState(session.player!!)
            } catch (_: Exception) { }
        }
    }
}

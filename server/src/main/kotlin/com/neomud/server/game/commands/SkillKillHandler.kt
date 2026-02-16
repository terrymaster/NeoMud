package com.neomud.server.game.commands

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

class SkillKillHandler(
    private val npcManager: NpcManager,
    private val sessionManager: SessionManager,
    private val playerRepository: PlayerRepository,
    private val lootService: LootService,
    private val lootTableCatalog: LootTableCatalog,
    private val roomItemManager: RoomItemManager
) {
    suspend fun handleNpcKill(target: NpcState, killerName: String, roomId: String, session: PlayerSession) {
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

        // Auto-disable attack mode if no hostiles remain
        for (s in sessionManager.getSessionsInRoom(roomId)) {
            if (s.attackMode) {
                val remaining = npcManager.getLivingHostileNpcsInRoom(roomId)
                if (remaining.isEmpty()) {
                    s.attackMode = false
                    s.selectedTargetId = null
                    try { s.send(ServerMessage.AttackModeUpdate(false)) } catch (_: Exception) { }
                } else if (s.selectedTargetId == target.id) {
                    s.selectedTargetId = null
                }
            }
        }
    }
}

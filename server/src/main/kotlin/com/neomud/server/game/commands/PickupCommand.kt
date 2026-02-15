package com.neomud.server.game.commands

import com.neomud.server.game.inventory.RoomItemManager
import com.neomud.server.persistence.repository.CoinRepository
import com.neomud.server.persistence.repository.InventoryRepository
import com.neomud.server.session.PlayerSession
import com.neomud.server.session.SessionManager
import com.neomud.server.world.ItemCatalog
import com.neomud.shared.model.Coins
import com.neomud.shared.protocol.ServerMessage

class PickupCommand(
    private val roomItemManager: RoomItemManager,
    private val inventoryRepository: InventoryRepository,
    private val coinRepository: CoinRepository,
    private val itemCatalog: ItemCatalog,
    private val sessionManager: SessionManager
) {
    suspend fun handlePickupItem(session: PlayerSession, itemId: String, quantity: Int) {
        if (quantity < 1) return
        val playerName = session.playerName ?: return
        val roomId = session.currentRoomId ?: return

        val removed = roomItemManager.removeItem(roomId, itemId, quantity)
        if (removed == 0) {
            session.send(ServerMessage.Error("Nothing to pick up."))
            return
        }

        inventoryRepository.addItem(playerName, itemId, removed)

        val itemName = itemCatalog.getItem(itemId)?.name ?: itemId
        session.send(ServerMessage.PickupResult(itemName, removed))

        // Send updated inventory to the picker
        sendInventoryUpdate(session, playerName)

        // Broadcast updated ground state to entire room
        broadcastRoomItems(roomId)
    }

    suspend fun handlePickupCoins(session: PlayerSession, coinType: String) {
        val playerName = session.playerName ?: return
        val roomId = session.currentRoomId ?: return

        val amount = roomItemManager.removeCoins(roomId, coinType)
        if (amount == 0) {
            session.send(ServerMessage.Error("No $coinType to pick up."))
            return
        }

        val coins = when (coinType) {
            "copper" -> Coins(copper = amount)
            "silver" -> Coins(silver = amount)
            "gold" -> Coins(gold = amount)
            "platinum" -> Coins(platinum = amount)
            else -> return
        }
        coinRepository.addCoins(playerName, coins)

        session.send(ServerMessage.PickupResult(coinType, amount, isCoin = true))

        // Send updated inventory to the picker
        sendInventoryUpdate(session, playerName)

        // Broadcast updated ground state to entire room
        broadcastRoomItems(roomId)
    }

    private suspend fun sendInventoryUpdate(session: PlayerSession, playerName: String) {
        val inventory = inventoryRepository.getInventory(playerName)
        val equipment = inventoryRepository.getEquippedItems(playerName)
        val coins = coinRepository.getCoins(playerName)
        session.send(ServerMessage.InventoryUpdate(inventory, equipment, coins))
    }

    private suspend fun broadcastRoomItems(roomId: String) {
        val items = roomItemManager.getGroundItems(roomId)
        val coins = roomItemManager.getGroundCoins(roomId)
        sessionManager.broadcastToRoom(roomId, ServerMessage.RoomItemsUpdate(items, coins))
    }
}

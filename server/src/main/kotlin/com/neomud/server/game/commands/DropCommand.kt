package com.neomud.server.game.commands

import com.neomud.server.game.MeditationUtils
import com.neomud.server.game.RestUtils
import com.neomud.server.game.StealthUtils
import com.neomud.server.game.inventory.RoomItemManager
import com.neomud.server.persistence.repository.CoinRepository
import com.neomud.server.persistence.repository.InventoryRepository
import com.neomud.server.session.PlayerSession
import com.neomud.server.session.SessionManager
import com.neomud.server.world.ItemCatalog
import com.neomud.shared.model.GroundItem
import com.neomud.shared.protocol.ServerMessage

class DropCommand(
    private val roomItemManager: RoomItemManager,
    private val inventoryRepository: InventoryRepository,
    private val coinRepository: CoinRepository,
    private val itemCatalog: ItemCatalog,
    private val sessionManager: SessionManager
) {
    suspend fun execute(session: PlayerSession, itemId: String, quantity: Int) {
        if (quantity < 1) return
        val playerName = session.playerName ?: return
        val roomId = session.currentRoomId ?: return

        MeditationUtils.breakMeditation(session, "You stop meditating.")
        RestUtils.breakRest(session, "You stop resting.")
        StealthUtils.breakStealth(session, sessionManager, "Dropping items reveals your presence!")

        // Check if the item is equipped — must unequip first
        val equipped = inventoryRepository.getEquippedItems(playerName)
        if (itemId in equipped.values) {
            session.send(ServerMessage.SystemMessage("You must unequip that item first."))
            return
        }

        // Check available quantity in inventory (non-equipped only)
        val inventory = inventoryRepository.getInventory(playerName)
        val available = inventory.filter { !it.equipped && it.itemId == itemId }
            .sumOf { it.quantity }
        if (available == 0) {
            session.send(ServerMessage.Error("You don't have that item."))
            return
        }

        val toDrop = minOf(quantity, available)

        // Remove from inventory and add to ground
        inventoryRepository.removeItem(playerName, itemId, toDrop)
        roomItemManager.addItems(roomId, listOf(GroundItem(itemId, toDrop)))

        val itemName = itemCatalog.getItem(itemId)?.name ?: itemId
        val qtyText = if (toDrop > 1) "$toDrop x $itemName" else itemName
        session.send(ServerMessage.SystemMessage("You drop $qtyText."))

        // Send updated inventory to the dropper
        val updatedInventory = inventoryRepository.getInventory(playerName)
        val updatedEquipment = inventoryRepository.getEquippedItems(playerName)
        val coins = coinRepository.getCoins(playerName)
        session.send(ServerMessage.InventoryUpdate(updatedInventory, updatedEquipment, coins))

        // Broadcast updated ground state to entire room
        val groundItems = roomItemManager.getGroundItems(roomId)
        val groundCoins = roomItemManager.getGroundCoins(roomId)
        sessionManager.broadcastToRoom(roomId, ServerMessage.RoomItemsUpdate(groundItems, groundCoins))
    }
}

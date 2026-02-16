package com.neomud.server.game.commands

import com.neomud.server.game.npc.NpcManager
import com.neomud.server.persistence.repository.CoinRepository
import com.neomud.server.persistence.repository.InventoryRepository
import com.neomud.server.session.PlayerSession
import com.neomud.server.world.ItemCatalog
import com.neomud.shared.model.Coins
import com.neomud.shared.model.VendorItem
import com.neomud.shared.protocol.ServerMessage
import org.slf4j.LoggerFactory

class VendorCommand(
    private val npcManager: NpcManager,
    private val itemCatalog: ItemCatalog,
    private val inventoryRepository: InventoryRepository,
    private val coinRepository: CoinRepository,
    private val inventoryCommand: InventoryCommand
) {
    private val logger = LoggerFactory.getLogger(VendorCommand::class.java)

    suspend fun handleInteract(session: PlayerSession) {
        val roomId = session.currentRoomId ?: return
        val playerName = session.playerName ?: return
        val player = session.player ?: return

        val vendor = npcManager.getVendorInRoom(roomId)
        if (vendor == null) {
            session.send(ServerMessage.SystemMessage("There is no vendor here."))
            return
        }

        val vendorItems = vendor.vendorItems.mapNotNull { itemId ->
            val item = itemCatalog.getItem(itemId) ?: return@mapNotNull null
            VendorItem(item = item, price = Coins.fromCopper(item.value.toLong()))
        }

        val playerCoins = coinRepository.getCoins(playerName)
        val playerInventory = inventoryRepository.getInventory(playerName)

        session.send(ServerMessage.VendorInfo(
            vendorName = vendor.name,
            items = vendorItems,
            playerCoins = playerCoins,
            playerInventory = playerInventory,
            playerCharm = player.stats.charm,
            interactSound = vendor.interactSound
        ))
    }

    suspend fun handleBuy(session: PlayerSession, itemId: String, quantity: Int) {
        if (quantity < 1) return
        val roomId = session.currentRoomId ?: return
        val playerName = session.playerName ?: return
        val player = session.player ?: return

        val vendor = npcManager.getVendorInRoom(roomId)
        if (vendor == null) {
            session.send(ServerMessage.SystemMessage("There is no vendor here."))
            return
        }

        if (itemId !in vendor.vendorItems) {
            session.send(ServerMessage.Error("This vendor doesn't sell that item."))
            return
        }

        val item = itemCatalog.getItem(itemId)
        if (item == null) {
            session.send(ServerMessage.Error("Unknown item."))
            return
        }

        if (item.levelRequirement > player.level) {
            session.send(ServerMessage.Error("You need to be level ${item.levelRequirement} to buy ${item.name}."))
            return
        }

        val totalCost = Coins.fromCopper(item.value.toLong() * quantity)
        val success = coinRepository.subtractCoins(playerName, totalCost)
        if (!success) {
            session.send(ServerMessage.Error("You can't afford ${item.name}. It costs ${totalCost.displayString()}."))
            return
        }

        inventoryRepository.addItem(playerName, itemId, quantity)
        logger.info("$playerName bought ${item.name} x$quantity for ${totalCost.displayString()}")

        val updatedCoins = coinRepository.getCoins(playerName)
        val updatedInventory = inventoryRepository.getInventory(playerName)
        val equipment = inventoryRepository.getEquippedItems(playerName)

        session.send(ServerMessage.BuyResult(
            success = true,
            message = "You bought ${item.name} for ${totalCost.displayString()}.",
            updatedCoins = updatedCoins,
            updatedInventory = updatedInventory,
            equipment = equipment
        ))

        // Also send inventory update to keep client state in sync
        inventoryCommand.sendInventoryUpdate(session)
    }

    suspend fun handleSell(session: PlayerSession, itemId: String, quantity: Int) {
        if (quantity < 1) return
        val roomId = session.currentRoomId ?: return
        val playerName = session.playerName ?: return
        val player = session.player ?: return

        val vendor = npcManager.getVendorInRoom(roomId)
        if (vendor == null) {
            session.send(ServerMessage.SystemMessage("There is no vendor here."))
            return
        }

        val item = itemCatalog.getItem(itemId)
        if (item == null) {
            session.send(ServerMessage.Error("Unknown item."))
            return
        }

        if (item.value <= 0) {
            session.send(ServerMessage.Error("${item.name} cannot be sold."))
            return
        }

        val removed = inventoryRepository.removeItem(playerName, itemId, quantity)
        if (!removed) {
            session.send(ServerMessage.Error("You don't have that item."))
            return
        }

        val sellPriceCopper = Coins.sellPriceCopper(item.value, quantity, player.stats.charm)
        val sellPrice = Coins.fromCopper(sellPriceCopper)
        coinRepository.addCoins(playerName, sellPrice)
        logger.info("$playerName sold ${item.name} x$quantity for ${sellPrice.displayString()}")

        val updatedCoins = coinRepository.getCoins(playerName)
        val updatedInventory = inventoryRepository.getInventory(playerName)
        val equipment = inventoryRepository.getEquippedItems(playerName)

        session.send(ServerMessage.SellResult(
            success = true,
            message = "You sold ${item.name} for ${sellPrice.displayString()}.",
            updatedCoins = updatedCoins,
            updatedInventory = updatedInventory,
            equipment = equipment
        ))

        inventoryCommand.sendInventoryUpdate(session)
    }
}

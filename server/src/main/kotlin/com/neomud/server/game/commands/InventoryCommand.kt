package com.neomud.server.game.commands

import com.neomud.server.persistence.repository.InventoryRepository
import com.neomud.server.session.PlayerSession
import com.neomud.server.world.ItemCatalog
import com.neomud.shared.protocol.ServerMessage

class InventoryCommand(
    private val inventoryRepository: InventoryRepository,
    private val itemCatalog: ItemCatalog
) {
    suspend fun handleViewInventory(session: PlayerSession) {
        val playerName = session.playerName ?: return
        val inventory = inventoryRepository.getInventory(playerName)
        val equipment = inventoryRepository.getEquippedItems(playerName)
        session.send(ServerMessage.InventoryUpdate(inventory, equipment))
    }

    suspend fun handleEquipItem(session: PlayerSession, itemId: String, slot: String) {
        val playerName = session.playerName ?: return
        val player = session.player ?: return

        val item = itemCatalog.getItem(itemId)
        if (item == null) {
            session.send(ServerMessage.Error("Unknown item."))
            return
        }

        if (item.slot.isEmpty() || (item.slot != slot)) {
            session.send(ServerMessage.Error("${item.name} cannot be equipped in that slot."))
            return
        }

        if (item.levelRequirement > player.level) {
            session.send(ServerMessage.Error("You need to be level ${item.levelRequirement} to equip ${item.name}."))
            return
        }

        val success = inventoryRepository.equipItem(playerName, itemId, slot)
        if (success) {
            session.send(ServerMessage.EquipUpdate(slot, itemId, item.name))
            sendInventoryUpdate(session)
        } else {
            session.send(ServerMessage.Error("Could not equip ${item.name}."))
        }
    }

    suspend fun handleUnequipItem(session: PlayerSession, slot: String) {
        val playerName = session.playerName ?: return

        val success = inventoryRepository.unequipItem(playerName, slot)
        if (success) {
            session.send(ServerMessage.EquipUpdate(slot, null, null))
            sendInventoryUpdate(session)
        } else {
            session.send(ServerMessage.Error("Nothing equipped in that slot."))
        }
    }

    suspend fun handleUseItem(session: PlayerSession, itemId: String) {
        val playerName = session.playerName ?: return
        val player = session.player ?: return

        val item = itemCatalog.getItem(itemId)
        if (item == null) {
            session.send(ServerMessage.Error("Unknown item."))
            return
        }

        if (item.type != "consumable") {
            session.send(ServerMessage.Error("${item.name} cannot be used."))
            return
        }

        // Parse effect
        val effect = item.useEffect
        if (effect.startsWith("heal:")) {
            val amount = effect.removePrefix("heal:").toIntOrNull() ?: 0
            val newHp = (player.currentHp + amount).coerceAtMost(player.maxHp)
            session.player = player.copy(currentHp = newHp)

            inventoryRepository.removeItem(playerName, itemId, 1)
            session.send(ServerMessage.ItemUsed(item.name, "You drink the ${item.name} and recover $amount HP.", newHp))
            sendInventoryUpdate(session)
        } else {
            session.send(ServerMessage.Error("Unknown effect for ${item.name}."))
        }
    }

    suspend fun sendInventoryUpdate(session: PlayerSession) {
        val playerName = session.playerName ?: return
        val inventory = inventoryRepository.getInventory(playerName)
        val equipment = inventoryRepository.getEquippedItems(playerName)
        session.send(ServerMessage.InventoryUpdate(inventory, equipment))
    }
}

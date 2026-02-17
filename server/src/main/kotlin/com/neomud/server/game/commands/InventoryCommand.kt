package com.neomud.server.game.commands

import com.neomud.server.game.UseEffectProcessor
import com.neomud.server.persistence.repository.CoinRepository
import com.neomud.server.persistence.repository.InventoryRepository
import com.neomud.server.session.PlayerSession
import com.neomud.server.world.ItemCatalog
import com.neomud.server.world.WorldGraph
import com.neomud.shared.protocol.ServerMessage

class InventoryCommand(
    private val inventoryRepository: InventoryRepository,
    private val itemCatalog: ItemCatalog,
    private val coinRepository: CoinRepository,
    private val worldGraph: WorldGraph
) {
    companion object {
        private val VALID_SLOTS = setOf("weapon", "shield", "head", "chest", "legs", "feet", "hands")
    }

    suspend fun handleViewInventory(session: PlayerSession) {
        val playerName = session.playerName ?: return
        val inventory = inventoryRepository.getInventory(playerName)
        val equipment = inventoryRepository.getEquippedItems(playerName)
        val coins = coinRepository.getCoins(playerName)
        session.send(ServerMessage.InventoryUpdate(inventory, equipment, coins))
    }

    suspend fun handleEquipItem(session: PlayerSession, itemId: String, slot: String) {
        val playerName = session.playerName ?: return
        val player = session.player ?: return

        if (slot !in VALID_SLOTS) {
            session.send(ServerMessage.Error("Invalid equipment slot."))
            return
        }

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

        if (isHostileEffect(item.useEffect)) {
            val roomId = session.currentRoomId
            if (roomId != null) {
                val room = worldGraph.getRoom(roomId)
                if (room != null && room.effects.any { it.type == "SANCTUARY" }) {
                    session.send(ServerMessage.Error("The sanctuary's protection prevents you from using ${item.name} here."))
                    return
                }
            }
        }

        val result = UseEffectProcessor.process(item.useEffect, player, item.name)
        if (result == null) {
            session.send(ServerMessage.Error("${item.name} has no usable effect."))
            return
        }

        val removed = inventoryRepository.removeItem(playerName, itemId, 1)
        if (!removed) {
            session.send(ServerMessage.Error("You don't have that item."))
            return
        }

        session.player = result.updatedPlayer
        for (effect in result.newEffects) {
            session.activeEffects.add(effect)
        }

        val message = result.messages.joinToString(" ")
        session.send(ServerMessage.ItemUsed(
            itemName = item.name,
            message = message,
            newHp = result.updatedPlayer.currentHp,
            newMp = result.updatedPlayer.currentMp
        ))
        if (result.newEffects.isNotEmpty()) {
            session.send(ServerMessage.ActiveEffectsUpdate(session.activeEffects.toList()))
        }
        sendInventoryUpdate(session)
    }

    suspend fun sendInventoryUpdate(session: PlayerSession) {
        val playerName = session.playerName ?: return
        val inventory = inventoryRepository.getInventory(playerName)
        val equipment = inventoryRepository.getEquippedItems(playerName)
        val coins = coinRepository.getCoins(playerName)
        session.send(ServerMessage.InventoryUpdate(inventory, equipment, coins))
    }

    private fun isHostileEffect(effectString: String): Boolean {
        return effectString.split(',').any { token ->
            token.trim().split(':').firstOrNull() in setOf("damage", "poison")
        }
    }
}

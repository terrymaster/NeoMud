package com.neomud.server.game.inventory

import com.neomud.server.persistence.repository.InventoryRepository
import com.neomud.server.world.ItemCatalog
import com.neomud.shared.model.LootEntry
import com.neomud.shared.model.LootedItem
import org.slf4j.LoggerFactory

class LootService(
    private val inventoryRepository: InventoryRepository,
    private val itemCatalog: ItemCatalog
) {
    private val logger = LoggerFactory.getLogger(LootService::class.java)

    fun rollLoot(lootTable: List<LootEntry>): List<LootedItem> {
        val results = mutableListOf<LootedItem>()
        for (entry in lootTable) {
            if (Math.random() <= entry.chance) {
                val item = itemCatalog.getItem(entry.itemId)
                if (item == null) {
                    logger.warn("Loot table references unknown item: ${entry.itemId}")
                    continue
                }
                val quantity = if (entry.minQuantity == entry.maxQuantity) {
                    entry.minQuantity
                } else {
                    (entry.minQuantity..entry.maxQuantity).random()
                }
                results.add(LootedItem(entry.itemId, item.name, quantity))
            }
        }
        return results
    }

    fun awardLoot(playerName: String, items: List<LootedItem>) {
        for (lootedItem in items) {
            inventoryRepository.addItem(playerName, lootedItem.itemId, lootedItem.quantity)
        }
    }
}

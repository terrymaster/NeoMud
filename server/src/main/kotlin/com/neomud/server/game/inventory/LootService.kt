package com.neomud.server.game.inventory

import com.neomud.server.world.ItemCatalog
import com.neomud.shared.model.CoinDrop
import com.neomud.shared.model.Coins
import com.neomud.shared.model.LootEntry
import com.neomud.shared.model.LootedItem
import org.slf4j.LoggerFactory

class LootService(
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

    fun rollCoins(coinDrop: CoinDrop?): Coins {
        if (coinDrop == null) return Coins()
        return Coins(
            copper = rollRange(coinDrop.minCopper, coinDrop.maxCopper),
            silver = rollRange(coinDrop.minSilver, coinDrop.maxSilver),
            gold = rollRange(coinDrop.minGold, coinDrop.maxGold),
            platinum = rollRange(coinDrop.minPlatinum, coinDrop.maxPlatinum)
        )
    }

    private fun rollRange(min: Int, max: Int): Int {
        if (max <= 0) return 0
        if (min == max) return min
        return (min..max).random()
    }
}

package com.neomud.server.world

import com.neomud.shared.model.CoinDrop
import com.neomud.shared.model.LootEntry
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

@Serializable
data class LootTableEntry(
    val items: List<LootEntry> = emptyList(),
    val coinDrop: CoinDrop? = null
)

class LootTableCatalog(private val tables: Map<String, LootTableEntry>) {

    val tableCount: Int get() = tables.size

    fun getLootTable(npcId: String): List<LootEntry> = tables[npcId]?.items ?: emptyList()

    fun getCoinDrop(npcId: String): CoinDrop? = tables[npcId]?.coinDrop

    fun getAllEntries(): Map<String, LootTableEntry> = tables

    companion object {
        private val logger = LoggerFactory.getLogger(LootTableCatalog::class.java)
        private val json = Json { ignoreUnknownKeys = true }

        fun load(source: WorldDataSource): LootTableCatalog {
            val tables = mutableMapOf<String, LootTableEntry>()
            val zoneFiles = source.list("world/", ".zone.json")

            for (file in zoneFiles) {
                val content = source.readText(file) ?: continue
                val zone = json.decodeFromString<ZoneData>(content)
                for (npc in zone.npcs) {
                    if (npc.lootItems.isNotEmpty() || npc.coinDrop != null) {
                        tables[npc.id] = LootTableEntry(
                            items = npc.lootItems,
                            coinDrop = npc.coinDrop
                        )
                    }
                }
            }

            logger.info("Loaded ${tables.size} loot tables from NPC data")
            return LootTableCatalog(tables)
        }
    }
}

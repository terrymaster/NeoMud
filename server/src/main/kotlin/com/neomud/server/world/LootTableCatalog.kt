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

@Serializable
data class LootTableData(val tables: Map<String, LootTableEntry>)

class LootTableCatalog(private val tables: Map<String, LootTableEntry>) {

    val tableCount: Int get() = tables.size

    fun getLootTable(npcId: String): List<LootEntry> = tables[npcId]?.items ?: emptyList()

    fun getCoinDrop(npcId: String): CoinDrop? = tables[npcId]?.coinDrop

    fun getAllEntries(): Map<String, LootTableEntry> = tables

    companion object {
        private val logger = LoggerFactory.getLogger(LootTableCatalog::class.java)
        private val json = Json { ignoreUnknownKeys = true }

        fun load(source: WorldDataSource): LootTableCatalog {
            val content = source.readText("world/loot_tables.json")
                ?: error("loot_tables.json not found")
            val data = json.decodeFromString<LootTableData>(content)
            logger.info("Loaded ${data.tables.size} loot tables")
            return LootTableCatalog(data.tables)
        }

        fun load(): LootTableCatalog = load(ClasspathDataSource())
    }
}

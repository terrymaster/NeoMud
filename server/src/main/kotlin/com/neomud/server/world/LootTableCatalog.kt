package com.neomud.server.world

import com.neomud.shared.model.LootEntry
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

@Serializable
data class LootTableData(val tables: Map<String, List<LootEntry>>)

class LootTableCatalog(private val tables: Map<String, List<LootEntry>>) {

    val tableCount: Int get() = tables.size

    fun getLootTable(npcId: String): List<LootEntry> = tables[npcId] ?: emptyList()

    companion object {
        private val logger = LoggerFactory.getLogger(LootTableCatalog::class.java)
        private val json = Json { ignoreUnknownKeys = true }

        fun load(): LootTableCatalog {
            val resource = LootTableCatalog::class.java.classLoader.getResourceAsStream("world/loot_tables.json")
                ?: error("loot_tables.json not found in resources")
            val content = resource.bufferedReader().use { it.readText() }
            val data = json.decodeFromString<LootTableData>(content)
            logger.info("Loaded ${data.tables.size} loot tables")
            return LootTableCatalog(data.tables)
        }
    }
}

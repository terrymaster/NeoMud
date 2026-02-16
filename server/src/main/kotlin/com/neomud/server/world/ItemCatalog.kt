package com.neomud.server.world

import com.neomud.shared.model.Item
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

@Serializable
data class ItemCatalogData(val items: List<Item>)

class ItemCatalog(items: List<Item>) {
    private val itemMap: Map<String, Item> = items.associateBy { it.id }

    val itemCount: Int get() = itemMap.size

    fun getItem(id: String): Item? = itemMap[id]

    fun getAllItems(): List<Item> = itemMap.values.toList()

    companion object {
        private val logger = LoggerFactory.getLogger(ItemCatalog::class.java)
        private val json = Json { ignoreUnknownKeys = true }

        fun load(source: WorldDataSource): ItemCatalog {
            val content = source.readText("world/items.json")
                ?: error("items.json not found")
            val data = json.decodeFromString<ItemCatalogData>(content)
            logger.info("Loaded ${data.items.size} items")
            return ItemCatalog(data.items)
        }
    }
}

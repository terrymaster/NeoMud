package com.neomud.server.world

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LootTableCatalogTest {

    @Test
    fun testLoadLootTablesFromJson() {
        val catalog = LootTableCatalog.load()
        assertTrue(catalog.tableCount >= 2, "Should load at least 2 loot tables")
    }

    @Test
    fun testShadowWolfLootTable() {
        val catalog = LootTableCatalog.load()
        val loot = catalog.getLootTable("npc:shadow_wolf")
        assertTrue(loot.isNotEmpty(), "Shadow wolf should have loot entries")
        val wolfPelt = loot.find { it.itemId == "item:wolf_pelt" }
        assertTrue(wolfPelt != null, "Shadow wolf should drop wolf pelt")
        assertEquals(0.8, wolfPelt.chance)
    }

    @Test
    fun testForestSpiderLootTable() {
        val catalog = LootTableCatalog.load()
        val loot = catalog.getLootTable("npc:forest_spider")
        assertTrue(loot.isNotEmpty(), "Forest spider should have loot entries")
        val fang = loot.find { it.itemId == "item:spider_fang" }
        assertTrue(fang != null, "Forest spider should drop spider fang")
        assertEquals(0.9, fang.chance)
        assertEquals(1, fang.minQuantity)
        assertEquals(2, fang.maxQuantity)
    }

    @Test
    fun testUnknownNpcReturnsEmptyList() {
        val catalog = LootTableCatalog.load()
        val loot = catalog.getLootTable("npc:nonexistent")
        assertTrue(loot.isEmpty())
    }
}

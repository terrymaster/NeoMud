package com.neomud.server.world

import com.neomud.server.defaultWorldSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LootTableCatalogTest {

    private fun load() = LootTableCatalog.load(defaultWorldSource())

    @Test
    fun testLoadLootTablesFromJson() {
        val catalog = load()
        assertTrue(catalog.tableCount >= 2, "Should load at least 2 loot tables")
    }

    @Test
    fun testShadowWolfLootTable() {
        val catalog = load()
        val loot = catalog.getLootTable("npc:shadow_wolf")
        assertTrue(loot.isNotEmpty(), "Shadow wolf should have loot entries")
        val wolfPelt = loot.find { it.itemId == "item:wolf_pelt" }
        assertTrue(wolfPelt != null, "Shadow wolf should drop wolf pelt")
        assertEquals(0.8, wolfPelt.chance)
    }

    @Test
    fun testForestSpiderLootTable() {
        val catalog = load()
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
        val catalog = load()
        val loot = catalog.getLootTable("npc:nonexistent")
        assertTrue(loot.isEmpty())
    }

    @Test
    fun testShadowWolfCoinDrop() {
        val catalog = load()
        val coinDrop = catalog.getCoinDrop("npc:shadow_wolf")
        assertNotNull(coinDrop)
        assertEquals(5, coinDrop.minCopper)
        assertEquals(20, coinDrop.maxCopper)
        assertEquals(0, coinDrop.minSilver)
        assertEquals(1, coinDrop.maxSilver)
    }

    @Test
    fun testForestSpiderCoinDrop() {
        val catalog = load()
        val coinDrop = catalog.getCoinDrop("npc:forest_spider")
        assertNotNull(coinDrop)
        assertEquals(10, coinDrop.minCopper)
        assertEquals(35, coinDrop.maxCopper)
        assertEquals(0, coinDrop.minSilver)
        assertEquals(1, coinDrop.maxSilver)
    }

    @Test
    fun testUnknownNpcCoinDropReturnsNull() {
        val catalog = load()
        assertNull(catalog.getCoinDrop("npc:nonexistent"))
    }
}

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
        assertTrue(catalog.tableCount >= 12, "Should load at least 12 loot tables (4 forest + 4 marsh + 4 gorge)")
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
    fun testMarshLootTables() {
        val catalog = load()

        val lurkerLoot = catalog.getLootTable("npc:bog_lurker")
        assertTrue(lurkerLoot.isNotEmpty(), "Bog Lurker should have loot entries")
        val hide = lurkerLoot.find { it.itemId == "item:marsh_hide" }
        assertNotNull(hide, "Bog Lurker should drop marsh hide")
        assertEquals(0.7, hide.chance)

        val hagLoot = catalog.getLootTable("npc:mire_hag")
        assertTrue(hagLoot.isNotEmpty(), "Mire Hag should have loot entries")
        val amulet = hagLoot.find { it.itemId == "item:amulet_of_warding" }
        assertNotNull(amulet, "Mire Hag should drop amulet of warding")
        assertEquals(0.12, amulet.chance)
    }

    @Test
    fun testGorgeLootTables() {
        val catalog = load()

        val stalkerLoot = catalog.getLootTable("npc:gorge_stalker")
        assertTrue(stalkerLoot.isNotEmpty(), "Gorge Stalker should have loot entries")
        val shard = stalkerLoot.find { it.itemId == "item:obsidian_shard" }
        assertNotNull(shard, "Gorge Stalker should drop obsidian shard")
        assertEquals(0.6, shard.chance)

        val wardenLoot = catalog.getLootTable("npc:gorge_warden")
        assertTrue(wardenLoot.isNotEmpty(), "Gorge Warden should have loot entries")
        val greatsword = wardenLoot.find { it.itemId == "item:steel_greatsword" }
        assertNotNull(greatsword, "Gorge Warden should drop steel greatsword")
        assertEquals(0.06, greatsword.chance)
    }

    @Test
    fun testGorgeWardenCoinDrop() {
        val catalog = load()
        val coinDrop = catalog.getCoinDrop("npc:gorge_warden")
        assertNotNull(coinDrop)
        assertEquals(50, coinDrop.minCopper)
        assertEquals(99, coinDrop.maxCopper)
        assertEquals(3, coinDrop.minSilver)
        assertEquals(8, coinDrop.maxSilver)
        assertEquals(0, coinDrop.minGold)
        assertEquals(1, coinDrop.maxGold)
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

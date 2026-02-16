package com.neomud.server.world

import com.neomud.server.defaultWorldSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ItemCatalogTest {

    private fun load() = ItemCatalog.load(defaultWorldSource())

    @Test
    fun testLoadItemsFromJson() {
        val catalog = load()
        assertTrue(catalog.itemCount >= 10, "Should load at least 10 items")
    }

    @Test
    fun testLookupWeapon() {
        val catalog = load()
        val sword = catalog.getItem("item:iron_sword")
        assertNotNull(sword)
        assertEquals("Iron Sword", sword.name)
        assertEquals("weapon", sword.type)
        assertEquals("weapon", sword.slot)
        assertEquals(3, sword.damageBonus)
        assertEquals(6, sword.damageRange)
    }

    @Test
    fun testLookupArmor() {
        val catalog = load()
        val chest = catalog.getItem("item:leather_chest")
        assertNotNull(chest)
        assertEquals("Leather Vest", chest.name)
        assertEquals("armor", chest.type)
        assertEquals("chest", chest.slot)
        assertEquals(3, chest.armorValue)
    }

    @Test
    fun testLookupConsumable() {
        val catalog = load()
        val potion = catalog.getItem("item:health_potion")
        assertNotNull(potion)
        assertEquals("Health Potion", potion.name)
        assertEquals("consumable", potion.type)
        assertTrue(potion.stackable)
        assertEquals("heal:25", potion.useEffect)
    }

    @Test
    fun testUnknownItemReturnsNull() {
        val catalog = load()
        assertNull(catalog.getItem("item:nonexistent"))
    }

    @Test
    fun testGetAllItems() {
        val catalog = load()
        val all = catalog.getAllItems()
        assertTrue(all.size >= 10)
        val ids = all.map { it.id }.toSet()
        assertTrue("item:iron_sword" in ids)
        assertTrue("item:health_potion" in ids)
        assertTrue("item:wolf_pelt" in ids)
    }
}

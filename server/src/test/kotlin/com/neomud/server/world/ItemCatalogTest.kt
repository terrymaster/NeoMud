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
        assertTrue(catalog.itemCount >= 38, "Should load at least 38 items")
    }

    @Test
    fun testLookupWeapon() {
        val catalog = load()
        val sword = catalog.getItem("item:iron_sword")
        assertNotNull(sword)
        assertEquals("Iron Sword", sword.name)
        assertEquals("weapon", sword.type)
        assertEquals("weapon", sword.slot)
        assertEquals(8, sword.damageBonus)
        assertEquals(8, sword.damageRange)
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
    fun testLookupAccessory() {
        val catalog = load()
        val amulet = catalog.getItem("item:amulet_of_warding")
        assertNotNull(amulet)
        assertEquals("Amulet of Warding", amulet.name)
        assertEquals("armor", amulet.type)
        assertEquals("neck", amulet.slot)
        assertEquals(3, amulet.armorValue)
        assertEquals(3, amulet.levelRequirement)

        val ring = catalog.getItem("item:ring_of_intellect")
        assertNotNull(ring)
        assertEquals("Ring of Intellect", ring.name)
        assertEquals("ring", ring.slot)
        assertEquals(2, ring.damageBonus)
    }

    @Test
    fun testLookupMagicWeapon() {
        val catalog = load()
        val staff = catalog.getItem("item:mystic_staff")
        assertNotNull(staff)
        assertEquals("Mystic Staff", staff.name)
        assertEquals("weapon", staff.type)
        assertEquals(8, staff.damageBonus)
        assertEquals(8, staff.damageRange)
        assertEquals(5, staff.levelRequirement)
    }

    @Test
    fun testLookupGreaterPotions() {
        val catalog = load()
        val hp = catalog.getItem("item:greater_health_potion")
        assertNotNull(hp)
        assertEquals("consumable", hp.type)
        assertEquals("heal:60", hp.useEffect)
        assertTrue(hp.stackable)

        val mp = catalog.getItem("item:greater_mana_potion")
        assertNotNull(mp)
        assertEquals("consumable", mp.type)
        assertEquals("mana:50", mp.useEffect)
    }

    @Test
    fun testLookupScrolls() {
        val catalog = load()
        val fireball = catalog.getItem("item:scroll_of_fireball")
        assertNotNull(fireball)
        assertEquals("consumable", fireball.type)
        assertEquals("damage:35", fireball.useEffect)
        assertTrue(fireball.stackable)

        val healing = catalog.getItem("item:scroll_of_healing")
        assertNotNull(healing)
        assertEquals("heal:40", healing.useEffect)
    }

    @Test
    fun testLookupTavernItems() {
        val catalog = load()
        val ale = catalog.getItem("item:ale")
        assertNotNull(ale)
        assertEquals("consumable", ale.type)
        assertEquals("heal:10", ale.useEffect)
        assertTrue(ale.stackable)

        val bread = catalog.getItem("item:bread_loaf")
        assertNotNull(bread)
        assertEquals("heal:15", bread.useEffect)
    }

    @Test
    fun testLookupEnchantedArmor() {
        val catalog = load()
        val robes = catalog.getItem("item:enchanted_robes")
        assertNotNull(robes)
        assertEquals("armor", robes.type)
        assertEquals("chest", robes.slot)
        assertEquals(4, robes.armorValue)
        assertEquals(2, robes.damageBonus)
        assertEquals(5, robes.levelRequirement)
    }

    @Test
    fun testLookupCraftingMaterials() {
        val catalog = load()

        val hide = catalog.getItem("item:marsh_hide")
        assertNotNull(hide)
        assertEquals("Marsh Hide", hide.name)
        assertEquals("crafting", hide.type)
        assertEquals(12, hide.value)
        assertTrue(hide.stackable)
        assertEquals(20, hide.maxStack)

        val essence = catalog.getItem("item:wraith_essence")
        assertNotNull(essence)
        assertEquals("Wraith Essence", essence.name)
        assertEquals("crafting", essence.type)
        assertEquals(18, essence.value)

        val shard = catalog.getItem("item:obsidian_shard")
        assertNotNull(shard)
        assertEquals("Obsidian Shard", shard.name)
        assertEquals("crafting", shard.type)
        assertEquals(25, shard.value)
    }

    @Test
    fun testGetAllItems() {
        val catalog = load()
        val all = catalog.getAllItems()
        assertTrue(all.size >= 38)
        val ids = all.map { it.id }.toSet()
        assertTrue("item:iron_sword" in ids)
        assertTrue("item:health_potion" in ids)
        assertTrue("item:wolf_pelt" in ids)
        assertTrue("item:mystic_staff" in ids)
        assertTrue("item:amulet_of_warding" in ids)
        assertTrue("item:scroll_of_fireball" in ids)
    }
}

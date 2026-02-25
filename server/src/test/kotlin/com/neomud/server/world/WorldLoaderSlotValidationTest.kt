package com.neomud.server.world

import com.neomud.server.defaultWorldSource
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertEquals

class WorldLoaderSlotValidationTest {

    private fun load() = WorldLoader.load(defaultWorldSource())

    @Test
    fun testNeckSlotItemLoadsWithoutWarning() {
        val result = load()
        val amulet = result.itemCatalog.getItem("item:amulet_of_warding")
        assertNotNull(amulet, "Amulet of Warding should exist in item catalog")
        assertEquals("neck", amulet.slot)
    }

    @Test
    fun testRingSlotItemLoadsWithoutWarning() {
        val result = load()
        val ring = result.itemCatalog.getItem("item:ring_of_intellect")
        assertNotNull(ring, "Ring of Intellect should exist in item catalog")
        assertEquals("ring", ring.slot)
    }
}

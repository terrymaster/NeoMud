package com.neomud.server.world

import com.neomud.server.defaultWorldSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ClassCatalogTest {

    private fun load() = ClassCatalog.load(defaultWorldSource())

    @Test
    fun testLoadClassesFromJson() {
        val catalog = load()
        assertEquals(15, catalog.classCount)
    }

    @Test
    fun testLookupWarrior() {
        val catalog = load()
        val warrior = catalog.getClass("WARRIOR")
        assertNotNull(warrior)
        assertEquals("Warrior", warrior.name)
        assertEquals(20, warrior.minimumStats.strength)
        assertEquals(20, warrior.minimumStats.health)
        assertEquals(6, warrior.hpPerLevelMin)
        assertEquals(10, warrior.hpPerLevelMax)
    }

    @Test
    fun testLookupMage() {
        val catalog = load()
        val mage = catalog.getClass("MAGE")
        assertNotNull(mage)
        assertEquals("Mage", mage.name)
        assertEquals(22, mage.minimumStats.intellect)
        assertEquals(6, mage.minimumStats.strength)
        assertEquals(3, mage.magicSchools["mage"])
    }

    @Test
    fun testUnknownClassReturnsNull() {
        val catalog = load()
        assertNull(catalog.getClass("UNKNOWN"))
    }

    @Test
    fun testGetAllClasses() {
        val catalog = load()
        val all = catalog.getAllClasses()
        assertEquals(15, all.size)
        val ids = all.map { it.id }.toSet()
        assertTrue("WARRIOR" in ids)
        assertTrue("MAGE" in ids)
        assertTrue("THIEF" in ids)
        assertTrue("NINJA" in ids)
        assertTrue("GYPSY" in ids)
    }

    @Test
    fun testAllClassesHaveHpRanges() {
        val catalog = load()
        for (cls in catalog.getAllClasses()) {
            assertTrue(cls.hpPerLevelMax >= cls.hpPerLevelMin, "${cls.id} hpPerLevelMax < hpPerLevelMin")
            assertTrue(cls.hpPerLevelMin >= 3, "${cls.id} hpPerLevelMin too low")
        }
    }
}

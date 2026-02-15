package com.neomud.server.world

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ClassCatalogTest {

    @Test
    fun testLoadClassesFromJson() {
        val catalog = ClassCatalog.load()
        assertEquals(15, catalog.classCount)
    }

    @Test
    fun testLookupWarrior() {
        val catalog = ClassCatalog.load()
        val warrior = catalog.getClass("WARRIOR")
        assertNotNull(warrior)
        assertEquals("Warrior", warrior.name)
        assertEquals(45, warrior.baseStats.strength)
        assertEquals(45, warrior.baseStats.health)
        assertEquals(6, warrior.hpPerLevelMin)
        assertEquals(10, warrior.hpPerLevelMax)
    }

    @Test
    fun testLookupMage() {
        val catalog = ClassCatalog.load()
        val mage = catalog.getClass("MAGE")
        assertNotNull(mage)
        assertEquals("Mage", mage.name)
        assertEquals(50, mage.baseStats.intellect)
        assertEquals(15, mage.baseStats.strength)
        assertEquals(3, mage.magicSchools["mage"])
    }

    @Test
    fun testUnknownClassReturnsNull() {
        val catalog = ClassCatalog.load()
        assertNull(catalog.getClass("UNKNOWN"))
    }

    @Test
    fun testGetAllClasses() {
        val catalog = ClassCatalog.load()
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
        val catalog = ClassCatalog.load()
        for (cls in catalog.getAllClasses()) {
            assertTrue(cls.hpPerLevelMax >= cls.hpPerLevelMin, "${cls.id} hpPerLevelMax < hpPerLevelMin")
            assertTrue(cls.hpPerLevelMin >= 3, "${cls.id} hpPerLevelMin too low")
        }
    }
}

package com.neomud.server.world

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ClassCatalogTest {

    @Test
    fun testLoadClassesFromJson() {
        val catalog = ClassCatalog.load()
        assertEquals(12, catalog.classCount)
    }

    @Test
    fun testLookupById() {
        val catalog = ClassCatalog.load()
        val fighter = catalog.getClass("FIGHTER")
        assertNotNull(fighter)
        assertEquals("Fighter", fighter.name)
        assertEquals(16, fighter.baseStats.strength)
        assertEquals(14, fighter.baseStats.constitution)
    }

    @Test
    fun testLookupWizard() {
        val catalog = ClassCatalog.load()
        val wizard = catalog.getClass("WIZARD")
        assertNotNull(wizard)
        assertEquals("Wizard", wizard.name)
        assertEquals(16, wizard.baseStats.intelligence)
        assertEquals(8, wizard.baseStats.strength)
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
        assertEquals(12, all.size)
        val ids = all.map { it.id }.toSet()
        assert("BARBARIAN" in ids)
        assert("WIZARD" in ids)
        assert("ROGUE" in ids)
    }
}

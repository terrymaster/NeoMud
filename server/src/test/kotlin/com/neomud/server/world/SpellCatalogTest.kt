package com.neomud.server.world

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SpellCatalogTest {

    @Test
    fun testLoadSpellCatalog() {
        val catalog = SpellCatalog.load()
        assertTrue(catalog.spellCount >= 20, "Should load at least 20 spells, got ${catalog.spellCount}")
    }

    @Test
    fun testGetSpellById() {
        val catalog = SpellCatalog.load()
        val spell = catalog.getSpell("MAGIC_MISSILE")
        assertNotNull(spell)
        assertEquals("Magic Missile", spell.name)
        assertEquals("mage", spell.school)
        assertEquals(5, spell.manaCost)
    }

    @Test
    fun testGetNonexistentSpell() {
        val catalog = SpellCatalog.load()
        assertNull(catalog.getSpell("NONEXISTENT"))
    }

    @Test
    fun testGetAllSpells() {
        val catalog = SpellCatalog.load()
        val all = catalog.getAllSpells()
        assertEquals(catalog.spellCount, all.size)
        assertTrue(all.any { it.id == "FIREBALL" })
        assertTrue(all.any { it.id == "MINOR_HEAL" })
    }

    @Test
    fun testGetSpellsForSchool() {
        val catalog = SpellCatalog.load()
        val mageSpells = catalog.getSpellsForSchool("mage")
        assertEquals(4, mageSpells.size)
        assertTrue(mageSpells.all { it.school == "mage" })

        val priestSpells = catalog.getSpellsForSchool("priest")
        assertEquals(4, priestSpells.size)

        val druidSpells = catalog.getSpellsForSchool("druid")
        assertEquals(4, druidSpells.size)

        val kaiSpells = catalog.getSpellsForSchool("kai")
        assertEquals(4, kaiSpells.size)

        val bardSpells = catalog.getSpellsForSchool("bard")
        assertEquals(4, bardSpells.size)
    }

    @Test
    fun testSpellProperties() {
        val catalog = SpellCatalog.load()
        val fireball = catalog.getSpell("FIREBALL")
        assertNotNull(fireball)
        assertEquals(18, fireball.manaCost)
        assertEquals(5, fireball.levelRequired)
        assertEquals(22, fireball.basePower)
        assertEquals("intellect", fireball.primaryStat)
    }
}

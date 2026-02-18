package com.neomud.server.world

import com.neomud.server.defaultWorldSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SkillCatalogTest {

    private fun load() = SkillCatalog.load(defaultWorldSource())

    @Test
    fun testLoadSkillCatalog() {
        val catalog = load()
        assertTrue(catalog.skillCount > 0, "Should load at least one skill")
    }

    @Test
    fun testGetSkillById() {
        val catalog = load()
        val sneak = catalog.getSkill("SNEAK")
        assertNotNull(sneak)
        assertEquals("Sneak", sneak.name)
        assertEquals("stealth", sneak.category)
        assertEquals("agility", sneak.primaryStat)
        assertEquals("willpower", sneak.secondaryStat)
    }

    @Test
    fun testGetNonexistentSkill() {
        val catalog = load()
        assertNull(catalog.getSkill("NONEXISTENT"))
    }

    @Test
    fun testHideSkillRemoved() {
        val catalog = load()
        assertNull(catalog.getSkill("HIDE"), "HIDE skill should no longer exist")
    }

    @Test
    fun testGetAllSkills() {
        val catalog = load()
        val all = catalog.getAllSkills()
        assertEquals(catalog.skillCount, all.size)
        assertTrue(all.any { it.id == "BACKSTAB" })
        assertTrue(all.any { it.id == "SNEAK" })
    }

    @Test
    fun testGetSkillsForClass() {
        val catalog = load()
        val thiefSkills = catalog.getSkillsForClass("THIEF")
        assertTrue(thiefSkills.any { it.id == "SNEAK" })
        assertTrue(thiefSkills.any { it.id == "BACKSTAB" })
    }

    @Test
    fun testBackstabIsPassive() {
        val catalog = load()
        val backstab = catalog.getSkill("BACKSTAB")
        assertNotNull(backstab)
        assertEquals("3", backstab.properties["damageMultiplier"])
        assertEquals(0, backstab.cooldownTicks)
        assertTrue(backstab.isPassive)
        assertTrue(backstab.classRestrictions.contains("THIEF"))
    }

    @Test
    fun testSneakIsNotPassive() {
        val catalog = load()
        val sneak = catalog.getSkill("SNEAK")
        assertNotNull(sneak)
        assertEquals(false, sneak.isPassive)
        assertEquals(2, sneak.cooldownTicks)
        assertEquals(15, sneak.difficulty)
    }
}

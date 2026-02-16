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
        val hide = catalog.getSkill("HIDE")
        assertNotNull(hide)
        assertEquals("Hide", hide.name)
        assertEquals("stealth", hide.category)
        assertEquals("agility", hide.primaryStat)
    }

    @Test
    fun testGetNonexistentSkill() {
        val catalog = load()
        assertNull(catalog.getSkill("NONEXISTENT"))
    }

    @Test
    fun testGetAllSkills() {
        val catalog = load()
        val all = catalog.getAllSkills()
        assertEquals(catalog.skillCount, all.size)
        assertTrue(all.any { it.id == "BACKSTAB" })
        assertTrue(all.any { it.id == "HIDE" })
    }

    @Test
    fun testGetSkillsForClass() {
        val catalog = load()
        val thiefSkills = catalog.getSkillsForClass("THIEF")
        assertTrue(thiefSkills.any { it.id == "HIDE" })
        assertTrue(thiefSkills.any { it.id == "BACKSTAB" })
    }

    @Test
    fun testBackstabProperties() {
        val catalog = load()
        val backstab = catalog.getSkill("BACKSTAB")
        assertNotNull(backstab)
        assertEquals("3", backstab.properties["damageMultiplier"])
        assertEquals(4, backstab.cooldownTicks)
        assertTrue(backstab.classRestrictions.contains("THIEF"))
    }
}

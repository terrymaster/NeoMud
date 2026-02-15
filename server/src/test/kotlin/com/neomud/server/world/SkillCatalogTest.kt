package com.neomud.server.world

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SkillCatalogTest {

    @Test
    fun testLoadSkillCatalog() {
        val catalog = SkillCatalog.load()
        assertTrue(catalog.skillCount > 0, "Should load at least one skill")
    }

    @Test
    fun testGetSkillById() {
        val catalog = SkillCatalog.load()
        val hide = catalog.getSkill("HIDE")
        assertNotNull(hide)
        assertEquals("Hide", hide.name)
        assertEquals("stealth", hide.category)
        assertEquals("agility", hide.primaryStat)
    }

    @Test
    fun testGetNonexistentSkill() {
        val catalog = SkillCatalog.load()
        assertNull(catalog.getSkill("NONEXISTENT"))
    }

    @Test
    fun testGetAllSkills() {
        val catalog = SkillCatalog.load()
        val all = catalog.getAllSkills()
        assertEquals(catalog.skillCount, all.size)
        assertTrue(all.any { it.id == "BACKSTAB" })
        assertTrue(all.any { it.id == "HIDE" })
    }

    @Test
    fun testGetSkillsForClass() {
        val catalog = SkillCatalog.load()
        val thiefSkills = catalog.getSkillsForClass("THIEF")
        assertTrue(thiefSkills.any { it.id == "HIDE" })
        assertTrue(thiefSkills.any { it.id == "BACKSTAB" })
    }

    @Test
    fun testBackstabProperties() {
        val catalog = SkillCatalog.load()
        val backstab = catalog.getSkill("BACKSTAB")
        assertNotNull(backstab)
        assertEquals("3", backstab.properties["damageMultiplier"])
        assertEquals(4, backstab.cooldownTicks)
        assertTrue(backstab.classRestrictions.contains("THIEF"))
    }
}

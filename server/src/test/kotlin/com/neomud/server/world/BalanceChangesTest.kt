package com.neomud.server.world

import com.neomud.server.defaultWorldSource
import com.neomud.server.game.GameConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for balance changes from issues #44, #45, #46.
 */
class BalanceChangesTest {

    private val worldSource = defaultWorldSource()
    private val classCatalog = ClassCatalog.load(worldSource)
    private val spellCatalog = SpellCatalog.load(worldSource)

    // --- #44: Orphaned spells fixed ---

    @Test
    fun `Mystic has kai school level 3`() {
        val mystic = classCatalog.getClass("MYSTIC")
        assertNotNull(mystic)
        assertEquals(3, mystic.magicSchools["kai"], "Mystic should have kai:3 to unlock Diamond Body")
    }

    @Test
    fun `Bard has bard school level 3`() {
        val bard = classCatalog.getClass("BARD")
        assertNotNull(bard)
        assertEquals(3, bard.magicSchools["bard"], "Bard should have bard:3 to unlock full bard spell tree")
    }

    @Test
    fun `Diamond Body is castable by Mystic`() {
        val mystic = classCatalog.getClass("MYSTIC")!!
        val spell = spellCatalog.getSpell("DIAMOND_BODY")!!
        val schoolLevel = mystic.magicSchools[spell.school]!!
        assertTrue(schoolLevel >= spell.schoolLevel, "Mystic (kai:$schoolLevel) should meet Diamond Body (schoolLevel:${spell.schoolLevel})")
    }

    @Test
    fun `Rallying Cry is castable by Bard`() {
        val bard = classCatalog.getClass("BARD")!!
        val spell = spellCatalog.getSpell("RALLYING_CRY")!!
        val schoolLevel = bard.magicSchools[spell.school]!!
        assertTrue(schoolLevel >= spell.schoolLevel, "Bard (bard:$schoolLevel) should meet Rallying Cry (schoolLevel:${spell.schoolLevel})")
    }

    // --- #45: Priest buff ---

    @Test
    fun `Priest xpModifier is reduced to 1_05`() {
        val priest = classCatalog.getClass("PRIEST")
        assertNotNull(priest)
        assertEquals(1.05, priest.xpModifier, "Priest XP modifier should be 1.05 (reduced from 1.1)")
    }

    @Test
    fun `Priest hpPerLevelMax is increased to 7`() {
        val priest = classCatalog.getClass("PRIEST")
        assertNotNull(priest)
        assertEquals(7, priest.hpPerLevelMax, "Priest hpPerLevelMax should be 7 (increased from 6)")
    }

    @Test
    fun `Holy Smite spell exists with correct properties`() {
        val holySmite = spellCatalog.getSpell("HOLY_SMITE")
        assertNotNull(holySmite, "Holy Smite spell should exist")
        assertEquals("priest", holySmite.school)
        assertEquals(2, holySmite.schoolLevel)
        assertEquals(3, holySmite.levelRequired)
        assertEquals(10, holySmite.manaCost)
        assertEquals(18, holySmite.basePower)
        assertEquals("willpower", holySmite.primaryStat)
        assertEquals("ENEMY", holySmite.targetType.name)
    }

    @Test
    fun `Holy Smite is castable by Priest and Cleric but not Paladin`() {
        val priest = classCatalog.getClass("PRIEST")!!
        val cleric = classCatalog.getClass("CLERIC")!!
        val paladin = classCatalog.getClass("PALADIN")!!
        val holySmite = spellCatalog.getSpell("HOLY_SMITE")!!

        assertTrue(priest.magicSchools["priest"]!! >= holySmite.schoolLevel, "Priest should cast Holy Smite")
        assertTrue(cleric.magicSchools["priest"]!! >= holySmite.schoolLevel, "Cleric should cast Holy Smite")
        assertTrue(paladin.magicSchools["priest"]!! < holySmite.schoolLevel, "Paladin should NOT cast Holy Smite (priest:1 < schoolLevel:2)")
    }

    // --- #46: Parry/Dodge buff ---

    @Test
    fun `Dodge stat divisor is 80`() {
        assertEquals(80.0, GameConfig.Combat.DODGE_STAT_DIVISOR)
    }

    @Test
    fun `Parry stat divisor is 80`() {
        assertEquals(80.0, GameConfig.Combat.PARRY_STAT_DIVISOR)
    }

    @Test
    fun `Dodge max chance is 30 percent`() {
        assertEquals(0.30, GameConfig.Combat.DODGE_MAX_CHANCE)
    }

    @Test
    fun `Parry max chance is 30 percent`() {
        assertEquals(0.30, GameConfig.Combat.PARRY_MAX_CHANCE)
    }
}

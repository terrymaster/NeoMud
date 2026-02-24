package com.neomud.server.game.skills

import com.neomud.server.game.GameConfig
import com.neomud.shared.model.SkillDef
import com.neomud.shared.model.Stats
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SkillCheckTest {

    private val sneakDifficulty = GameConfig.Stealth.SNEAK_DIFFICULTY

    private val sneakSkill = SkillDef(
        id = "SNEAK",
        name = "Sneak",
        description = "Enter stealth.",
        category = "stealth",
        primaryStat = "agility",
        secondaryStat = "willpower",
        difficulty = sneakDifficulty
    )

    private val rogueStats = Stats(
        strength = 25,
        agility = 40,
        intellect = 30,
        willpower = 25,
        health = 30,
        charm = 35
    )

    @Test
    fun testHighRollSucceeds() {
        // AGI(40) + WIL/2(12) + level/2(0) + roll(20) = 72 vs sneakDifficulty
        val result = SkillCheck.check(sneakSkill, rogueStats, level = 1, roll = 20)
        assertTrue(result.success)
        assertEquals(20, result.roll)
        assertEquals(sneakDifficulty, result.difficulty)
    }

    @Test
    fun testLowRollFails() {
        val hardSkill = sneakSkill.copy(difficulty = 100)
        val result = SkillCheck.check(hardSkill, rogueStats, level = 1, roll = 1)
        assertFalse(result.success)
        assertEquals(100, result.difficulty)
    }

    @Test
    fun testDifficultyModifier() {
        val result = SkillCheck.check(sneakSkill, rogueStats, level = 1, difficultyModifier = 100, roll = 1)
        assertFalse(result.success)
        assertEquals(sneakDifficulty + 100, result.difficulty)
    }

    @Test
    fun testLevelContributes() {
        // level/2 = 5 for level 10
        val result1 = SkillCheck.check(sneakSkill, rogueStats, level = 10, roll = 1)
        val result2 = SkillCheck.check(sneakSkill, rogueStats, level = 1, roll = 1)
        assertTrue(result1.total > result2.total)
    }

    @Test
    fun testFormulaCalculation() {
        // primary=AGI(40) + secondary/2=WIL/2(12) + level/2(2) + roll(5) = 59
        val result = SkillCheck.check(sneakSkill, rogueStats, level = 5, roll = 5)
        assertEquals(59, result.total)
        assertEquals(sneakDifficulty, result.difficulty)
        assertTrue(result.success)
    }
}

package com.neomud.server.game.combat

import com.neomud.shared.model.ActiveEffect
import com.neomud.shared.model.EffectType
import com.neomud.shared.model.Stats
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CombatUtilsTest {

    // ─── playerParry (STR-scaled, class-gated upstream) ────────────

    @Test
    fun testPlayerParryZeroStrength() {
        val stats = Stats(strength = 0, agility = 30, intellect = 30, willpower = 30, health = 30, charm = 30)
        assertEquals(0.0, CombatUtils.playerParry(stats))
    }

    @Test
    fun testPlayerParryWarriorMinStrength() {
        // Warrior min STR = 20 → 20/100 * 0.15 = 0.03 (3%)
        val stats = Stats(strength = 20, agility = 30, intellect = 30, willpower = 30, health = 30, charm = 30)
        assertTrue(kotlin.math.abs(CombatUtils.playerParry(stats) - 0.03) < 1e-9)
    }

    @Test
    fun testPlayerParryMidStrength() {
        // STR 50 → 50/100 * 0.15 = 0.075 (7.5%)
        val stats = Stats(strength = 50, agility = 30, intellect = 30, willpower = 30, health = 30, charm = 30)
        assertTrue(kotlin.math.abs(CombatUtils.playerParry(stats) - 0.075) < 1e-9)
    }

    @Test
    fun testPlayerParryMaxStrength() {
        // STR 100 → 100/100 * 0.15 = 0.15 (15%)
        val stats = Stats(strength = 100, agility = 30, intellect = 30, willpower = 30, health = 30, charm = 30)
        assertTrue(kotlin.math.abs(CombatUtils.playerParry(stats) - 0.15) < 1e-9)
    }

    @Test
    fun testPlayerParryScalesLinearly() {
        val low = CombatUtils.playerParry(Stats(strength = 30))
        val mid = CombatUtils.playerParry(Stats(strength = 60))
        val high = CombatUtils.playerParry(Stats(strength = 90))
        assertTrue(low < mid)
        assertTrue(mid < high)
        // Double the STR should give double the parry
        assertTrue(kotlin.math.abs(mid - low * 2) < 1e-9)
    }

    @Test
    fun testPlayerParryWithBuffedStats() {
        // Buffs are baked into effectiveStats before calling playerParry
        val base = Stats(strength = 40, agility = 30, intellect = 30, willpower = 30, health = 30, charm = 30)
        val buffed = CombatUtils.effectiveStats(base, listOf(
            ActiveEffect("Str Buff", EffectType.BUFF_STRENGTH, 10, magnitude = 10)
        ))
        // Effective STR = 50 → 0.075
        assertTrue(kotlin.math.abs(CombatUtils.playerParry(buffed) - 0.075) < 1e-9)
    }

    // ─── parryReduction ─────────────────────────────────────────

    @Test
    fun testParryReductionLowStrength() {
        val stats = Stats(strength = 20, agility = 30, intellect = 30, willpower = 30, health = 30, charm = 30)
        // 2 + 20/20 = 3
        assertEquals(3, CombatUtils.parryReduction(stats))
    }

    @Test
    fun testParryReductionHighStrength() {
        val stats = Stats(strength = 100, agility = 30, intellect = 30, willpower = 30, health = 30, charm = 30)
        // 2 + 100/20 = 7
        assertEquals(7, CombatUtils.parryReduction(stats))
    }

    @Test
    fun testParryReductionMinimumStrength() {
        val stats = Stats(strength = 0, agility = 30, intellect = 30, willpower = 30, health = 30, charm = 30)
        // 2 + 0/20 = 2
        assertEquals(2, CombatUtils.parryReduction(stats))
    }

    // ─── playerEvasion (AGI-scaled, class-gated upstream) ───────

    @Test
    fun testPlayerEvasionZeroAgility() {
        val stats = Stats(strength = 30, agility = 0, intellect = 30, willpower = 30, health = 30, charm = 30)
        assertEquals(0.0, CombatUtils.playerEvasion(stats))
    }

    @Test
    fun testPlayerEvasionLowAgility() {
        // AGI 20 → 20/100 * 0.15 = 0.03 (3%)
        val stats = Stats(strength = 30, agility = 20, intellect = 30, willpower = 30, health = 30, charm = 30)
        assertTrue(kotlin.math.abs(CombatUtils.playerEvasion(stats) - 0.03) < 1e-9)
    }

    @Test
    fun testPlayerEvasionMidAgility() {
        // AGI 50 → 50/100 * 0.15 = 0.075 (7.5%)
        val stats = Stats(strength = 30, agility = 50, intellect = 30, willpower = 30, health = 30, charm = 30)
        assertTrue(kotlin.math.abs(CombatUtils.playerEvasion(stats) - 0.075) < 1e-9)
    }

    @Test
    fun testPlayerEvasionMaxAgility() {
        // AGI 100 → 100/100 * 0.15 = 0.15 (15%)
        val stats = Stats(strength = 30, agility = 100, intellect = 30, willpower = 30, health = 30, charm = 30)
        assertTrue(kotlin.math.abs(CombatUtils.playerEvasion(stats) - 0.15) < 1e-9)
    }

    @Test
    fun testPlayerEvasionWithBuffedStats() {
        val base = Stats(strength = 30, agility = 40, intellect = 30, willpower = 30, health = 30, charm = 30)
        val buffed = CombatUtils.effectiveStats(base, listOf(
            ActiveEffect("Agi Buff", EffectType.BUFF_AGILITY, 10, magnitude = 10)
        ))
        // Effective AGI = 50 → 0.075
        assertTrue(kotlin.math.abs(CombatUtils.playerEvasion(buffed) - 0.075) < 1e-9)
    }

    // ─── effectiveStats ─────────────────────────────────────────

    @Test
    fun testEffectiveStatsWithBuffs() {
        val base = Stats(strength = 50, agility = 50, intellect = 50, willpower = 50, health = 50, charm = 50)
        val effects = listOf(
            ActiveEffect("Str Buff", EffectType.BUFF_STRENGTH, 10, magnitude = 5),
            ActiveEffect("Agi Buff", EffectType.BUFF_AGILITY, 10, magnitude = 3)
        )
        val effective = CombatUtils.effectiveStats(base, effects)
        assertEquals(55, effective.strength)
        assertEquals(53, effective.agility)
        assertEquals(50, effective.intellect) // unchanged
    }

    @Test
    fun testEffectiveStatsNoEffects() {
        val base = Stats(strength = 50, agility = 50, intellect = 50, willpower = 50, health = 50, charm = 50)
        val effective = CombatUtils.effectiveStats(base, emptyList())
        assertEquals(base, effective)
    }

    // ─── rollParry (statistical) ────────────────────────────────

    @Test
    fun testRollParryZeroChance() {
        repeat(100) {
            assertEquals(false, CombatUtils.rollParry(0.0))
        }
    }

    @Test
    fun testRollParryFullChance() {
        repeat(100) {
            assertEquals(true, CombatUtils.rollParry(1.0))
        }
    }

    // ─── rollEvasion (statistical) ──────────────────────────────

    @Test
    fun testRollEvasionZeroChance() {
        repeat(100) {
            assertEquals(false, CombatUtils.rollEvasion(0.0))
        }
    }

    @Test
    fun testRollEvasionFullChance() {
        repeat(100) {
            assertEquals(true, CombatUtils.rollEvasion(1.0))
        }
    }
}

package com.neomud.server.game.progression

import com.neomud.server.game.GameConfig
import com.neomud.shared.model.Stats
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StatThresholdsTest {

    // STR thresholds (if/else-if: only highest qualifying tier applies)
    private val str90Dmg = GameConfig.Thresholds.STR_90_MELEE_DAMAGE
    private val str90Hit = GameConfig.Thresholds.STR_90_HIT_BONUS
    private val str75Dmg = GameConfig.Thresholds.STR_75_MELEE_DAMAGE
    private val str75Hit = GameConfig.Thresholds.STR_75_HIT_BONUS
    private val str60Dmg = GameConfig.Thresholds.STR_60_MELEE_DAMAGE
    private val str60Hit = GameConfig.Thresholds.STR_60_HIT_BONUS
    private val str40Dmg = GameConfig.Thresholds.STR_40_MELEE_DAMAGE
    private val agi90Crit = GameConfig.Thresholds.AGI_90_CRIT_CHANCE
    private val int75Crit = GameConfig.Thresholds.INT_75_CRIT_CHANCE
    private val hea90Hp = GameConfig.Thresholds.HEA_90_HP_BONUS
    private val hea60Hp = GameConfig.Thresholds.HEA_60_HP_BONUS
    private val wil75Mp = GameConfig.Thresholds.WIL_75_MP_BONUS
    private val wil60Mp = GameConfig.Thresholds.WIL_60_MP_BONUS

    @Test
    fun testNoThresholdsForLowStats() {
        val stats = Stats(strength = 30, agility = 30, intellect = 30, willpower = 30, health = 30, charm = 30)
        val bonuses = ThresholdBonuses.compute(stats)
        assertEquals(0, bonuses.meleeDamageBonus)
        assertEquals(0.0, bonuses.critChance)
        assertEquals(0, bonuses.hpBonus)
        assertEquals(0, bonuses.mpBonus)
    }

    @Test
    fun testStrength40Threshold() {
        val stats = Stats(strength = 40, agility = 30, intellect = 30, willpower = 30, health = 30, charm = 30)
        val bonuses = ThresholdBonuses.compute(stats)
        assertEquals(str40Dmg, bonuses.meleeDamageBonus)
        assertEquals(0, bonuses.hitBonus)
    }

    @Test
    fun testStrength60Threshold() {
        val stats = Stats(strength = 60, agility = 30, intellect = 30, willpower = 30, health = 30, charm = 30)
        val bonuses = ThresholdBonuses.compute(stats)
        assertEquals(str60Dmg, bonuses.meleeDamageBonus)
        assertEquals(str60Hit, bonuses.hitBonus)
    }

    @Test
    fun testStrength75Threshold() {
        val stats = Stats(strength = 75, agility = 30, intellect = 30, willpower = 30, health = 30, charm = 30)
        val bonuses = ThresholdBonuses.compute(stats)
        assertEquals(str75Dmg, bonuses.meleeDamageBonus)
        assertEquals(str75Hit, bonuses.hitBonus)
    }

    @Test
    fun testStrength90Threshold() {
        val stats = Stats(strength = 90, agility = 30, intellect = 30, willpower = 30, health = 30, charm = 30)
        val bonuses = ThresholdBonuses.compute(stats)
        assertEquals(str90Dmg, bonuses.meleeDamageBonus)
        assertEquals(str90Hit, bonuses.hitBonus)
    }

    @Test
    fun testAgility90GivesCrit() {
        val stats = Stats(strength = 30, agility = 90, intellect = 30, willpower = 30, health = 30, charm = 30)
        val bonuses = ThresholdBonuses.compute(stats)
        assertEquals(agi90Crit, bonuses.critChance)
    }

    @Test
    fun testHealth90GivesHpBonus() {
        val stats = Stats(strength = 30, agility = 30, intellect = 30, willpower = 30, health = 90, charm = 30)
        val bonuses = ThresholdBonuses.compute(stats)
        assertEquals(hea90Hp, bonuses.hpBonus)
    }

    @Test
    fun testWillpower75GivesMpBonus() {
        val stats = Stats(strength = 30, agility = 30, intellect = 30, willpower = 75, health = 30, charm = 30)
        val bonuses = ThresholdBonuses.compute(stats)
        assertEquals(wil75Mp, bonuses.mpBonus)
    }

    @Test
    fun testIntellect75GivesCrit() {
        val stats = Stats(strength = 30, agility = 30, intellect = 75, willpower = 30, health = 30, charm = 30)
        val bonuses = ThresholdBonuses.compute(stats)
        assertEquals(int75Crit, bonuses.critChance)
    }

    @Test
    fun testCombinedThresholds() {
        val stats = Stats(strength = 90, agility = 90, intellect = 75, willpower = 60, health = 60, charm = 75)
        val bonuses = ThresholdBonuses.compute(stats)
        assertEquals(str90Dmg, bonuses.meleeDamageBonus)
        // AGI 90 crit + INT 75 crit (these are additive, not else-if gated)
        assertEquals(agi90Crit + int75Crit, bonuses.critChance)
        assertEquals(hea60Hp, bonuses.hpBonus)
        assertEquals(wil60Mp, bonuses.mpBonus)
    }
}

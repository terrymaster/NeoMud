package com.neomud.server.game.progression

import com.neomud.shared.model.Stats
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StatThresholdsTest {

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
        assertEquals(1, bonuses.meleeDamageBonus)
        assertEquals(0, bonuses.hitBonus)
    }

    @Test
    fun testStrength60Threshold() {
        val stats = Stats(strength = 60, agility = 30, intellect = 30, willpower = 30, health = 30, charm = 30)
        val bonuses = ThresholdBonuses.compute(stats)
        assertEquals(2, bonuses.meleeDamageBonus)
        assertEquals(1, bonuses.hitBonus)
    }

    @Test
    fun testStrength75Threshold() {
        val stats = Stats(strength = 75, agility = 30, intellect = 30, willpower = 30, health = 30, charm = 30)
        val bonuses = ThresholdBonuses.compute(stats)
        assertEquals(3, bonuses.meleeDamageBonus)
        assertEquals(2, bonuses.hitBonus)
    }

    @Test
    fun testStrength90Threshold() {
        val stats = Stats(strength = 90, agility = 30, intellect = 30, willpower = 30, health = 30, charm = 30)
        val bonuses = ThresholdBonuses.compute(stats)
        assertEquals(5, bonuses.meleeDamageBonus)
        assertEquals(3, bonuses.hitBonus)
    }

    @Test
    fun testAgility90GivesCrit() {
        val stats = Stats(strength = 30, agility = 90, intellect = 30, willpower = 30, health = 30, charm = 30)
        val bonuses = ThresholdBonuses.compute(stats)
        assertEquals(0.05, bonuses.critChance)
    }

    @Test
    fun testHealth90GivesHpBonus() {
        val stats = Stats(strength = 30, agility = 30, intellect = 30, willpower = 30, health = 90, charm = 30)
        val bonuses = ThresholdBonuses.compute(stats)
        assertEquals(50, bonuses.hpBonus)
    }

    @Test
    fun testWillpower75GivesMpBonus() {
        val stats = Stats(strength = 30, agility = 30, intellect = 30, willpower = 75, health = 30, charm = 30)
        val bonuses = ThresholdBonuses.compute(stats)
        assertEquals(15, bonuses.mpBonus)
    }

    @Test
    fun testIntellect75GivesCrit() {
        val stats = Stats(strength = 30, agility = 30, intellect = 75, willpower = 30, health = 30, charm = 30)
        val bonuses = ThresholdBonuses.compute(stats)
        assertEquals(0.05, bonuses.critChance)
    }

    @Test
    fun testCombinedThresholds() {
        val stats = Stats(strength = 90, agility = 90, intellect = 75, willpower = 60, health = 60, charm = 75)
        val bonuses = ThresholdBonuses.compute(stats)
        assertEquals(5, bonuses.meleeDamageBonus)
        // AGI 90 crit (0.05) + INT 75 crit (0.05) = 0.10
        assertEquals(0.10, bonuses.critChance)
        assertEquals(15, bonuses.hpBonus)
        assertEquals(10, bonuses.mpBonus)
    }
}

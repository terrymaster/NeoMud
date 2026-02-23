package com.neomud.server.game

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameConfigTest {

    @Test
    fun testGraceTicksPositive() {
        assertTrue(GameConfig.Combat.GRACE_TICKS > 0, "Grace period should be at least 1 tick")
    }

    @Test
    fun testCombatConstants() {
        assertEquals(5, GameConfig.Combat.MIN_HIT_CHANCE)
        assertEquals(95, GameConfig.Combat.MAX_HIT_CHANCE)
        assertEquals(50, GameConfig.Combat.BASE_HIT_CHANCE)
        assertEquals(3, GameConfig.Combat.UNARMED_DAMAGE_RANGE)
        assertEquals(1.5, GameConfig.Combat.CRIT_DAMAGE_MULTIPLIER)
        assertEquals(3, GameConfig.Combat.BACKSTAB_DAMAGE_MULTIPLIER)
    }

    @Test
    fun testProgressionConstants() {
        assertEquals(30, GameConfig.Progression.MAX_LEVEL)
        assertEquals(100, GameConfig.Progression.XP_BASE_MULTIPLIER)
        assertEquals(100L, GameConfig.Progression.XP_MINIMUM)
        assertTrue(GameConfig.Progression.DEATH_XP_LOSS_PERCENT in 0.0..1.0)
    }

    @Test
    fun testSkillConstants() {
        assertTrue(GameConfig.Skills.BASH_DAMAGE_RANGE > 0)
        assertTrue(GameConfig.Skills.BASH_STUN_CHANCE in 1..100)
        assertTrue(GameConfig.Skills.BASH_COOLDOWN_TICKS > 0)
        assertTrue(GameConfig.Skills.KICK_DAMAGE_RANGE > 0)
        assertTrue(GameConfig.Skills.KICK_COOLDOWN_TICKS > 0)
    }

    @Test
    fun testNpcConstants() {
        assertTrue(GameConfig.Npc.WANDER_MOVE_TICKS > 0)
        assertTrue(GameConfig.Npc.PATROL_MOVE_TICKS > 0)
        assertTrue(GameConfig.Npc.PURSUIT_MAX_TICKS > 0)
        assertTrue(GameConfig.Npc.PURSUIT_MOVE_TICKS > 0)
    }

    @Test
    fun testTrailConstants() {
        assertTrue(GameConfig.Trails.MAX_ENTRIES_PER_ROOM > 0)
        assertTrue(GameConfig.Trails.LIFETIME_MS > 0)
        assertTrue(GameConfig.Trails.STALENESS_PENALTY_MAX > 0)
    }

    @Test
    fun testXpModifiersOrdered() {
        // Higher level difference should give more XP
        assertTrue(GameConfig.Progression.XP_MOD_5_ABOVE > GameConfig.Progression.XP_MOD_3_ABOVE)
        assertTrue(GameConfig.Progression.XP_MOD_3_ABOVE > GameConfig.Progression.XP_MOD_1_ABOVE)
        assertTrue(GameConfig.Progression.XP_MOD_1_ABOVE > GameConfig.Progression.XP_MOD_SAME)
        assertTrue(GameConfig.Progression.XP_MOD_SAME > GameConfig.Progression.XP_MOD_2_BELOW)
        assertTrue(GameConfig.Progression.XP_MOD_2_BELOW > GameConfig.Progression.XP_MOD_4_BELOW)
        assertTrue(GameConfig.Progression.XP_MOD_4_BELOW > GameConfig.Progression.XP_MOD_5_PLUS_BELOW)
    }

    @Test
    fun testCpTierProgression() {
        assertTrue(GameConfig.Progression.CP_PER_LEVEL_LOW < GameConfig.Progression.CP_PER_LEVEL_MID)
        assertTrue(GameConfig.Progression.CP_PER_LEVEL_MID < GameConfig.Progression.CP_PER_LEVEL_HIGH)
        assertTrue(GameConfig.Progression.CP_TIER_2_LEVEL < GameConfig.Progression.CP_TIER_3_LEVEL)
    }
}

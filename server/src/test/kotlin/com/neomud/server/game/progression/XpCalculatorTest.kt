package com.neomud.server.game.progression

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class XpCalculatorTest {

    @Test
    fun testXpForLevel1() {
        val xp = XpCalculator.xpForLevel(1)
        assertEquals(100, xp)
    }

    @Test
    fun testXpCurveIncreases() {
        val xp1 = XpCalculator.xpForLevel(1)
        val xp5 = XpCalculator.xpForLevel(5)
        val xp10 = XpCalculator.xpForLevel(10)
        val xp20 = XpCalculator.xpForLevel(20)
        assertTrue(xp5 > xp1)
        assertTrue(xp10 > xp5)
        assertTrue(xp20 > xp10)
    }

    @Test
    fun testKillXpSameLevel() {
        val xp = XpCalculator.xpForKill(5, 5, 50)
        assertEquals(50, xp)
    }

    @Test
    fun testKillXpHigherNpc() {
        val xp = XpCalculator.xpForKill(10, 5, 50)
        assertEquals(75, xp) // 1.5x for 5+ above
    }

    @Test
    fun testKillXpLowerNpc() {
        val xp = XpCalculator.xpForKill(1, 10, 50)
        assertEquals(5, xp) // 0.1x for 5+ below
    }

    @Test
    fun testKillXpNeverZero() {
        val xp = XpCalculator.xpForKill(1, 30, 1)
        assertTrue(xp >= 1)
    }

    @Test
    fun testAdjustedXpWithModifiers() {
        val base = XpCalculator.xpForLevel(5)
        val adjusted = XpCalculator.adjustedXpForLevel(5, 1.1, 1.15)
        assertTrue(adjusted > base)
    }

    @Test
    fun testCpTiers() {
        assertEquals(10, XpCalculator.cpForLevel(1))
        assertEquals(10, XpCalculator.cpForLevel(10))
        assertEquals(15, XpCalculator.cpForLevel(11))
        assertEquals(15, XpCalculator.cpForLevel(20))
        assertEquals(20, XpCalculator.cpForLevel(21))
        assertEquals(20, XpCalculator.cpForLevel(30))
    }

    @Test
    fun testReadyToLevel() {
        assertTrue(XpCalculator.isReadyToLevel(100, 100, 1))
        assertTrue(XpCalculator.isReadyToLevel(150, 100, 1))
        assertFalse(XpCalculator.isReadyToLevel(50, 100, 1))
    }

    @Test
    fun testCannotExceedMaxLevel() {
        assertFalse(XpCalculator.isReadyToLevel(999999, 100, 30))
    }
}

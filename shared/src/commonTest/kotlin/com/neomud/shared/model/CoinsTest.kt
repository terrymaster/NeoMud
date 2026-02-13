package com.neomud.shared.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CoinsTest {

    @Test
    fun testEmptyCoins() {
        assertTrue(Coins().isEmpty())
        assertTrue(Coins(0, 0, 0, 0).isEmpty())
    }

    @Test
    fun testNonEmptyCoins() {
        assertFalse(Coins(copper = 1).isEmpty())
        assertFalse(Coins(silver = 1).isEmpty())
        assertFalse(Coins(gold = 1).isEmpty())
        assertFalse(Coins(platinum = 1).isEmpty())
    }

    @Test
    fun testPlus() {
        val a = Coins(copper = 10, silver = 5, gold = 2, platinum = 1)
        val b = Coins(copper = 3, silver = 7, gold = 0, platinum = 2)
        val sum = a + b
        assertEquals(13, sum.copper)
        assertEquals(12, sum.silver)
        assertEquals(2, sum.gold)
        assertEquals(3, sum.platinum)
    }

    @Test
    fun testTotalCopper() {
        val coins = Coins(copper = 50, silver = 3, gold = 1, platinum = 0)
        // 50 + 300 + 10000 = 10350
        assertEquals(10350L, coins.totalCopper())
    }

    @Test
    fun testTotalCopperWithPlatinum() {
        val coins = Coins(copper = 0, silver = 0, gold = 0, platinum = 1)
        assertEquals(1_000_000L, coins.totalCopper())
    }
}

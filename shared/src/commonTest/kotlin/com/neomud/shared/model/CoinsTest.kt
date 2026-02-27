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

    // ─── buyPriceCopper ─────────────────────────────────────────

    @Test
    fun testBuyPriceWithoutHaggle() {
        // Without haggle, buy price is full value
        assertEquals(100L, Coins.buyPriceCopper(100, 1, 50, hasHaggle = false))
        assertEquals(500L, Coins.buyPriceCopper(100, 5, 50, hasHaggle = false))
    }

    @Test
    fun testBuyPriceWithHaggleAppliesCharmDiscount() {
        // charm=100 → discountPercent = min(100*15/100, 15) = 15%
        // 100 * (100-15) / 100 = 85
        assertEquals(85L, Coins.buyPriceCopper(100, 1, 100, hasHaggle = true))
    }

    @Test
    fun testBuyPriceWithHaggleLowCharm() {
        // charm=10 → discountPercent = min(10*15/100, 15) = 1
        // 100 * (100-1) / 100 = 99
        assertEquals(99L, Coins.buyPriceCopper(100, 1, 10, hasHaggle = true))
    }

    @Test
    fun testBuyPriceWithHaggleQuantity() {
        // charm=100 → 15% discount, quantity=3
        // 100 * 3 * 85 / 100 = 255
        assertEquals(255L, Coins.buyPriceCopper(100, 3, 100, hasHaggle = true))
    }

    @Test
    fun testBuyPriceNeverBelowOne() {
        assertEquals(1L, Coins.buyPriceCopper(1, 1, 100, hasHaggle = true))
    }

    // ─── sellPriceCopper ────────────────────────────────────────
    // Default formula: sellPercent = 50 + charm * 49 / 100, capped at 99%

    @Test
    fun testSellPriceBasicNoHaggle() {
        // charm=0 → sellPercent = 50 + 0 = 50
        // 100 * 1 * 50 / 100 = 50
        assertEquals(50L, Coins.sellPriceCopper(100, 1, 0, hasHaggle = false))
    }

    @Test
    fun testSellPriceHighCharmNoHaggle() {
        // charm=100 → sellPercent = 50 + 100*49/100 = 50 + 49 = 99
        assertEquals(99L, Coins.sellPriceCopper(100, 1, 100, hasHaggle = false))
    }

    @Test
    fun testSellPriceWithHaggleBonus() {
        // charm=50, no haggle → sellPercent = 50 + 50*49/100 = 50 + 24 = 74
        val noHaggle = Coins.sellPriceCopper(100, 1, 50, hasHaggle = false)
        // charm=50, haggle → sellPercent = 50 + 24 + 50*10/100 = 50 + 24 + 5 = 79
        val withHaggle = Coins.sellPriceCopper(100, 1, 50, hasHaggle = true)
        assertTrue(withHaggle > noHaggle)
        assertEquals(74L, noHaggle)
        assertEquals(79L, withHaggle)
    }

    @Test
    fun testSellPriceCappedAt99Percent() {
        // Even with haggle + max charm, cannot exceed 99%
        val price = Coins.sellPriceCopper(100, 1, 100, hasHaggle = true)
        assertTrue(price <= 99L)
    }

    @Test
    fun testSellPriceNeverBelowOne() {
        assertEquals(1L, Coins.sellPriceCopper(1, 1, 0, hasHaggle = false))
    }

    @Test
    fun testSellPriceQuantityMultiplier() {
        val single = Coins.sellPriceCopper(100, 1, 50)
        val triple = Coins.sellPriceCopper(100, 3, 50)
        assertEquals(single * 3, triple)
    }

    @Test
    fun testSellPriceCustomBasePercent() {
        // Verify custom parameters override defaults
        val price = Coins.sellPriceCopper(100, 1, 0, basePercent = 30, charmScale = 70)
        assertEquals(30L, price)
    }
}

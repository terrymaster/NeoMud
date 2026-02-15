package com.neomud.server.game.progression

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class CpAllocatorTest {

    @Test
    fun testCostTier1() {
        // 0-9 above base costs 1 CP each
        assertEquals(1, CpAllocator.costToRaiseStat(30, 30))
        assertEquals(1, CpAllocator.costToRaiseStat(39, 30))
    }

    @Test
    fun testCostTier2() {
        // 10-19 above base costs 2 CP each
        assertEquals(2, CpAllocator.costToRaiseStat(40, 30))
        assertEquals(2, CpAllocator.costToRaiseStat(49, 30))
    }

    @Test
    fun testCostTier3() {
        // 20+ above base costs 3 CP each
        assertEquals(3, CpAllocator.costToRaiseStat(50, 30))
        assertEquals(3, CpAllocator.costToRaiseStat(60, 30))
    }

    @Test
    fun testAllocateSinglePoint() {
        val result = CpAllocator.allocate(30, 30, 10, 1)
        assertNotNull(result)
        assertEquals(31, result.newValue)
        assertEquals(9, result.remainingCp)
        assertEquals(1, result.totalCpSpent)
    }

    @Test
    fun testAllocateMultiplePoints() {
        val result = CpAllocator.allocate(30, 30, 10, 5)
        assertNotNull(result)
        assertEquals(35, result.newValue)
        assertEquals(5, result.remainingCp)
        assertEquals(5, result.totalCpSpent)
    }

    @Test
    fun testAllocateCrossTier() {
        // Start at 39 (base 30, 9 above), allocate 2 points
        // First point costs 1 (still tier 1), second costs 2 (tier 2)
        val result = CpAllocator.allocate(39, 30, 10, 2)
        assertNotNull(result)
        assertEquals(41, result.newValue)
        assertEquals(7, result.remainingCp)
        assertEquals(3, result.totalCpSpent)
    }

    @Test
    fun testAllocateInsufficientCp() {
        val result = CpAllocator.allocate(40, 30, 1, 1)
        // Cost is 2 but only 1 CP available
        assertNull(result)
    }

    @Test
    fun testAllocatePartialFailure() {
        // 39 above base, cost is 1 for first point (tier 1 boundary), 2 for second
        // With only 2 CP, first succeeds (cost 1), second succeeds (cost 2 but wait...)
        // Actually at value 39, above = 9, cost = 1. After +1, value=40, above=10, cost=2.
        // Total = 3, but we only have 2 CP. So should fail.
        val result = CpAllocator.allocate(39, 30, 2, 2)
        assertNull(result)
    }

    @Test
    fun testAllocateExactCp() {
        val result = CpAllocator.allocate(30, 30, 3, 3)
        assertNotNull(result)
        assertEquals(33, result.newValue)
        assertEquals(0, result.remainingCp)
        assertEquals(3, result.totalCpSpent)
    }
}

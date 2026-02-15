package com.neomud.shared.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StatAllocatorTest {

    private val minimum = Stats(strength = 10, agility = 10, intellect = 10, willpower = 10, health = 10, charm = 10)

    @Test
    fun testCpCostTier1() {
        // 0-9 above minimum costs 1 CP each
        assertEquals(1, StatAllocator.cpCostForPoint(10, 10))
        assertEquals(1, StatAllocator.cpCostForPoint(19, 10))
    }

    @Test
    fun testCpCostTier2() {
        // 10-19 above minimum costs 2 CP each
        assertEquals(2, StatAllocator.cpCostForPoint(20, 10))
        assertEquals(2, StatAllocator.cpCostForPoint(29, 10))
    }

    @Test
    fun testCpCostTier3() {
        // 20+ above minimum costs 3 CP each
        assertEquals(3, StatAllocator.cpCostForPoint(30, 10))
        assertEquals(3, StatAllocator.cpCostForPoint(50, 10))
    }

    @Test
    fun testTotalCpUsedAllAtMinimum() {
        assertEquals(0, StatAllocator.totalCpUsed(minimum, minimum))
    }

    @Test
    fun testTotalCpUsedSimple() {
        // 5 points above minimum in strength = 5 CP (tier 1)
        val allocated = minimum.copy(strength = 15)
        assertEquals(5, StatAllocator.totalCpUsed(allocated, minimum))
    }

    @Test
    fun testTotalCpUsedCrossTier() {
        // 12 points above minimum: first 10 at 1 CP = 10, next 2 at 2 CP = 4, total = 14
        val allocated = minimum.copy(strength = 22)
        assertEquals(14, StatAllocator.totalCpUsed(allocated, minimum))
    }

    @Test
    fun testTotalCpUsedMultipleStats() {
        // 5 in str + 5 in agi = 10 CP
        val allocated = minimum.copy(strength = 15, agility = 15)
        assertEquals(10, StatAllocator.totalCpUsed(allocated, minimum))
    }

    @Test
    fun testValidAllocationExact() {
        // 60 CP: 10 points in each of 6 stats = 60 CP (all tier 1)
        val allocated = Stats(20, 20, 20, 20, 20, 20)
        assertTrue(StatAllocator.isValidAllocation(allocated, minimum))
    }

    @Test
    fun testValidAllocationUnderBudget() {
        val allocated = minimum.copy(strength = 15)
        assertTrue(StatAllocator.isValidAllocation(allocated, minimum))
    }

    @Test
    fun testInvalidAllocationBelowMinimum() {
        val allocated = minimum.copy(strength = 5)
        assertFalse(StatAllocator.isValidAllocation(allocated, minimum))
    }

    @Test
    fun testInvalidAllocationOverBudget() {
        // Way over budget
        val allocated = Stats(50, 50, 50, 50, 50, 50)
        assertFalse(StatAllocator.isValidAllocation(allocated, minimum))
    }

    @Test
    fun testCpPoolConstant() {
        assertEquals(60, StatAllocator.CP_POOL)
    }

    @Test
    fun testAsymmetricMinimums() {
        val asymMin = Stats(strength = 20, agility = 8, intellect = 22, willpower = 15, health = 8, charm = 10)
        // Allocating at minimum costs 0
        assertEquals(0, StatAllocator.totalCpUsed(asymMin, asymMin))
        // Adding 1 to strength (which is already 20, but 0 above min) costs 1
        val allocated = asymMin.copy(strength = 21)
        assertEquals(1, StatAllocator.totalCpUsed(allocated, asymMin))
    }
}

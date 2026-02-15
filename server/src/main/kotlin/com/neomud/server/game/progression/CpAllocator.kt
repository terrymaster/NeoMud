package com.neomud.server.game.progression

object CpAllocator {

    fun costToRaiseStat(currentValue: Int, baseValue: Int): Int {
        val above = currentValue - baseValue
        return when {
            above < 10 -> 1
            above < 20 -> 2
            else -> 3
        }
    }

    fun allocate(currentValue: Int, baseValue: Int, unspentCp: Int, points: Int = 1): AllocationResult? {
        var value = currentValue
        var remaining = unspentCp
        var totalCost = 0
        repeat(points) {
            val cost = costToRaiseStat(value, baseValue)
            if (remaining < cost) return null
            remaining -= cost
            totalCost += cost
            value++
        }
        return AllocationResult(newValue = value, remainingCp = remaining, totalCpSpent = totalCost)
    }

    data class AllocationResult(
        val newValue: Int,
        val remainingCp: Int,
        val totalCpSpent: Int
    )
}

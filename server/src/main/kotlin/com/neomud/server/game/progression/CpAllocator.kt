package com.neomud.server.game.progression

import com.neomud.shared.model.Stats

object CpAllocator {

    fun costToRaiseStat(currentValue: Int, baseValue: Int): Int {
        val above = currentValue - baseValue
        return when {
            above < 10 -> 1
            above < 20 -> 2
            else -> 3
        }
    }

    /**
     * Calculates total CP cost to raise all stats from [baseStats] to [stats].
     * Uses the same escalating cost curve per stat point.
     */
    fun totalCpUsed(stats: Stats, baseStats: Stats): Int {
        return cpForStat(stats.strength, baseStats.strength) +
                cpForStat(stats.agility, baseStats.agility) +
                cpForStat(stats.intellect, baseStats.intellect) +
                cpForStat(stats.willpower, baseStats.willpower) +
                cpForStat(stats.health, baseStats.health) +
                cpForStat(stats.charm, baseStats.charm)
    }

    private fun cpForStat(current: Int, base: Int): Int {
        var cost = 0
        for (i in 0 until (current - base)) {
            cost += when {
                i < 10 -> 1
                i < 20 -> 2
                else -> 3
            }
        }
        return cost
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

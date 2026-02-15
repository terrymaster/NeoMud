package com.neomud.shared.model

object StatAllocator {
    const val CP_POOL = 60

    fun cpCostForPoint(currentValue: Int, minimumValue: Int): Int {
        val above = currentValue - minimumValue
        return when {
            above < 10 -> 1
            above < 20 -> 2
            else -> 3
        }
    }

    fun totalCpUsed(allocated: Stats, minimum: Stats): Int {
        return cpForStat(allocated.strength, minimum.strength) +
                cpForStat(allocated.agility, minimum.agility) +
                cpForStat(allocated.intellect, minimum.intellect) +
                cpForStat(allocated.willpower, minimum.willpower) +
                cpForStat(allocated.health, minimum.health) +
                cpForStat(allocated.charm, minimum.charm)
    }

    fun isValidAllocation(allocated: Stats, minimum: Stats, cpPool: Int = CP_POOL): Boolean {
        if (allocated.strength < minimum.strength ||
            allocated.agility < minimum.agility ||
            allocated.intellect < minimum.intellect ||
            allocated.willpower < minimum.willpower ||
            allocated.health < minimum.health ||
            allocated.charm < minimum.charm
        ) return false
        return totalCpUsed(allocated, minimum) <= cpPool
    }

    private fun cpForStat(current: Int, minimum: Int): Int {
        var cost = 0
        for (v in minimum until current) {
            cost += cpCostForPoint(v, minimum)
        }
        return cost
    }
}

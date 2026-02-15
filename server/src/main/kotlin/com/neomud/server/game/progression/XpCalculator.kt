package com.neomud.server.game.progression

import kotlin.math.pow
import kotlin.math.roundToLong

object XpCalculator {

    private const val MAX_LEVEL = 30

    fun xpForLevel(level: Int): Long {
        return (100 * level.toDouble().pow(2.2)).roundToLong().coerceAtLeast(100)
    }

    fun xpForKill(npcLevel: Int, playerLevel: Int, baseXp: Long): Long {
        val diff = npcLevel - playerLevel
        val modifier = when {
            diff >= 5 -> 1.5
            diff >= 3 -> 1.25
            diff >= 1 -> 1.1
            diff == 0 -> 1.0
            diff >= -2 -> 0.75
            diff >= -4 -> 0.5
            else -> 0.1 // 5+ below
        }
        return (baseXp * modifier).roundToLong().coerceAtLeast(1)
    }

    fun adjustedXpForLevel(level: Int, raceXpMod: Double, classXpMod: Double): Long {
        return (xpForLevel(level) * raceXpMod * classXpMod).roundToLong().coerceAtLeast(100)
    }

    fun cpForLevel(level: Int): Int = when {
        level <= 10 -> 10
        level <= 20 -> 15
        else -> 20
    }

    fun isReadyToLevel(currentXp: Long, xpToNextLevel: Long, level: Int): Boolean {
        return level < MAX_LEVEL && currentXp >= xpToNextLevel
    }
}

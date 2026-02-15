package com.neomud.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class Coins(
    val copper: Int = 0,
    val silver: Int = 0,
    val gold: Int = 0,
    val platinum: Int = 0
) {
    fun isEmpty(): Boolean = copper == 0 && silver == 0 && gold == 0 && platinum == 0

    operator fun plus(other: Coins) = Coins(
        copper = copper + other.copper,
        silver = silver + other.silver,
        gold = gold + other.gold,
        platinum = platinum + other.platinum
    )

    fun totalCopper(): Long =
        copper.toLong() +
        silver.toLong() * 100 +
        gold.toLong() * 10_000 +
        platinum.toLong() * 1_000_000

    fun displayString(): String {
        val parts = mutableListOf<String>()
        if (platinum > 0) parts.add("${platinum}p")
        if (gold > 0) parts.add("${gold}g")
        if (silver > 0) parts.add("${silver}s")
        if (copper > 0 || parts.isEmpty()) parts.add("${copper}c")
        return parts.joinToString(" ")
    }

    companion object {
        fun fromCopper(total: Long): Coins {
            var remaining = total.coerceAtLeast(0L)
            val plat = (remaining / 1_000_000).toInt()
            remaining %= 1_000_000
            val g = (remaining / 10_000).toInt()
            remaining %= 10_000
            val s = (remaining / 100).toInt()
            remaining %= 100
            val c = remaining.toInt()
            return Coins(copper = c, silver = s, gold = g, platinum = plat)
        }
    }
}

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
}

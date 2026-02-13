package com.neomud.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class CoinDrop(
    val minCopper: Int = 0,
    val maxCopper: Int = 0,
    val minSilver: Int = 0,
    val maxSilver: Int = 0,
    val minGold: Int = 0,
    val maxGold: Int = 0,
    val minPlatinum: Int = 0,
    val maxPlatinum: Int = 0
)

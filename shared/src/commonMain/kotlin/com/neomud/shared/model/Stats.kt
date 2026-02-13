package com.neomud.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class Stats(
    val strength: Int,
    val dexterity: Int,
    val constitution: Int,
    val intelligence: Int,
    val wisdom: Int
) {
    val maxHitPoints: Int get() = 50 + (constitution * 5) + (strength * 2)
}

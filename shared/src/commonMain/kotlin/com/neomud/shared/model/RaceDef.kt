package com.neomud.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class RaceDef(
    val id: String,
    val name: String,
    val description: String,
    val statModifiers: Stats = Stats(0, 0, 0, 0, 0, 0),
    val xpModifier: Double = 1.0
)

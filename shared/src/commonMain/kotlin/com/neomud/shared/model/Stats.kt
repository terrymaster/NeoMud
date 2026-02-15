package com.neomud.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class Stats(
    val strength: Int = 30,
    val agility: Int = 30,
    val intellect: Int = 30,
    val willpower: Int = 30,
    val health: Int = 30,
    val charm: Int = 30
)

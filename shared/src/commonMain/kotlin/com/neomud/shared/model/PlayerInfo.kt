package com.neomud.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class PlayerInfo(
    val name: String,
    val characterClass: String,
    val race: String,
    val gender: String = "neutral",
    val level: Int,
    val spriteUrl: String = ""
)

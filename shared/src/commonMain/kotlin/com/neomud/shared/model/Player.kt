package com.neomud.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class Player(
    val name: String,
    val characterClass: String,
    val stats: Stats,
    val currentHp: Int,
    val maxHp: Int,
    val currentMp: Int = 0,
    val maxMp: Int = 0,
    val level: Int,
    val currentRoomId: RoomId,
    val race: String = "",
    val gender: String = "neutral",
    val currentXp: Long = 0,
    val xpToNextLevel: Long = 0,
    val unspentCp: Int = 0,
    val totalCpEarned: Int = 0,
    val isAdmin: Boolean = false,
    val imagePrompt: String = "",
    val imageStyle: String = "",
    val imageNegativePrompt: String = ""
)

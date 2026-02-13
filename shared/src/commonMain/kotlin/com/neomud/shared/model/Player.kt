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
    val currentRoomId: RoomId
)

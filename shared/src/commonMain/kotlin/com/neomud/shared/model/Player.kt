package com.neomud.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class Player(
    val name: String,
    val characterClass: CharacterClass,
    val stats: Stats,
    val currentHp: Int,
    val maxHp: Int,
    val level: Int,
    val currentRoomId: RoomId
)

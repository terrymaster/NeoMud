package com.neomud.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class Npc(
    val id: String,
    val name: String,
    val description: String,
    val currentRoomId: RoomId,
    val behaviorType: String,
    val hostile: Boolean = false
)

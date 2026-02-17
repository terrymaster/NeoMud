package com.neomud.shared.model

import kotlinx.serialization.Serializable

typealias RoomId = String

@Serializable
data class RoomEffect(
    val type: String,
    val value: Int = 0,
    val message: String = "",
    val sound: String = ""
)

@Serializable
data class Room(
    val id: RoomId,
    val name: String,
    val description: String,
    val exits: Map<Direction, RoomId>,
    val zoneId: String,
    val x: Int,
    val y: Int,
    val backgroundImage: String = "",
    val effects: List<RoomEffect> = emptyList(),
    val bgm: String = "",
    val departSound: String = "",
    val lockedExits: Map<Direction, Int> = emptyMap()
)

@Serializable
data class MapRoom(
    val id: RoomId,
    val name: String,
    val x: Int,
    val y: Int,
    val exits: Map<Direction, RoomId>,
    val hasPlayers: Boolean = false,
    val hasNpcs: Boolean = false,
    val backgroundImage: String = ""
)

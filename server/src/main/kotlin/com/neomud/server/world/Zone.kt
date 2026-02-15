package com.neomud.server.world

import com.neomud.shared.model.Direction
import com.neomud.shared.model.RoomId
import kotlinx.serialization.Serializable

@Serializable
data class SpawnConfig(
    val maxEntities: Int = 0,
    val maxPerRoom: Int = 0,
    val rateTicks: Int = 0
)

@Serializable
data class ZoneData(
    val id: String,
    val name: String,
    val description: String,
    val safe: Boolean = true,
    val rooms: List<RoomData>,
    val npcs: List<NpcData> = emptyList(),
    val spawnConfig: SpawnConfig = SpawnConfig(),
    val spawnRoom: String? = null
)

@Serializable
data class RoomData(
    val id: RoomId,
    val name: String,
    val description: String,
    val x: Int,
    val y: Int,
    val exits: Map<Direction, RoomId>,
    val backgroundImage: String = "",
    val healPerTick: Int = 0
)

@Serializable
data class NpcData(
    val id: String,
    val name: String,
    val description: String,
    val startRoomId: RoomId,
    val behaviorType: String,
    val patrolRoute: List<RoomId> = emptyList(),
    val hostile: Boolean = false,
    val maxHp: Int = 0,
    val damage: Int = 0,
    val level: Int = 1,
    val perception: Int = 0,
    val xpReward: Long = 0
)

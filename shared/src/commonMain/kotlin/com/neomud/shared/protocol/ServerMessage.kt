package com.neomud.shared.protocol

import com.neomud.shared.model.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class ServerMessage {
    // Auth
    @Serializable
    @SerialName("register_ok")
    data object RegisterOk : ServerMessage()

    @Serializable
    @SerialName("login_ok")
    data class LoginOk(val player: Player) : ServerMessage()

    @Serializable
    @SerialName("auth_error")
    data class AuthError(val reason: String) : ServerMessage()

    // Room
    @Serializable
    @SerialName("room_info")
    data class RoomInfo(
        val room: Room,
        val players: List<String>,
        val npcs: List<Npc>
    ) : ServerMessage()

    @Serializable
    @SerialName("move_ok")
    data class MoveOk(
        val direction: Direction,
        val room: Room,
        val players: List<String>,
        val npcs: List<Npc>
    ) : ServerMessage()

    @Serializable
    @SerialName("move_error")
    data class MoveError(val reason: String) : ServerMessage()

    // Map
    @Serializable
    @SerialName("map_data")
    data class MapData(
        val rooms: List<MapRoom>,
        val playerRoomId: RoomId
    ) : ServerMessage()

    // Events
    @Serializable
    @SerialName("player_entered")
    data class PlayerEntered(val playerName: String, val roomId: RoomId) : ServerMessage()

    @Serializable
    @SerialName("player_left")
    data class PlayerLeft(val playerName: String, val roomId: RoomId, val direction: Direction) : ServerMessage()

    @Serializable
    @SerialName("npc_entered")
    data class NpcEntered(val npcName: String, val roomId: RoomId) : ServerMessage()

    @Serializable
    @SerialName("npc_left")
    data class NpcLeft(val npcName: String, val roomId: RoomId, val direction: Direction) : ServerMessage()

    @Serializable
    @SerialName("player_says")
    data class PlayerSays(val playerName: String, val message: String) : ServerMessage()

    // Effects
    @Serializable
    @SerialName("effect_tick")
    data class EffectTick(
        val effectName: String,
        val message: String,
        val newHp: Int
    ) : ServerMessage()

    // System
    @Serializable
    @SerialName("system_message")
    data class SystemMessage(val message: String) : ServerMessage()

    @Serializable
    @SerialName("pong")
    data object Pong : ServerMessage()

    @Serializable
    @SerialName("error")
    data class Error(val message: String) : ServerMessage()
}

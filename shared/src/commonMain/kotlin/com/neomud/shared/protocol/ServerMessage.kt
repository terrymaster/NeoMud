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
    data class NpcEntered(
        val npcName: String,
        val roomId: RoomId,
        val npcId: String = "",
        val hostile: Boolean = false,
        val currentHp: Int = 0,
        val maxHp: Int = 0
    ) : ServerMessage()

    @Serializable
    @SerialName("npc_left")
    data class NpcLeft(
        val npcName: String,
        val roomId: RoomId,
        val direction: Direction,
        val npcId: String = ""
    ) : ServerMessage()

    @Serializable
    @SerialName("player_says")
    data class PlayerSays(val playerName: String, val message: String) : ServerMessage()

    // Combat
    @Serializable
    @SerialName("combat_hit")
    data class CombatHit(
        val attackerName: String,
        val defenderName: String,
        val damage: Int,
        val defenderHp: Int,
        val defenderMaxHp: Int,
        val isPlayerDefender: Boolean = false
    ) : ServerMessage()

    @Serializable
    @SerialName("npc_died")
    data class NpcDied(
        val npcId: String,
        val npcName: String,
        val killerName: String,
        val roomId: RoomId
    ) : ServerMessage()

    @Serializable
    @SerialName("player_died")
    data class PlayerDied(
        val killerName: String,
        val respawnRoomId: RoomId,
        val respawnHp: Int,
        val respawnMp: Int = 0
    ) : ServerMessage()

    @Serializable
    @SerialName("attack_mode_update")
    data class AttackModeUpdate(val enabled: Boolean) : ServerMessage()

    // Effects
    @Serializable
    @SerialName("active_effects_update")
    data class ActiveEffectsUpdate(val effects: List<ActiveEffect>) : ServerMessage()

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

    // Catalog sync
    @Serializable
    @SerialName("class_catalog_sync")
    data class ClassCatalogSync(val classes: List<CharacterClassDef>) : ServerMessage()

    @Serializable
    @SerialName("item_catalog_sync")
    data class ItemCatalogSync(val items: List<Item>) : ServerMessage()

    // Inventory
    @Serializable
    @SerialName("inventory_update")
    data class InventoryUpdate(
        val inventory: List<InventoryItem>,
        val equipment: Map<String, String>,
        val coins: Coins = Coins()
    ) : ServerMessage()

    @Serializable
    @SerialName("loot_received")
    data class LootReceived(
        val npcName: String,
        val items: List<LootedItem>
    ) : ServerMessage()

    // Ground items
    @Serializable
    @SerialName("room_items_update")
    data class RoomItemsUpdate(
        val items: List<GroundItem>,
        val coins: Coins
    ) : ServerMessage()

    @Serializable
    @SerialName("loot_dropped")
    data class LootDropped(
        val npcName: String,
        val items: List<LootedItem>,
        val coins: Coins
    ) : ServerMessage()

    @Serializable
    @SerialName("pickup_result")
    data class PickupResult(
        val itemName: String,
        val quantity: Int,
        val isCoin: Boolean = false
    ) : ServerMessage()

    @Serializable
    @SerialName("item_used")
    data class ItemUsed(
        val itemName: String,
        val message: String,
        val newHp: Int
    ) : ServerMessage()

    @Serializable
    @SerialName("equip_update")
    data class EquipUpdate(
        val slot: String,
        val itemId: String?,
        val itemName: String?
    ) : ServerMessage()
}

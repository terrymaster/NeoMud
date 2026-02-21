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
        val players: List<PlayerInfo>,
        val npcs: List<Npc>
    ) : ServerMessage()

    @Serializable
    @SerialName("move_ok")
    data class MoveOk(
        val direction: Direction,
        val room: Room,
        val players: List<PlayerInfo>,
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
    data class PlayerEntered(val playerName: String, val roomId: RoomId, val playerInfo: PlayerInfo? = null) : ServerMessage()

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
        val maxHp: Int = 0,
        val spawned: Boolean = false,
        val templateId: String = ""
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
        val isPlayerDefender: Boolean = false,
        val isBackstab: Boolean = false,
        val isMiss: Boolean = false,
        val isDodge: Boolean = false,
        val isParry: Boolean = false,
        val defenderId: String = ""
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
        val newHp: Int,
        val sound: String = "",
        val newMp: Int = -1
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
        val newHp: Int,
        val newMp: Int
    ) : ServerMessage()

    @Serializable
    @SerialName("equip_update")
    data class EquipUpdate(
        val slot: String,
        val itemId: String?,
        val itemName: String?
    ) : ServerMessage()

    @Serializable
    @SerialName("stealth_update")
    data class StealthUpdate(val hidden: Boolean, val message: String = "") : ServerMessage()

    @Serializable
    @SerialName("meditate_update")
    data class MeditateUpdate(val meditating: Boolean, val message: String = "") : ServerMessage()

    @Serializable
    @SerialName("track_result")
    data class TrackResult(
        val success: Boolean,
        val direction: Direction? = null,
        val targetName: String? = null,
        val message: String
    ) : ServerMessage()

    @Serializable
    @SerialName("skill_catalog_sync")
    data class SkillCatalogSync(val skills: List<SkillDef>) : ServerMessage()

    @Serializable
    @SerialName("race_catalog_sync")
    data class RaceCatalogSync(val races: List<RaceDef>) : ServerMessage()

    @Serializable
    @SerialName("xp_gained")
    data class XpGained(val amount: Long, val currentXp: Long, val xpToNextLevel: Long) : ServerMessage()

    @Serializable
    @SerialName("level_up")
    data class LevelUp(
        val newLevel: Int,
        val hpRoll: Int,
        val newMaxHp: Int,
        val mpRoll: Int,
        val newMaxMp: Int,
        val cpGained: Int,
        val totalUnspentCp: Int,
        val xpToNextLevel: Long = 0
    ) : ServerMessage()

    @Serializable
    @SerialName("trainer_info")
    data class TrainerInfo(
        val canLevelUp: Boolean,
        val unspentCp: Int,
        val totalCpEarned: Int,
        val baseStats: Stats,
        val currentStats: Stats,
        val interactSound: String = ""
    ) : ServerMessage()

    @Serializable
    @SerialName("stat_trained")
    data class StatTrained(
        val stat: String,
        val newValue: Int,
        val cpSpent: Int,
        val remainingCp: Int
    ) : ServerMessage()

    @Serializable
    @SerialName("spell_catalog_sync")
    data class SpellCatalogSync(val spells: List<SpellDef>) : ServerMessage()

    @Serializable
    @SerialName("spell_cast_result")
    data class SpellCastResult(
        val success: Boolean,
        val spellName: String,
        val message: String,
        val newMp: Int,
        val newHp: Int? = null
    ) : ServerMessage()

    @Serializable
    @SerialName("spell_effect")
    data class SpellEffect(
        val casterName: String,
        val targetName: String,
        val spellName: String,
        val effectAmount: Int,
        val targetNewHp: Int,
        val targetMaxHp: Int,
        val isPlayerTarget: Boolean = false,
        val targetId: String = ""
    ) : ServerMessage()

    @Serializable
    @SerialName("vendor_info")
    data class VendorInfo(
        val vendorName: String,
        val items: List<VendorItem>,
        val playerCoins: Coins,
        val playerInventory: List<InventoryItem>,
        val playerCharm: Int = 0,
        val interactSound: String = "",
        val hasHaggle: Boolean = false
    ) : ServerMessage()

    @Serializable
    @SerialName("buy_result")
    data class BuyResult(
        val success: Boolean,
        val message: String,
        val updatedCoins: Coins,
        val updatedInventory: List<InventoryItem>,
        val equipment: Map<String, String>
    ) : ServerMessage()

    @Serializable
    @SerialName("sell_result")
    data class SellResult(
        val success: Boolean,
        val message: String,
        val updatedCoins: Coins,
        val updatedInventory: List<InventoryItem>,
        val equipment: Map<String, String>
    ) : ServerMessage()

    @Serializable
    @SerialName("interact_result")
    data class InteractResult(
        val success: Boolean,
        val featureName: String,
        val message: String,
        val sound: String = ""
    ) : ServerMessage()
}

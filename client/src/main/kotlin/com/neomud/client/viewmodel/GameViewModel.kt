package com.neomud.client.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neomud.client.network.WebSocketClient
import com.neomud.client.ui.theme.MudColors
import com.neomud.shared.model.*
import com.neomud.shared.protocol.ClientMessage
import com.neomud.shared.protocol.ServerMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class GameViewModel(private val wsClient: WebSocketClient, var serverBaseUrl: String = "") : ViewModel() {

    private val _roomInfo = MutableStateFlow<ServerMessage.RoomInfo?>(null)
    val roomInfo: StateFlow<ServerMessage.RoomInfo?> = _roomInfo

    private val _mapData = MutableStateFlow<ServerMessage.MapData?>(null)
    val mapData: StateFlow<ServerMessage.MapData?> = _mapData

    private val _gameLog = MutableStateFlow<List<LogEntry>>(emptyList())
    val gameLog: StateFlow<List<LogEntry>> = _gameLog

    private val _player = MutableStateFlow<Player?>(null)
    val player: StateFlow<Player?> = _player

    private val _roomEntities = MutableStateFlow<List<Npc>>(emptyList())
    val roomEntities: StateFlow<List<Npc>> = _roomEntities

    private val _attackMode = MutableStateFlow(false)
    val attackMode: StateFlow<Boolean> = _attackMode

    private val _selectedTargetId = MutableStateFlow<String?>(null)
    val selectedTargetId: StateFlow<String?> = _selectedTargetId

    // Inventory
    private val _inventory = MutableStateFlow<List<InventoryItem>>(emptyList())
    val inventory: StateFlow<List<InventoryItem>> = _inventory

    private val _equipment = MutableStateFlow<Map<String, String>>(emptyMap())
    val equipment: StateFlow<Map<String, String>> = _equipment

    private val _itemCatalog = MutableStateFlow<Map<String, Item>>(emptyMap())
    val itemCatalog: StateFlow<Map<String, Item>> = _itemCatalog

    private val _showInventory = MutableStateFlow(false)
    val showInventory: StateFlow<Boolean> = _showInventory

    // Coins & ground items
    private val _playerCoins = MutableStateFlow(Coins())
    val playerCoins: StateFlow<Coins> = _playerCoins

    private val _roomGroundItems = MutableStateFlow<List<GroundItem>>(emptyList())
    val roomGroundItems: StateFlow<List<GroundItem>> = _roomGroundItems

    private val _roomGroundCoins = MutableStateFlow(Coins())
    val roomGroundCoins: StateFlow<Coins> = _roomGroundCoins

    // Active effects
    private val _activeEffects = MutableStateFlow<List<ActiveEffect>>(emptyList())
    val activeEffects: StateFlow<List<ActiveEffect>> = _activeEffects

    // Character sheet
    private val _showCharacterSheet = MutableStateFlow(false)
    val showCharacterSheet: StateFlow<Boolean> = _showCharacterSheet

    // Death notification
    private val _deathMessage = MutableStateFlow<String?>(null)
    val deathMessage: StateFlow<String?> = _deathMessage

    fun dismissDeath() {
        _deathMessage.value = null
    }

    // Settings
    private val _showSettings = MutableStateFlow(false)
    val showSettings: StateFlow<Boolean> = _showSettings

    private val _classCatalog = MutableStateFlow<Map<String, CharacterClassDef>>(emptyMap())
    val classCatalog: StateFlow<Map<String, CharacterClassDef>> = _classCatalog

    fun setInitialPlayer(player: Player) {
        _player.value = player
    }

    fun startCollecting() {
        viewModelScope.launch {
            wsClient.messages.collect { message ->
                handleMessage(message)
            }
        }
        look()
    }

    private fun handleMessage(message: ServerMessage) {
        when (message) {
            is ServerMessage.RoomInfo -> {
                _roomInfo.value = message
                _roomEntities.value = message.npcs
                logRoomInfo(message.room, message.players, message.npcs)
            }
            is ServerMessage.MoveOk -> {
                _roomInfo.value = ServerMessage.RoomInfo(message.room, message.players, message.npcs)
                _roomEntities.value = message.npcs
                // Clear ground items on move (will be refreshed by RoomItemsUpdate)
                _roomGroundItems.value = emptyList()
                _roomGroundCoins.value = Coins()
                addLog("You move ${message.direction.name.lowercase()}.", MudColors.selfAction)
                logRoomInfo(message.room, message.players, message.npcs)
            }
            is ServerMessage.MoveError -> addLog("Cannot move: ${message.reason}", MudColors.error)
            is ServerMessage.MapData -> _mapData.value = message
            is ServerMessage.PlayerEntered -> addLog("${message.playerName} has arrived.", MudColors.playerEvent)
            is ServerMessage.PlayerLeft -> addLog("${message.playerName} left ${message.direction.name.lowercase()}.", MudColors.playerEvent)
            is ServerMessage.NpcEntered -> {
                val color = if (message.hostile) MudColors.hostile else MudColors.friendly
                addLog("${message.npcName} has arrived.", color)
                if (message.npcId.isNotEmpty()) {
                    val current = _roomEntities.value.toMutableList()
                    current.add(Npc(
                        id = message.npcId,
                        name = message.npcName,
                        description = "",
                        currentRoomId = message.roomId,
                        behaviorType = "unknown",
                        hostile = message.hostile,
                        currentHp = message.currentHp,
                        maxHp = message.maxHp
                    ))
                    _roomEntities.value = current
                }
            }
            is ServerMessage.NpcLeft -> {
                val npc = _roomEntities.value.find { it.id == message.npcId }
                val color = if (npc?.hostile == true) MudColors.hostile else MudColors.friendly
                addLog("${message.npcName} left ${message.direction.name.lowercase()}.", color)
                if (message.npcId.isNotEmpty()) {
                    _roomEntities.value = _roomEntities.value.filter { it.id != message.npcId }
                }
            }
            is ServerMessage.CombatHit -> {
                if (message.damage > 0) {
                    val color = if (message.isPlayerDefender) MudColors.combatEnemy else MudColors.combatYou
                    addLog("${message.attackerName} hits ${message.defenderName} for ${message.damage} damage! (${message.defenderHp}/${message.defenderMaxHp} HP)", color)
                }
                if (message.isPlayerDefender) {
                    _player.value = _player.value?.copy(currentHp = message.defenderHp)
                } else {
                    _roomEntities.value = _roomEntities.value.map { npc ->
                        if (npc.name == message.defenderName) {
                            npc.copy(currentHp = message.defenderHp)
                        } else npc
                    }
                }
            }
            is ServerMessage.NpcDied -> {
                addLog("${message.npcName} has been slain by ${message.killerName}!", MudColors.kill)
                _roomEntities.value = _roomEntities.value.filter { it.id != message.npcId }
                if (_selectedTargetId.value == message.npcId) {
                    _selectedTargetId.value = null
                }
            }
            is ServerMessage.PlayerDied -> {
                addLog("You have been slain by ${message.killerName}! Respawning...", MudColors.death)
                _deathMessage.value = "Slain by ${message.killerName}"
                _player.value = _player.value?.copy(
                    currentHp = message.respawnHp,
                    currentMp = message.respawnMp,
                    currentRoomId = message.respawnRoomId
                )
                _attackMode.value = false
                _selectedTargetId.value = null
                _activeEffects.value = emptyList()
                _roomGroundItems.value = emptyList()
                _roomGroundCoins.value = Coins()
            }
            is ServerMessage.AttackModeUpdate -> {
                _attackMode.value = message.enabled
                if (!message.enabled) {
                    addLog("Attack mode disabled.", MudColors.system)
                } else {
                    addLog("Attack mode enabled!", MudColors.combatYou)
                }
            }
            is ServerMessage.PlayerSays -> {
                addEntry(LogEntry(listOf(
                    LogSpan("${message.playerName}", MudColors.playerName),
                    LogSpan(" says: \"${message.message}\"", MudColors.say)
                )))
            }
            is ServerMessage.ActiveEffectsUpdate -> {
                _activeEffects.value = message.effects
            }
            is ServerMessage.EffectTick -> addLog(message.message, MudColors.effect)
            is ServerMessage.SystemMessage -> addLog("[System] ${message.message}", MudColors.system)
            is ServerMessage.Error -> addLog("[Error] ${message.message}", MudColors.error)
            is ServerMessage.LoginOk -> _player.value = message.player
            is ServerMessage.Pong -> { /* ignore */ }
            is ServerMessage.RegisterOk -> { /* handled by AuthViewModel */ }
            is ServerMessage.AuthError -> { /* handled by AuthViewModel */ }
            is ServerMessage.ClassCatalogSync -> {
                _classCatalog.value = message.classes.associateBy { it.id }
            }
            is ServerMessage.ItemCatalogSync -> {
                _itemCatalog.value = message.items.associateBy { it.id }
            }
            is ServerMessage.InventoryUpdate -> {
                _inventory.value = message.inventory
                _equipment.value = message.equipment
                _playerCoins.value = message.coins
            }
            is ServerMessage.LootReceived -> {
                for (lootedItem in message.items) {
                    val qtyStr = if (lootedItem.quantity > 1) " x${lootedItem.quantity}" else ""
                    addLog("You loot ${lootedItem.itemName}$qtyStr from ${message.npcName}.", MudColors.loot)
                }
            }
            is ServerMessage.ItemUsed -> {
                addLog(message.message, MudColors.effect)
                _player.value = _player.value?.copy(currentHp = message.newHp)
            }
            is ServerMessage.EquipUpdate -> {
                if (message.itemName != null) {
                    addLog("You equip ${message.itemName}.", MudColors.selfAction)
                } else {
                    addLog("You unequip from ${message.slot}.", MudColors.selfAction)
                }
            }
            is ServerMessage.RoomItemsUpdate -> {
                _roomGroundItems.value = message.items
                _roomGroundCoins.value = message.coins
            }
            is ServerMessage.LootDropped -> {
                val catalog = _itemCatalog.value
                for (item in message.items) {
                    val name = catalog[item.itemId]?.name ?: item.itemName
                    val qtyStr = if (item.quantity > 1) " x${item.quantity}" else ""
                    addLog("${message.npcName} dropped $name$qtyStr.", MudColors.loot)
                }
                if (!message.coins.isEmpty()) {
                    val parts = mutableListOf<String>()
                    if (message.coins.platinum > 0) parts.add("${message.coins.platinum} platinum")
                    if (message.coins.gold > 0) parts.add("${message.coins.gold} gold")
                    if (message.coins.silver > 0) parts.add("${message.coins.silver} silver")
                    if (message.coins.copper > 0) parts.add("${message.coins.copper} copper")
                    addLog("${message.npcName} dropped ${parts.joinToString(", ")}.", MudColors.loot)
                }
            }
            is ServerMessage.PickupResult -> {
                if (message.isCoin) {
                    addLog("You pick up ${message.quantity} ${message.itemName}.", MudColors.loot)
                } else {
                    val qtyStr = if (message.quantity > 1) " x${message.quantity}" else ""
                    addLog("You pick up ${message.itemName}$qtyStr.", MudColors.loot)
                }
            }
        }
    }

    private fun logRoomInfo(room: Room, players: List<String>, npcs: List<Npc>) {
        addLog("--- ${room.name} ---", MudColors.roomName)
        addLog(room.description, MudColors.roomDesc)
        if (players.isNotEmpty()) {
            val spans = mutableListOf(LogSpan("Players here: ", MudColors.default))
            players.forEachIndexed { i, name ->
                if (i > 0) spans.add(LogSpan(", ", MudColors.default))
                spans.add(LogSpan(name, MudColors.playerName))
            }
            addEntry(LogEntry(spans))
        }
        if (npcs.isNotEmpty()) {
            val spans = mutableListOf(LogSpan("You see: ", MudColors.default))
            npcs.forEachIndexed { i, npc ->
                if (i > 0) spans.add(LogSpan(", ", MudColors.default))
                spans.add(LogSpan(npc.name, if (npc.hostile) MudColors.hostile else MudColors.friendly))
            }
            addEntry(LogEntry(spans))
        }
        val exits = room.exits.keys.joinToString(", ") { it.name.lowercase() }
        addLog("Exits: $exits", MudColors.exits)
    }

    private fun addLog(text: String, color: androidx.compose.ui.graphics.Color) {
        addEntry(LogEntry(text, color))
    }

    private fun addEntry(entry: LogEntry) {
        val log = _gameLog.value + entry
        _gameLog.value = if (log.size > 200) log.takeLast(200) else log
    }

    fun move(direction: Direction) {
        viewModelScope.launch {
            wsClient.send(ClientMessage.Move(direction))
        }
    }

    fun look() {
        viewModelScope.launch {
            wsClient.send(ClientMessage.Look)
        }
    }

    fun say(message: String) {
        viewModelScope.launch {
            wsClient.send(ClientMessage.Say(message))
        }
    }

    fun toggleAttackMode(enabled: Boolean) {
        viewModelScope.launch {
            wsClient.send(ClientMessage.AttackToggle(enabled))
        }
    }

    fun selectTarget(npcId: String?) {
        _selectedTargetId.value = npcId
        viewModelScope.launch {
            wsClient.send(ClientMessage.SelectTarget(npcId))
        }
    }

    fun toggleInventory() {
        _showInventory.value = !_showInventory.value
        if (_showInventory.value) {
            viewInventory()
        }
    }

    fun viewInventory() {
        viewModelScope.launch {
            wsClient.send(ClientMessage.ViewInventory)
        }
    }

    fun equipItem(itemId: String, slot: String) {
        viewModelScope.launch {
            wsClient.send(ClientMessage.EquipItem(itemId, slot))
        }
    }

    fun unequipItem(slot: String) {
        viewModelScope.launch {
            wsClient.send(ClientMessage.UnequipItem(slot))
        }
    }

    fun useItem(itemId: String) {
        viewModelScope.launch {
            wsClient.send(ClientMessage.UseItem(itemId))
        }
    }

    fun pickupItem(itemId: String, quantity: Int) {
        viewModelScope.launch {
            wsClient.send(ClientMessage.PickupItem(itemId, quantity))
        }
    }

    fun pickupCoins(coinType: String) {
        viewModelScope.launch {
            wsClient.send(ClientMessage.PickupCoins(coinType))
        }
    }

    fun toggleCharacterSheet() {
        _showCharacterSheet.value = !_showCharacterSheet.value
    }

    fun toggleSettings() {
        _showSettings.value = !_showSettings.value
    }
}

package com.neomud.client.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neomud.client.audio.AudioManager
import com.neomud.client.network.WebSocketClient
import com.neomud.client.ui.theme.MudColors
import com.neomud.shared.model.*
import com.neomud.shared.protocol.ClientMessage
import com.neomud.shared.protocol.ServerMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class GameViewModel(
    private val wsClient: WebSocketClient,
    var serverBaseUrl: String = "",
    private val audioManager: AudioManager? = null
) : ViewModel() {

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

    private val _roomPlayers = MutableStateFlow<List<PlayerInfo>>(emptyList())
    val roomPlayers: StateFlow<List<PlayerInfo>> = _roomPlayers

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

    private val _showEquipment = MutableStateFlow(false)
    val showEquipment: StateFlow<Boolean> = _showEquipment

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

    // Stealth
    private val _isHidden = MutableStateFlow(false)
    val isHidden: StateFlow<Boolean> = _isHidden

    // Meditation
    private val _isMeditating = MutableStateFlow(false)
    val isMeditating: StateFlow<Boolean> = _isMeditating

    // Skill catalog
    private val _skillCatalog = MutableStateFlow<Map<String, SkillDef>>(emptyMap())
    val skillCatalog: StateFlow<Map<String, SkillDef>> = _skillCatalog

    // Character sheet
    private val _showCharacterSheet = MutableStateFlow(false)
    val showCharacterSheet: StateFlow<Boolean> = _showCharacterSheet

    // Trainer
    private val _trainerInfo = MutableStateFlow<ServerMessage.TrainerInfo?>(null)
    val trainerInfo: StateFlow<ServerMessage.TrainerInfo?> = _trainerInfo

    private val _showTrainer = MutableStateFlow(false)
    val showTrainer: StateFlow<Boolean> = _showTrainer

    // Spells
    private val _spellCatalog = MutableStateFlow<Map<String, SpellDef>>(emptyMap())
    val spellCatalog: StateFlow<Map<String, SpellDef>> = _spellCatalog

    private val _spellSlots = MutableStateFlow<List<String?>>(listOf(null, null, null, null))
    val spellSlots: StateFlow<List<String?>> = _spellSlots

    private val _readiedSpellId = MutableStateFlow<String?>(null)
    val readiedSpellId: StateFlow<String?> = _readiedSpellId

    private val _showSpellPicker = MutableStateFlow(false)
    val showSpellPicker: StateFlow<Boolean> = _showSpellPicker

    private val _editingSlotIndex = MutableStateFlow<Int?>(null)
    val editingSlotIndex: StateFlow<Int?> = _editingSlotIndex

    // Vendor
    private val _vendorInfo = MutableStateFlow<ServerMessage.VendorInfo?>(null)
    val vendorInfo: StateFlow<ServerMessage.VendorInfo?> = _vendorInfo

    private val _showVendor = MutableStateFlow(false)
    val showVendor: StateFlow<Boolean> = _showVendor

    // Tracked direction (from TRACK skill)
    private val _trackedDirection = MutableStateFlow<Direction?>(null)
    val trackedDirection: StateFlow<Direction?> = _trackedDirection

    // Lock target picker
    private val _showLockTargetPicker = MutableStateFlow(false)
    val showLockTargetPicker: StateFlow<Boolean> = _showLockTargetPicker

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

    private fun sfx(soundId: String) {
        if (soundId.isNotBlank()) audioManager?.playSfx(serverBaseUrl, soundId)
    }

    private fun bgm(trackId: String) {
        audioManager?.playBgm(serverBaseUrl, trackId)
    }

    private fun findSpellByName(name: String): SpellDef? =
        _spellCatalog.value.values.find { it.name == name }

    private fun findItemByName(name: String): Item? =
        _itemCatalog.value.values.find { it.name == name }

    fun setInitialPlayer(player: Player) {
        _player.value = player
    }

    fun setInitialRoomInfo(roomInfo: ServerMessage.RoomInfo) {
        _roomInfo.value = roomInfo
        _roomEntities.value = roomInfo.npcs
        _roomPlayers.value = roomInfo.players
        bgm(roomInfo.room.bgm)
    }

    fun setInitialCatalogs(
        classes: List<CharacterClassDef> = emptyList(),
        items: List<Item> = emptyList(),
        spells: List<SpellDef> = emptyList()
    ) {
        if (classes.isNotEmpty()) _classCatalog.value = classes.associateBy { it.id }
        if (items.isNotEmpty()) _itemCatalog.value = items.associateBy { it.id }
        if (spells.isNotEmpty()) _spellCatalog.value = spells.associateBy { it.id }
        autoAssignSpells()
    }

    fun startCollecting() {
        viewModelScope.launch {
            wsClient.messages.collect { message ->
                try {
                    handleMessage(message)
                } catch (e: Exception) {
                    android.util.Log.e("GameViewModel", "handleMessage crashed for ${message::class.simpleName}", e)
                }
            }
        }
        look()
    }

    private fun handleMessage(message: ServerMessage) {
        when (message) {
            is ServerMessage.RoomInfo -> {
                _roomInfo.value = message
                _roomEntities.value = message.npcs
                _roomPlayers.value = message.players
                logRoomInfo(message.room, message.players, message.npcs)
                bgm(message.room.bgm)
            }
            is ServerMessage.MoveOk -> {
                // Play departure sound from the room we're leaving
                val previousRoom = _roomInfo.value?.room
                if (previousRoom != null) sfx(previousRoom.departSound)
                _roomInfo.value = ServerMessage.RoomInfo(message.room, message.players, message.npcs)
                _roomEntities.value = message.npcs
                _roomPlayers.value = message.players
                // Clear ground items on move (will be refreshed by RoomItemsUpdate)
                _roomGroundItems.value = emptyList()
                _roomGroundCoins.value = Coins()
                _trackedDirection.value = null
                addLog("You move ${message.direction.name.lowercase()}.", MudColors.selfAction)
                logRoomInfo(message.room, message.players, message.npcs)
                bgm(message.room.bgm)
            }
            is ServerMessage.MoveError -> addLog("Cannot move: ${message.reason}", MudColors.error)
            is ServerMessage.MapData -> _mapData.value = message
            is ServerMessage.PlayerEntered -> {
                addLog("${message.playerName} has arrived.", MudColors.playerEvent)
                val info = message.playerInfo
                if (info != null) {
                    _roomPlayers.value = _roomPlayers.value + info
                }
            }
            is ServerMessage.PlayerLeft -> {
                addLog("${message.playerName} left ${message.direction.name.lowercase()}.", MudColors.playerEvent)
                _roomPlayers.value = _roomPlayers.value.filter { it.name != message.playerName }
            }
            is ServerMessage.NpcEntered -> {
                val color = if (message.hostile) MudColors.hostile else MudColors.friendly
                val text = if (message.spawned) {
                    "${message.npcName} emerges from the shadows."
                } else {
                    "${message.npcName} has arrived."
                }
                addLog(text, color)
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
                when {
                    message.isMiss -> {
                        addLog("${message.attackerName} misses ${message.defenderName}!", MudColors.combatYou)
                        if (!message.isPlayerDefender) {
                            // Player missed NPC — play weapon miss sound
                            val weaponId = _equipment.value["weapon"]
                            val missSound = weaponId?.let { _itemCatalog.value[it]?.missSound } ?: ""
                            sfx(missSound.ifEmpty { "miss" })
                        } else {
                            // NPC missed player — play NPC miss sound
                            val npc = _roomEntities.value.find { it.name == message.attackerName }
                            sfx(npc?.missSound?.ifEmpty { "miss" } ?: "miss")
                        }
                    }
                    message.isDodge -> {
                        addLog("${message.defenderName} dodges ${message.attackerName}'s attack!", MudColors.combatYou)
                        sfx("dodge")
                    }
                    message.isParry && message.damage > 0 -> {
                        addLog("${message.defenderName} parries ${message.attackerName}'s blow for reduced damage! (${message.damage} damage, ${message.defenderHp}/${message.defenderMaxHp} HP)", MudColors.combatYou)
                        sfx("parry")
                    }
                    message.damage > 0 -> {
                        val verb = if (message.isBackstab) "backstabs" else "hits"
                        val color = if (message.isPlayerDefender) MudColors.combatEnemy
                            else if (message.isBackstab) MudColors.stealth else MudColors.combatYou
                        addLog("${message.attackerName} $verb ${message.defenderName} for ${message.damage} damage! (${message.defenderHp}/${message.defenderMaxHp} HP)", color)
                        if (message.isBackstab) {
                            sfx("backstab")
                        } else if (!message.isPlayerDefender) {
                            val weaponId = _equipment.value["weapon"]
                            val weaponSound = weaponId?.let { _itemCatalog.value[it]?.attackSound } ?: ""
                            sfx(weaponSound)
                        } else {
                            val npc = _roomEntities.value.find { it.name == message.attackerName }
                            sfx(npc?.attackSound ?: "")
                        }
                    }
                }
                if (message.isPlayerDefender) {
                    _player.value = _player.value?.copy(currentHp = message.defenderHp)
                } else {
                    _roomEntities.value = _roomEntities.value.map { npc ->
                        if (npc.id == message.defenderId) {
                            npc.copy(currentHp = message.defenderHp)
                        } else npc
                    }
                }
            }
            is ServerMessage.NpcDied -> {
                val dyingNpc = _roomEntities.value.find { it.id == message.npcId }
                sfx(dyingNpc?.deathSound ?: "enemy_death")
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
                _isHidden.value = false
                _isMeditating.value = false
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
            is ServerMessage.EffectTick -> {
                sfx(message.sound)
                addLog(message.message, MudColors.effect)
                _player.value = _player.value?.let { p ->
                    val newMp = if (message.newMp >= 0) message.newMp else p.currentMp
                    p.copy(currentHp = message.newHp, currentMp = newMp)
                }
            }
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
                val usedItem = findItemByName(message.itemName)
                sfx(usedItem?.useSound ?: "potion_drink")
                addLog(message.message, MudColors.effect)
                _player.value = _player.value?.copy(
                    currentHp = message.newHp,
                    currentMp = message.newMp
                )
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
                sfx("loot_drop")
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
                sfx(if (message.isCoin) "coin_pickup" else "item_pickup")
                if (message.isCoin) {
                    addLog("You pick up ${message.quantity} ${message.itemName}.", MudColors.loot)
                } else {
                    val qtyStr = if (message.quantity > 1) " x${message.quantity}" else ""
                    addLog("You pick up ${message.itemName}$qtyStr.", MudColors.loot)
                }
            }
            is ServerMessage.StealthUpdate -> {
                _isHidden.value = message.hidden
                if (message.message.isNotEmpty()) {
                    addLog(message.message, MudColors.stealth)
                }
                if (message.hidden) {
                    _attackMode.value = false
                }
            }
            is ServerMessage.MeditateUpdate -> {
                _isMeditating.value = message.meditating
                if (message.message.isNotEmpty()) {
                    addLog(message.message, MudColors.spell)
                }
            }
            is ServerMessage.TrackResult -> {
                if (message.success && message.direction != null) {
                    _trackedDirection.value = message.direction
                }
                addLog(message.message, MudColors.system)
            }
            is ServerMessage.SkillCatalogSync -> {
                _skillCatalog.value = message.skills.associateBy { it.id }
            }
            is ServerMessage.RaceCatalogSync -> { /* handled by AuthViewModel */ }
            is ServerMessage.SpellCatalogSync -> {
                _spellCatalog.value = message.spells.associateBy { it.id }
                autoAssignSpells()
            }
            is ServerMessage.SpellCastResult -> {
                val color = if (message.success) MudColors.spell else MudColors.error
                addLog(message.message, color)
                if (message.success) {
                    val spell = findSpellByName(message.spellName)
                    sfx(spell?.castSound ?: "")
                    _player.value = _player.value?.let { p ->
                        val newHp = message.newHp ?: p.currentHp
                        p.copy(currentMp = message.newMp, currentHp = newHp)
                    }
                }
                _readiedSpellId.value = null
            }
            is ServerMessage.SpellEffect -> {
                val impactSpell = findSpellByName(message.spellName)
                sfx(impactSpell?.impactSound ?: "")
                if (!message.isPlayerTarget) {
                    _roomEntities.value = _roomEntities.value.map { npc ->
                        if (message.targetId.isNotEmpty() && npc.id == message.targetId ||
                            message.targetId.isEmpty() && npc.name == message.targetName) {
                            npc.copy(currentHp = message.targetNewHp)
                        } else npc
                    }
                }
            }
            is ServerMessage.VendorInfo -> {
                sfx(message.interactSound)
                _vendorInfo.value = message
                _showVendor.value = true
            }
            is ServerMessage.BuyResult -> {
                if (message.success) {
                    addLog(message.message, MudColors.loot)
                } else {
                    addLog(message.message, MudColors.error)
                }
                _playerCoins.value = message.updatedCoins
                _inventory.value = message.updatedInventory
                _equipment.value = message.equipment
                // Refresh vendor info if panel is open
                if (_showVendor.value) interactVendor()
            }
            is ServerMessage.SellResult -> {
                if (message.success) {
                    addLog(message.message, MudColors.loot)
                } else {
                    addLog(message.message, MudColors.error)
                }
                _playerCoins.value = message.updatedCoins
                _inventory.value = message.updatedInventory
                _equipment.value = message.equipment
                if (_showVendor.value) interactVendor()
            }
            is ServerMessage.XpGained -> {
                addLog("+${message.amount} XP (${message.currentXp}/${message.xpToNextLevel})", MudColors.xp)
                _player.value = _player.value?.copy(
                    currentXp = message.currentXp,
                    xpToNextLevel = message.xpToNextLevel
                )
            }
            is ServerMessage.LevelUp -> {
                addLog("LEVEL UP! You are now level ${message.newLevel}! HP+${message.hpRoll} MP+${message.mpRoll} CP+${message.cpGained}", MudColors.levelUp)
                _player.value = _player.value?.copy(
                    level = message.newLevel,
                    maxHp = message.newMaxHp,
                    currentHp = message.newMaxHp,
                    maxMp = message.newMaxMp,
                    currentMp = message.newMaxMp,
                    unspentCp = message.totalUnspentCp,
                    xpToNextLevel = message.xpToNextLevel
                )
                // Refresh trainer info if trainer panel is open
                if (_showTrainer.value) interactTrainer()
            }
            is ServerMessage.TrainerInfo -> {
                sfx(message.interactSound)
                _trainerInfo.value = message
                _showTrainer.value = true
                _player.value = _player.value?.copy(
                    stats = message.currentStats,
                    unspentCp = message.unspentCp
                )
            }
            is ServerMessage.StatTrained -> {
                addLog("Trained ${message.stat} to ${message.newValue} (${message.cpSpent} CP spent, ${message.remainingCp} remaining)", MudColors.system)
                _player.value = _player.value?.let { p ->
                    val newStats = when (message.stat.lowercase()) {
                        "strength" -> p.stats.copy(strength = message.newValue)
                        "agility" -> p.stats.copy(agility = message.newValue)
                        "intellect" -> p.stats.copy(intellect = message.newValue)
                        "willpower" -> p.stats.copy(willpower = message.newValue)
                        "health" -> p.stats.copy(health = message.newValue)
                        "charm" -> p.stats.copy(charm = message.newValue)
                        else -> p.stats
                    }
                    p.copy(stats = newStats, unspentCp = message.remainingCp)
                }
                // Refresh trainer info after training
                interactTrainer()
            }
        }
    }

    private fun logRoomInfo(room: Room, players: List<PlayerInfo>, npcs: List<Npc>) {
        addLog("--- ${room.name} ---", MudColors.roomName)
        addLog(room.description, MudColors.roomDesc)
        if (players.isNotEmpty()) {
            val spans = mutableListOf(LogSpan("Players here: ", MudColors.default))
            players.forEachIndexed { i, info ->
                if (i > 0) spans.add(LogSpan(", ", MudColors.default))
                spans.add(LogSpan(info.name, MudColors.playerName))
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

    fun attackTarget(npcId: String) {
        selectTarget(npcId)
        toggleAttackMode(true)
    }

    fun toggleInventory() {
        _showInventory.value = !_showInventory.value
        if (_showInventory.value) {
            _showEquipment.value = false
            viewInventory()
        }
    }

    fun toggleEquipment() {
        _showEquipment.value = !_showEquipment.value
        if (_showEquipment.value) {
            _showInventory.value = false
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

    fun toggleSneakMode(enabled: Boolean) {
        viewModelScope.launch {
            wsClient.send(ClientMessage.SneakToggle(enabled))
        }
    }

    fun useSkill(skillId: String, targetId: String? = null) {
        viewModelScope.launch {
            wsClient.send(ClientMessage.UseSkill(skillId, targetId))
        }
    }

    fun toggleCharacterSheet() {
        _showCharacterSheet.value = !_showCharacterSheet.value
    }

    fun toggleSettings() {
        _showSettings.value = !_showSettings.value
    }

    fun interactTrainer() {
        viewModelScope.launch {
            wsClient.send(ClientMessage.InteractTrainer)
        }
    }

    fun trainLevelUp() {
        viewModelScope.launch {
            wsClient.send(ClientMessage.TrainLevelUp)
        }
    }

    fun trainStat(stat: String, points: Int = 1) {
        viewModelScope.launch {
            wsClient.send(ClientMessage.TrainStat(stat, points))
        }
    }

    fun allocateTrainedStats(stats: com.neomud.shared.model.Stats) {
        viewModelScope.launch {
            wsClient.send(ClientMessage.AllocateTrainedStats(stats))
        }
    }

    fun dismissTrainer() {
        _showTrainer.value = false
        _trainerInfo.value = null
    }

    fun interactVendor() {
        viewModelScope.launch {
            wsClient.send(ClientMessage.InteractVendor)
        }
    }

    fun buyItem(itemId: String, quantity: Int = 1) {
        viewModelScope.launch {
            wsClient.send(ClientMessage.BuyItem(itemId, quantity))
        }
    }

    fun sellItem(itemId: String, quantity: Int = 1) {
        viewModelScope.launch {
            wsClient.send(ClientMessage.SellItem(itemId, quantity))
        }
    }

    fun dismissVendor() {
        _showVendor.value = false
        _vendorInfo.value = null
    }

    // Lock target picker methods
    fun showLockTargetPicker() {
        val lockedExits = _roomInfo.value?.room?.lockedExits ?: emptyMap()
        when (lockedExits.size) {
            0 -> useSkill("PICK_LOCK") // Let server handle "nothing locked" message
            1 -> {
                val dir = lockedExits.keys.first()
                useSkill("PICK_LOCK", "exit:${dir.name}")
            }
            else -> _showLockTargetPicker.value = true
        }
    }

    fun dismissLockTargetPicker() {
        _showLockTargetPicker.value = false
    }

    fun pickLockTarget(targetId: String) {
        _showLockTargetPicker.value = false
        useSkill("PICK_LOCK", targetId)
    }

    // Spell methods
    fun castSpell(spellId: String, targetId: String? = null) {
        viewModelScope.launch {
            wsClient.send(ClientMessage.CastSpell(spellId, targetId))
        }
    }

    fun readySpell(slotIndex: Int) {
        val spellId = _spellSlots.value.getOrNull(slotIndex) ?: return
        val spell = _spellCatalog.value[spellId] ?: return

        if (spell.targetType == TargetType.SELF) {
            // Self spells cast immediately
            castSpell(spellId)
        } else {
            // Enemy spells enter readied state
            _readiedSpellId.value = if (_readiedSpellId.value == spellId) null else spellId
        }
    }

    fun assignSpellToSlot(index: Int, spellId: String?) {
        val slots = _spellSlots.value.toMutableList()
        slots[index] = spellId
        _spellSlots.value = slots
        _showSpellPicker.value = false
        _editingSlotIndex.value = null
    }

    fun openSpellPicker(slotIndex: Int) {
        _editingSlotIndex.value = slotIndex
        _showSpellPicker.value = true
    }

    fun dismissSpellPicker() {
        _showSpellPicker.value = false
        _editingSlotIndex.value = null
    }

    private fun autoAssignSpells() {
        val player = _player.value ?: return
        val classDef = _classCatalog.value[player.characterClass] ?: return
        if (classDef.magicSchools.isEmpty()) return

        val catalog = _spellCatalog.value
        val classSchools = classDef.magicSchools.keys
        val classSpells = catalog.values
            .filter { it.school in classSchools && player.level >= it.levelRequired }
            .sortedBy { it.levelRequired }

        val slots = _spellSlots.value.toMutableList()
        var spellIdx = 0
        for (i in slots.indices) {
            if (slots[i] == null && spellIdx < classSpells.size) {
                slots[i] = classSpells[spellIdx].id
                spellIdx++
            }
        }
        _spellSlots.value = slots
    }
}

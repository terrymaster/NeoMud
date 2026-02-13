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

class GameViewModel(private val wsClient: WebSocketClient) : ViewModel() {

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
                _player.value = _player.value?.copy(
                    currentHp = message.respawnHp,
                    currentRoomId = message.respawnRoomId
                )
                _attackMode.value = false
                _selectedTargetId.value = null
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
            is ServerMessage.EffectTick -> addLog(message.message, MudColors.effect)
            is ServerMessage.SystemMessage -> addLog("[System] ${message.message}", MudColors.system)
            is ServerMessage.Error -> addLog("[Error] ${message.message}", MudColors.error)
            is ServerMessage.LoginOk -> _player.value = message.player
            is ServerMessage.Pong -> { /* ignore */ }
            is ServerMessage.RegisterOk -> { /* handled by AuthViewModel */ }
            is ServerMessage.AuthError -> { /* handled by AuthViewModel */ }
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
}

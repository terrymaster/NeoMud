package com.neomud.client.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neomud.client.network.WebSocketClient
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

    private val _gameLog = MutableStateFlow<List<String>>(emptyList())
    val gameLog: StateFlow<List<String>> = _gameLog

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
        // Request initial room info since LoginOk/RoomInfo/MapData may have
        // been emitted before this collector started
        look()
    }

    private fun handleMessage(message: ServerMessage) {
        when (message) {
            is ServerMessage.RoomInfo -> {
                _roomInfo.value = message
                _roomEntities.value = message.npcs
                addLog("--- ${message.room.name} ---")
                addLog(message.room.description)
                if (message.players.isNotEmpty()) {
                    addLog("Players here: ${message.players.joinToString(", ")}")
                }
                if (message.npcs.isNotEmpty()) {
                    addLog("You see: ${message.npcs.joinToString(", ") { it.name }}")
                }
                val exits = message.room.exits.keys.joinToString(", ") { it.name.lowercase() }
                addLog("Exits: $exits")
            }
            is ServerMessage.MoveOk -> {
                _roomInfo.value = ServerMessage.RoomInfo(message.room, message.players, message.npcs)
                _roomEntities.value = message.npcs
                addLog("You move ${message.direction.name.lowercase()}.")
                addLog("--- ${message.room.name} ---")
                addLog(message.room.description)
                if (message.players.isNotEmpty()) {
                    addLog("Players here: ${message.players.joinToString(", ")}")
                }
                if (message.npcs.isNotEmpty()) {
                    addLog("You see: ${message.npcs.joinToString(", ") { it.name }}")
                }
                val exits = message.room.exits.keys.joinToString(", ") { it.name.lowercase() }
                addLog("Exits: $exits")
            }
            is ServerMessage.MoveError -> addLog("Cannot move: ${message.reason}")
            is ServerMessage.MapData -> _mapData.value = message
            is ServerMessage.PlayerEntered -> addLog("${message.playerName} has arrived.")
            is ServerMessage.PlayerLeft -> addLog("${message.playerName} left ${message.direction.name.lowercase()}.")
            is ServerMessage.NpcEntered -> {
                addLog("${message.npcName} has arrived.")
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
                addLog("${message.npcName} left ${message.direction.name.lowercase()}.")
                if (message.npcId.isNotEmpty()) {
                    _roomEntities.value = _roomEntities.value.filter { it.id != message.npcId }
                }
            }
            is ServerMessage.CombatHit -> {
                if (message.damage > 0) {
                    addLog("${message.attackerName} hits ${message.defenderName} for ${message.damage} damage! (${message.defenderHp}/${message.defenderMaxHp} HP)")
                }
                if (message.isPlayerDefender) {
                    // Update player HP
                    _player.value = _player.value?.copy(currentHp = message.defenderHp)
                } else {
                    // Update NPC HP in entity list
                    _roomEntities.value = _roomEntities.value.map { npc ->
                        if (npc.name == message.defenderName) {
                            npc.copy(currentHp = message.defenderHp)
                        } else npc
                    }
                }
            }
            is ServerMessage.NpcDied -> {
                addLog("${message.npcName} has been slain by ${message.killerName}!")
                _roomEntities.value = _roomEntities.value.filter { it.id != message.npcId }
                if (_selectedTargetId.value == message.npcId) {
                    _selectedTargetId.value = null
                }
            }
            is ServerMessage.PlayerDied -> {
                addLog("You have been slain by ${message.killerName}! Respawning...")
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
                    addLog("Attack mode disabled.")
                } else {
                    addLog("Attack mode enabled!")
                }
            }
            is ServerMessage.PlayerSays -> addLog("${message.playerName} says: \"${message.message}\"")
            is ServerMessage.EffectTick -> addLog(message.message)
            is ServerMessage.SystemMessage -> addLog("[System] ${message.message}")
            is ServerMessage.Error -> addLog("[Error] ${message.message}")
            is ServerMessage.LoginOk -> _player.value = message.player
            is ServerMessage.Pong -> { /* ignore */ }
            is ServerMessage.RegisterOk -> { /* handled by AuthViewModel */ }
            is ServerMessage.AuthError -> { /* handled by AuthViewModel */ }
        }
    }

    private fun addLog(text: String) {
        val log = _gameLog.value + text
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

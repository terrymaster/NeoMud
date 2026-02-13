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

    fun startCollecting() {
        viewModelScope.launch {
            wsClient.messages.collect { message ->
                handleMessage(message)
            }
        }
    }

    private fun handleMessage(message: ServerMessage) {
        when (message) {
            is ServerMessage.RoomInfo -> {
                _roomInfo.value = message
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
            is ServerMessage.NpcEntered -> addLog("${message.npcName} has arrived.")
            is ServerMessage.NpcLeft -> addLog("${message.npcName} left ${message.direction.name.lowercase()}.")
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
        _gameLog.value = _gameLog.value + text
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
}

package com.neomud.client.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neomud.client.network.ConnectionState
import com.neomud.client.network.WebSocketClient
import com.neomud.shared.model.CharacterClassDef
import com.neomud.shared.model.Item
import com.neomud.shared.model.Player
import com.neomud.shared.model.RaceDef
import com.neomud.shared.model.SpellDef
import com.neomud.shared.model.Stats
import com.neomud.shared.protocol.ClientMessage
import com.neomud.shared.protocol.ServerMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    val wsClient = WebSocketClient()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    val connectionState: StateFlow<ConnectionState> = wsClient.connectionState
    val connectionError: StateFlow<String?> = wsClient.connectionError

    private val _availableClasses = MutableStateFlow<List<CharacterClassDef>>(emptyList())
    val availableClasses: StateFlow<List<CharacterClassDef>> = _availableClasses

    private val _availableRaces = MutableStateFlow<List<RaceDef>>(emptyList())
    val availableRaces: StateFlow<List<RaceDef>> = _availableRaces

    private val _availableSpells = MutableStateFlow<List<SpellDef>>(emptyList())
    val availableSpells: StateFlow<List<SpellDef>> = _availableSpells

    private val _availableItems = MutableStateFlow<List<Item>>(emptyList())
    val availableItems: StateFlow<List<Item>> = _availableItems

    private var pendingLoginUsername: String? = null
    private var pendingLoginPassword: String? = null

    private val _initialRoomInfo = MutableStateFlow<ServerMessage.RoomInfo?>(null)
    val initialRoomInfo: StateFlow<ServerMessage.RoomInfo?> = _initialRoomInfo

    private var _serverHost: String = ""
    private var _serverPort: Int = 0
    val serverBaseUrl: String get() = "http://$_serverHost:$_serverPort"

    init {
        viewModelScope.launch {
            wsClient.messages.collect { message ->
                try {
                    when (message) {
                        is ServerMessage.LoginOk -> {
                            pendingLoginUsername = null
                            pendingLoginPassword = null
                            _initialRoomInfo.value = null
                            _authState.value = AuthState.LoggedIn(message.player)
                        }
                        is ServerMessage.RoomInfo -> {
                            // Capture the initial RoomInfo sent right after LoginOk
                            // so GameViewModel can use it before its collector subscribes
                            if (_authState.value is AuthState.LoggedIn) {
                                _initialRoomInfo.value = message
                            }
                        }
                        is ServerMessage.RegisterOk -> {
                            // Auto-login after successful registration
                            val username = pendingLoginUsername
                            val password = pendingLoginPassword
                            if (username != null && password != null) {
                                wsClient.send(ClientMessage.Login(username, password))
                            } else {
                                _authState.value = AuthState.Registered
                            }
                        }
                        is ServerMessage.AuthError -> {
                            pendingLoginUsername = null
                            pendingLoginPassword = null
                            _authState.value = AuthState.Error(message.reason)
                        }
                        is ServerMessage.ClassCatalogSync -> {
                            _availableClasses.value = message.classes
                        }
                        is ServerMessage.RaceCatalogSync -> {
                            _availableRaces.value = message.races
                        }
                        is ServerMessage.SpellCatalogSync -> {
                            _availableSpells.value = message.spells
                        }
                        is ServerMessage.ItemCatalogSync -> {
                            _availableItems.value = message.items
                        }
                        else -> { /* handled by GameViewModel */ }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("AuthViewModel", "Message handling failed for ${message::class.simpleName}", e)
                }
            }
        }
    }

    fun connect(host: String, port: Int) {
        _serverHost = host
        _serverPort = port
        wsClient.connect(host, port, viewModelScope)
    }

    fun login(username: String, password: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            val sent = wsClient.send(ClientMessage.Login(username, password))
            if (!sent) {
                _authState.value = AuthState.Error("Not connected to server")
            }
        }
    }

    fun register(username: String, password: String, characterName: String, characterClass: String, race: String = "", gender: String = "neutral", allocatedStats: Stats = Stats()) {
        _authState.value = AuthState.Loading
        pendingLoginUsername = username
        pendingLoginPassword = password
        viewModelScope.launch {
            val sent = wsClient.send(
                ClientMessage.Register(username, password, characterName, characterClass, race, gender, allocatedStats)
            )
            if (!sent) {
                pendingLoginUsername = null
                pendingLoginPassword = null
                _authState.value = AuthState.Error("Not connected to server")
            }
        }
    }

    fun clearError() {
        _authState.value = AuthState.Idle
    }

    fun logout() {
        wsClient.disconnect()
        _authState.value = AuthState.Idle
        _availableClasses.value = emptyList()
    }

    override fun onCleared() {
        wsClient.disconnect()
    }
}

sealed class AuthState {
    data object Idle : AuthState()
    data object Loading : AuthState()
    data object Registered : AuthState()
    data class LoggedIn(val player: Player) : AuthState()
    data class Error(val message: String) : AuthState()
}

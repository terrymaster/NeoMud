package com.neomud.client.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neomud.client.network.ConnectionState
import com.neomud.client.network.GameConnection
import com.neomud.client.platform.PlatformLogger
import com.neomud.client.network.WebSocketClient
import com.neomud.shared.NeoMudVersion
import com.neomud.shared.model.CharacterClassDef
import com.neomud.shared.model.Item
import com.neomud.shared.model.Player
import com.neomud.shared.model.RaceDef
import com.neomud.shared.model.SkillDef
import com.neomud.shared.model.SpellDef
import com.neomud.shared.model.Stats
import com.neomud.shared.protocol.ClientMessage
import com.neomud.shared.protocol.ServerMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    val wsClient: GameConnection = WebSocketClient()
) : ViewModel() {

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

    private val _availableSkills = MutableStateFlow<List<SkillDef>>(emptyList())
    val availableSkills: StateFlow<List<SkillDef>> = _availableSkills

    private var pendingLoginUsername: String? = null
    private var pendingLoginPassword: String? = null

    private val _initialRoomInfo = MutableStateFlow<ServerMessage.RoomInfo?>(null)
    val initialRoomInfo: StateFlow<ServerMessage.RoomInfo?> = _initialRoomInfo

    private val _initialMapData = MutableStateFlow<ServerMessage.MapData?>(null)
    val initialMapData: StateFlow<ServerMessage.MapData?> = _initialMapData

    private val _initialTutorial = MutableStateFlow<ServerMessage.Tutorial?>(null)
    val initialTutorial: StateFlow<ServerMessage.Tutorial?> = _initialTutorial

    private val _serverInfo = MutableStateFlow(ServerInfo())
    val serverInfo: StateFlow<ServerInfo> = _serverInfo

    data class UpdateInfo(val minVersion: String, val currentVersion: String)
    private val _updateRequired = MutableStateFlow<UpdateInfo?>(null)
    val updateRequired: StateFlow<UpdateInfo?> = _updateRequired

    data class NameAvailability(val usernameAvailable: Boolean, val characterNameAvailable: Boolean)
    private val _nameAvailability = MutableStateFlow<NameAvailability?>(null)
    val nameAvailability: StateFlow<NameAvailability?> = _nameAvailability

    private var _serverHost: String = ""
    private var _serverPort: Int = 0
    private var _useTls: Boolean = false
    val serverBaseUrl: String get() {
        val scheme = if (_useTls) "https" else "http"
        return "$scheme://$_serverHost:$_serverPort"
    }

    init {
        viewModelScope.launch {
            wsClient.messages.collect { message ->
                try {
                    when (message) {
                        is ServerMessage.LoginOk -> {
                            pendingLoginUsername = null
                            pendingLoginPassword = null
                            _initialRoomInfo.value = null
                            _initialMapData.value = null
                            _initialTutorial.value = null
                            _authState.value = AuthState.LoggedIn(message.player)
                        }
                        is ServerMessage.RoomInfo -> {
                            // Capture the initial RoomInfo sent right after LoginOk
                            // so GameViewModel can use it before its collector subscribes
                            if (_authState.value is AuthState.LoggedIn) {
                                _initialRoomInfo.value = message
                            }
                        }
                        is ServerMessage.MapData -> {
                            if (_authState.value is AuthState.LoggedIn) {
                                _initialMapData.value = message
                            }
                        }
                        is ServerMessage.Tutorial -> {
                            if (_authState.value is AuthState.LoggedIn) {
                                _initialTutorial.value = message
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
                        is ServerMessage.NameCheckResult -> {
                            _nameAvailability.value = NameAvailability(
                                message.usernameAvailable,
                                message.characterNameAvailable
                            )
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
                        is ServerMessage.SkillCatalogSync -> {
                            _availableSkills.value = message.skills
                        }
                        is ServerMessage.ServerHello -> {
                            _serverInfo.value = ServerInfo(
                                engineVersion = message.engineVersion,
                                protocolVersion = message.protocolVersion,
                                worldName = message.worldName,
                                worldVersion = message.worldVersion
                            )

                            // Check minimum version before proceeding
                            if (message.minClientVersion.isNotEmpty() &&
                                NeoMudVersion.compareVersions(NeoMudVersion.ENGINE_VERSION, message.minClientVersion) < 0) {
                                _updateRequired.value = UpdateInfo(
                                    minVersion = message.minClientVersion,
                                    currentVersion = NeoMudVersion.ENGINE_VERSION
                                )
                                return@collect
                            }

                            wsClient.send(ClientMessage.ClientHello(
                                clientVersion = NeoMudVersion.ENGINE_VERSION,
                                protocolVersion = NeoMudVersion.PROTOCOL_VERSION
                            ))
                        }
                        is ServerMessage.ConnectionRejected -> {
                            _updateRequired.value = UpdateInfo(
                                minVersion = message.minClientVersion,
                                currentVersion = NeoMudVersion.ENGINE_VERSION
                            )
                        }
                        else -> { /* handled by GameViewModel */ }
                    }
                } catch (e: Exception) {
                    PlatformLogger.e("AuthViewModel", "Message handling failed for ${message::class.simpleName}", e)
                }
            }
        }
    }

    fun connect(host: String, port: Int, useTls: Boolean = false, path: String = "/game") {
        _serverHost = host
        _serverPort = port
        _useTls = useTls
        wsClient.connect(host, port, useTls, viewModelScope, path)
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

    fun checkName(username: String, characterName: String) {
        _nameAvailability.value = null // reset while checking
        viewModelScope.launch {
            wsClient.send(ClientMessage.CheckName(username, characterName))
        }
    }

    fun clearNameCheck() {
        _nameAvailability.value = null
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

data class ServerInfo(
    val engineVersion: String = "",
    val protocolVersion: Int = 1,
    val worldName: String = "",
    val worldVersion: String = ""
)

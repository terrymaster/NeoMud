package com.neomud.client.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neomud.client.network.ConnectionState
import com.neomud.client.network.WebSocketClient
import com.neomud.shared.model.CharacterClass
import com.neomud.shared.model.Player
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

    init {
        viewModelScope.launch {
            wsClient.messages.collect { message ->
                when (message) {
                    is ServerMessage.LoginOk -> {
                        _authState.value = AuthState.LoggedIn(message.player)
                    }
                    is ServerMessage.RegisterOk -> {
                        _authState.value = AuthState.Registered
                    }
                    is ServerMessage.AuthError -> {
                        _authState.value = AuthState.Error(message.reason)
                    }
                    else -> { /* handled by GameViewModel */ }
                }
            }
        }
    }

    fun connect(host: String, port: Int) {
        wsClient.connect(host, port, viewModelScope)
    }

    fun login(username: String, password: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            wsClient.send(ClientMessage.Login(username, password))
        }
    }

    fun register(username: String, password: String, characterName: String, characterClass: CharacterClass) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            wsClient.send(
                ClientMessage.Register(username, password, characterName, characterClass)
            )
        }
    }

    fun clearError() {
        _authState.value = AuthState.Idle
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

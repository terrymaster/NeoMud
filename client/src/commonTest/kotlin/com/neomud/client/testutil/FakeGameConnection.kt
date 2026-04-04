package com.neomud.client.testutil

import com.neomud.client.network.ConnectionState
import com.neomud.client.network.GameConnection
import com.neomud.shared.protocol.ClientMessage
import com.neomud.shared.protocol.ServerMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Fake GameConnection for testing ViewModels without real WebSocket connections.
 * Records all calls for verification.
 */
class FakeGameConnection : GameConnection {
    private val _messages = MutableSharedFlow<ServerMessage>(extraBufferCapacity = 64)
    override val messages: SharedFlow<ServerMessage> = _messages

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _connectionError = MutableStateFlow<String?>(null)
    override val connectionError: StateFlow<String?> = _connectionError

    // ─── Call recording ──────────────────────────────────

    data class ConnectCall(val host: String, val port: Int, val useTls: Boolean)

    val connectCalls = mutableListOf<ConnectCall>()
    val sentMessages = mutableListOf<ClientMessage>()
    var disconnectCount = 0
        private set

    /** Controls what send() returns. */
    var sendResult = true

    // ─── GameConnection implementation ───────────────────

    override fun connect(host: String, port: Int, useTls: Boolean, scope: CoroutineScope) {
        connectCalls.add(ConnectCall(host, port, useTls))
        _connectionState.value = ConnectionState.CONNECTED
    }

    override suspend fun send(message: ClientMessage): Boolean {
        sentMessages.add(message)
        return sendResult
    }

    override fun disconnect() {
        disconnectCount++
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    // ─── Test helpers ────────────────────────────────────

    /** Simulate a server message arriving. */
    suspend fun receiveMessage(message: ServerMessage) {
        _messages.emit(message)
    }

    /** Set the connection state directly. */
    fun setConnectionState(state: ConnectionState) {
        _connectionState.value = state
    }

    /** Set a connection error. */
    fun setConnectionError(error: String?) {
        _connectionError.value = error
    }
}

package com.neomud.client.network

import com.neomud.client.platform.PlatformLogger
import com.neomud.client.platform.createPlatformHttpClient
import com.neomud.shared.protocol.ClientMessage
import com.neomud.shared.protocol.MessageSerializer
import com.neomud.shared.protocol.ServerMessage
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

class WebSocketClient : GameConnection {
    private val client = createPlatformHttpClient()

    private var session: DefaultClientWebSocketSession? = null
    private var connectionJob: Job? = null

    private val _messages = MutableSharedFlow<ServerMessage>(extraBufferCapacity = 64)
    override val messages: SharedFlow<ServerMessage> = _messages

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _connectionError = MutableStateFlow<String?>(null)
    override val connectionError: StateFlow<String?> = _connectionError

    override fun connect(host: String, port: Int, useTls: Boolean, scope: CoroutineScope) {
        connectionJob?.cancel()
        connectionJob = scope.launch(Dispatchers.Default) {
            _connectionState.value = ConnectionState.CONNECTING
            _connectionError.value = null
            try {
                if (useTls) {
                    client.wss(host = host, port = port, path = "/game") {
                        handleSession(this)
                    }
                } else {
                    client.webSocket(host = host, port = port, path = "/game") {
                        handleSession(this)
                    }
                }
            } catch (_: CancellationException) {
                // Normal disconnection – not an error
            } catch (e: Exception) {
                _connectionError.value = e.message ?: "Connection failed"
            } finally {
                session = null
                _connectionState.value = ConnectionState.DISCONNECTED
            }
        }
    }

    private suspend fun handleSession(wsSession: DefaultClientWebSocketSession) {
        session = wsSession
        _connectionState.value = ConnectionState.CONNECTED

        for (frame in wsSession.incoming) {
            if (frame is Frame.Text) {
                val text = frame.readText()
                try {
                    val message = MessageSerializer.decodeServerMessage(text)
                    _messages.emit(message)
                } catch (e: Exception) {
                    PlatformLogger.w("WebSocketClient", "Failed to decode message: ${text.take(200)}", e)
                }
            }
        }
    }

    override suspend fun send(message: ClientMessage): Boolean {
        val s = session ?: run {
            PlatformLogger.w("WebSocketClient", "send() called with no active session for ${message::class.simpleName}")
            return false
        }
        val text = MessageSerializer.encodeClientMessage(message)
        return try {
            s.send(Frame.Text(text))
            true
        } catch (e: Exception) {
            PlatformLogger.e("WebSocketClient", "send() failed for ${message::class.simpleName}", e)
            false
        }
    }

    override fun disconnect() {
        connectionJob?.cancel()
        connectionJob = null
    }
}

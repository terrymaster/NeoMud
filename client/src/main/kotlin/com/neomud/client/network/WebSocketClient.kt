package com.neomud.client.network

import com.neomud.shared.protocol.ClientMessage
import com.neomud.shared.protocol.MessageSerializer
import com.neomud.shared.protocol.ServerMessage
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

class WebSocketClient {
    private val client = HttpClient(OkHttp) {
        install(WebSockets)
    }

    private var session: DefaultClientWebSocketSession? = null
    private var connectionJob: Job? = null

    private val _messages = MutableSharedFlow<ServerMessage>(extraBufferCapacity = 64)
    val messages: SharedFlow<ServerMessage> = _messages

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _connectionError = MutableStateFlow<String?>(null)
    val connectionError: StateFlow<String?> = _connectionError

    fun connect(host: String, port: Int, scope: CoroutineScope) {
        connectionJob?.cancel()
        connectionJob = scope.launch(Dispatchers.IO) {
            _connectionState.value = ConnectionState.CONNECTING
            _connectionError.value = null
            try {
                client.webSocket(host = host, port = port, path = "/game") {
                    session = this
                    _connectionState.value = ConnectionState.CONNECTED

                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            val text = frame.readText()
                            try {
                                val message = MessageSerializer.decodeServerMessage(text)
                                _messages.emit(message)
                            } catch (_: Exception) {
                                // Ignore malformed messages
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                _connectionError.value = e.message ?: "Connection failed"
            } finally {
                session = null
                _connectionState.value = ConnectionState.DISCONNECTED
            }
        }
    }

    suspend fun send(message: ClientMessage): Boolean {
        val s = session ?: return false
        val text = MessageSerializer.encodeClientMessage(message)
        return try {
            s.send(Frame.Text(text))
            true
        } catch (e: Exception) {
            false
        }
    }

    fun disconnect() {
        connectionJob?.cancel()
        connectionJob = null
    }
}

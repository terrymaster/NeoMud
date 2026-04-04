package com.neomud.client.network

import com.neomud.shared.protocol.ClientMessage
import com.neomud.shared.protocol.ServerMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/** Abstraction over the WebSocket connection for testability. */
interface GameConnection {
    val messages: SharedFlow<ServerMessage>
    val connectionState: StateFlow<ConnectionState>
    val connectionError: StateFlow<String?>

    fun connect(host: String, port: Int, useTls: Boolean, scope: CoroutineScope)
    suspend fun send(message: ClientMessage): Boolean
    fun disconnect()
}

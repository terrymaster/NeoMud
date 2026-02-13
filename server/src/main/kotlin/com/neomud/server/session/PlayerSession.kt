package com.neomud.server.session

import com.neomud.shared.model.*
import com.neomud.shared.protocol.MessageSerializer
import com.neomud.shared.protocol.ServerMessage
import io.ktor.websocket.*

class PlayerSession(
    val webSocketSession: WebSocketSession
) {
    var playerName: String? = null
    var currentRoomId: RoomId? = null
    var player: Player? = null
    val activeEffects: MutableList<ActiveEffect> = mutableListOf()

    val isAuthenticated: Boolean get() = player != null

    suspend fun send(message: ServerMessage) {
        val text = MessageSerializer.encodeServerMessage(message)
        webSocketSession.send(Frame.Text(text))
    }
}

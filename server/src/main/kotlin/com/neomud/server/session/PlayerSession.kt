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
    var attackMode: Boolean = false
    var selectedTargetId: String? = null
    var isHidden: Boolean = false
    var godMode: Boolean = false
    val skillCooldowns: MutableMap<String, Int> = mutableMapOf()

    // Rate limiting (token bucket)
    private var messageTokens: Double = BURST_CAPACITY.toDouble()
    private var lastRefillTime: Long = System.currentTimeMillis()

    fun tryConsumeMessage(): Boolean {
        val now = System.currentTimeMillis()
        val elapsed = (now - lastRefillTime) / 1000.0
        lastRefillTime = now
        messageTokens = (messageTokens + elapsed * MAX_MESSAGES_PER_SECOND)
            .coerceAtMost(BURST_CAPACITY.toDouble())
        return if (messageTokens >= 1.0) {
            messageTokens -= 1.0
            true
        } else false
    }

    val isAuthenticated: Boolean get() = player != null

    suspend fun send(message: ServerMessage) {
        val text = MessageSerializer.encodeServerMessage(message)
        webSocketSession.send(Frame.Text(text))
    }

    companion object {
        const val MAX_MESSAGES_PER_SECOND = 10
        const val BURST_CAPACITY = 20
    }
}

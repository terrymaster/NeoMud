package com.neomud.server.session

import com.neomud.shared.model.RoomId
import com.neomud.shared.protocol.ServerMessage
import java.util.concurrent.ConcurrentHashMap

class SessionManager {
    private val sessions = ConcurrentHashMap<String, PlayerSession>()

    fun addSession(playerName: String, session: PlayerSession) {
        sessions[playerName] = session
    }

    fun removeSession(playerName: String) {
        sessions.remove(playerName)
    }

    fun getSession(playerName: String): PlayerSession? = sessions[playerName]

    fun isLoggedIn(playerName: String): Boolean = sessions.containsKey(playerName)

    fun getSessionsInRoom(roomId: RoomId): List<PlayerSession> =
        sessions.values.filter { it.currentRoomId == roomId }

    fun getPlayerNamesInRoom(roomId: RoomId): List<String> =
        sessions.values
            .filter { it.currentRoomId == roomId && it.playerName != null }
            .map { it.playerName!! }

    fun getVisiblePlayerNamesInRoom(roomId: RoomId): List<String> =
        sessions.values
            .filter { it.currentRoomId == roomId && it.playerName != null && !it.isHidden }
            .map { it.playerName!! }

    fun getAllAuthenticatedSessions(): List<PlayerSession> =
        sessions.values.filter { it.isAuthenticated }

    suspend fun broadcastToAll(message: ServerMessage) {
        sessions.values.forEach { session ->
            try {
                session.send(message)
            } catch (_: Exception) {
                // Session might be closing
            }
        }
    }

    suspend fun broadcastToRoom(roomId: RoomId, message: ServerMessage, exclude: String? = null) {
        sessions.values
            .filter { it.currentRoomId == roomId && it.playerName != exclude }
            .forEach { session ->
                try {
                    session.send(message)
                } catch (_: Exception) {
                    // Session might be closing
                }
            }
    }
}

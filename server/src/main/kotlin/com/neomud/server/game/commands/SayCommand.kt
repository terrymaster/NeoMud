package com.neomud.server.game.commands

import com.neomud.server.session.PlayerSession
import com.neomud.server.session.SessionManager
import com.neomud.shared.protocol.ServerMessage

class SayCommand(
    private val sessionManager: SessionManager
) {
    suspend fun execute(session: PlayerSession, message: String) {
        val roomId = session.currentRoomId ?: return
        val playerName = session.playerName ?: return

        sessionManager.broadcastToRoom(
            roomId,
            ServerMessage.PlayerSays(playerName, message)
        )
    }
}

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

        // Break stealth on say
        if (session.isHidden) {
            session.isHidden = false
            session.send(ServerMessage.HideModeUpdate(false, "Speaking reveals your presence!"))
            sessionManager.broadcastToRoom(
                roomId,
                ServerMessage.PlayerEntered(playerName, roomId),
                exclude = playerName
            )
        }

        val sanitized = message.take(500)

        sessionManager.broadcastToRoom(
            roomId,
            ServerMessage.PlayerSays(playerName, sanitized)
        )
    }
}

package com.neomud.server.game.commands

import com.neomud.server.game.MeditationUtils
import com.neomud.server.session.PlayerSession
import com.neomud.server.session.SessionManager
import com.neomud.shared.protocol.ServerMessage

class SayCommand(
    private val sessionManager: SessionManager,
    private val adminCommand: AdminCommand
) {
    suspend fun execute(session: PlayerSession, message: String) {
        val roomId = session.currentRoomId ?: return
        val playerName = session.playerName ?: return

        // Intercept slash commands
        if (message.startsWith("/")) {
            if (session.player?.isAdmin == true) {
                adminCommand.execute(session, message)
            } else {
                session.send(ServerMessage.SystemMessage("Unknown command."))
            }
            return
        }

        // Break meditation on say
        MeditationUtils.breakMeditation(session, "You stop meditating.")

        // Break stealth on say
        if (session.isHidden) {
            session.isHidden = false
            session.send(ServerMessage.StealthUpdate(false, "Speaking reveals your presence!"))
            sessionManager.broadcastToRoom(
                roomId,
                ServerMessage.PlayerEntered(playerName, roomId, session.toPlayerInfo()),
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

package com.neomud.server.game

import com.neomud.server.session.PlayerSession
import com.neomud.shared.protocol.ServerMessage

object MeditationUtils {
    suspend fun breakMeditation(session: PlayerSession, message: String) {
        if (!session.isMeditating) return
        session.isMeditating = false
        session.send(ServerMessage.MeditateUpdate(false, message))
    }
}

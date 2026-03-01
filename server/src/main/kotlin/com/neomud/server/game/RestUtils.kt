package com.neomud.server.game

import com.neomud.server.session.PlayerSession
import com.neomud.shared.protocol.ServerMessage

object RestUtils {
    suspend fun breakRest(session: PlayerSession, message: String) {
        if (!session.isResting) return
        session.isResting = false
        session.send(ServerMessage.RestUpdate(false, message))
    }
}

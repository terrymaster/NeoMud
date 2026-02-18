package com.neomud.server.game

import com.neomud.server.session.PlayerSession
import com.neomud.server.session.SessionManager
import com.neomud.server.world.ClassCatalog
import com.neomud.server.world.SkillCatalog
import com.neomud.shared.protocol.ServerMessage

object StealthUtils {
    suspend fun breakStealth(session: PlayerSession, sessionManager: SessionManager, message: String) {
        if (!session.isHidden) return
        val roomId = session.currentRoomId ?: return
        val playerName = session.playerName ?: return

        session.isHidden = false
        session.send(ServerMessage.StealthUpdate(false, message))
        sessionManager.broadcastToRoom(
            roomId,
            ServerMessage.PlayerEntered(playerName, roomId, session.toPlayerInfo()),
            exclude = playerName
        )
    }

    fun perceptionBonus(playerClass: String, classCatalog: ClassCatalog): Int {
        val classDef = classCatalog.getClass(playerClass) ?: return 0
        return if ("PERCEPTION" in classDef.skills) 3 else 0
    }
}

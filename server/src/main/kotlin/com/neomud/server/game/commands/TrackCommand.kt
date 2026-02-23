package com.neomud.server.game.commands

import com.neomud.server.session.PendingSkill
import com.neomud.server.session.PlayerSession
import com.neomud.shared.protocol.ServerMessage

class TrackCommand {
    suspend fun execute(session: PlayerSession, targetId: String? = null) {
        session.player ?: return

        val cooldown = session.skillCooldowns["TRACK"]
        if (cooldown != null && cooldown > 0) {
            session.send(ServerMessage.SystemMessage("Track is on cooldown ($cooldown ticks remaining)."))
            return
        }

        // Queue for next tick
        session.pendingSkill = PendingSkill.Track(targetId)
    }
}

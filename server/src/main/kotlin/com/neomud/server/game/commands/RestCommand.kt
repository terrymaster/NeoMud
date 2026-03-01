package com.neomud.server.game.commands

import com.neomud.server.game.MeditationUtils
import com.neomud.server.session.PendingSkill
import com.neomud.server.session.PlayerSession
import com.neomud.shared.protocol.ServerMessage

class RestCommand {
    suspend fun execute(session: PlayerSession) {
        val player = session.player ?: return

        // If already resting, cancel immediately (no tick needed)
        if (session.isResting) {
            session.isResting = false
            session.send(ServerMessage.RestUpdate(false, "You stop resting."))
            return
        }

        if (session.attackMode) {
            session.send(ServerMessage.SystemMessage("You cannot rest while in combat!"))
            return
        }

        if (player.currentHp >= player.maxHp) {
            session.send(ServerMessage.SystemMessage("Your health is already full."))
            return
        }

        val cooldown = session.skillCooldowns["REST"]
        if (cooldown != null && cooldown > 0) {
            session.send(ServerMessage.SystemMessage("Rest is on cooldown ($cooldown ticks remaining)."))
            return
        }

        // Break meditation if meditating (mutual exclusion)
        MeditationUtils.breakMeditation(session, "You stop meditating to rest.")

        // Queue for next tick
        session.pendingSkill = PendingSkill.Rest
    }
}

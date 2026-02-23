package com.neomud.server.game.commands

import com.neomud.server.session.PendingSkill
import com.neomud.server.session.PlayerSession
import com.neomud.shared.protocol.ServerMessage

class MeditateCommand {
    suspend fun execute(session: PlayerSession) {
        val player = session.player ?: return

        // If already meditating, cancel immediately (no tick needed)
        if (session.isMeditating) {
            session.isMeditating = false
            session.send(ServerMessage.MeditateUpdate(false, "You stop meditating."))
            return
        }

        if (session.attackMode) {
            session.send(ServerMessage.SystemMessage("You cannot meditate while in combat!"))
            return
        }

        if (player.currentMp >= player.maxMp) {
            session.send(ServerMessage.SystemMessage("Your mana is already full."))
            return
        }

        val cooldown = session.skillCooldowns["MEDITATE"]
        if (cooldown != null && cooldown > 0) {
            session.send(ServerMessage.SystemMessage("Meditate is on cooldown ($cooldown ticks remaining)."))
            return
        }

        // Queue for next tick
        session.pendingSkill = PendingSkill.Meditate
    }
}

package com.neomud.server.game.commands

import com.neomud.server.persistence.repository.PlayerRepository
import com.neomud.server.session.PlayerSession
import com.neomud.shared.protocol.ServerMessage

class MeditateCommand(
    private val playerRepository: PlayerRepository
) {
    suspend fun execute(session: PlayerSession) {
        val player = session.player ?: return

        if (session.attackMode) {
            session.send(ServerMessage.SystemMessage("You cannot meditate while in combat!"))
            return
        }

        val cooldown = session.skillCooldowns["MEDITATE"]
        if (cooldown != null && cooldown > 0) {
            session.send(ServerMessage.SystemMessage("Meditate is on cooldown ($cooldown ticks remaining)."))
            return
        }

        val missingMp = player.maxMp - player.currentMp
        if (missingMp <= 0) {
            session.send(ServerMessage.SystemMessage("Your mana is already full."))
            return
        }

        val restored = minOf(15, missingMp)
        val newMp = player.currentMp + restored
        session.player = player.copy(currentMp = newMp)

        session.skillCooldowns["MEDITATE"] = 6

        // Use SpellCastResult to push MP update to client (it handles newMp)
        session.send(ServerMessage.SpellCastResult(
            success = true,
            spellName = "Meditate",
            message = "You meditate and restore $restored mana.",
            newMp = newMp
        ))

        try {
            playerRepository.savePlayerState(session.player!!)
        } catch (_: Exception) { }
    }
}

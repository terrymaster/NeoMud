package com.neomud.server.game.commands

import com.neomud.server.game.GameConfig
import com.neomud.server.game.MeditationUtils
import com.neomud.server.game.RestUtils
import com.neomud.server.game.StealthUtils

import com.neomud.server.game.npc.NpcManager
import com.neomud.server.session.PendingSkill
import com.neomud.server.session.PlayerSession
import com.neomud.server.session.SessionManager
import com.neomud.server.world.ClassCatalog
import com.neomud.server.world.SkillCatalog
import com.neomud.shared.protocol.ServerMessage

class SneakCommand(
    private val sessionManager: SessionManager,
    private val npcManager: NpcManager,
    private val skillCatalog: SkillCatalog,
    private val classCatalog: ClassCatalog
) {
    suspend fun handleToggle(session: PlayerSession, enabled: Boolean) {
        val roomId = session.currentRoomId ?: return
        val playerName = session.playerName ?: return
        val player = session.player ?: return

        if (!enabled) {
            // Unsneak
            if (!session.isHidden) {
                session.send(ServerMessage.StealthUpdate(false, "You are not hidden."))
                return
            }
            session.isHidden = false
            session.send(ServerMessage.StealthUpdate(false, "You step out of the shadows."))
            sessionManager.broadcastToRoom(
                roomId,
                ServerMessage.PlayerEntered(playerName, roomId, session.toPlayerInfo()),
                exclude = playerName
            )
            return
        }

        // Sneaking breaks meditation and rest
        MeditationUtils.breakMeditation(session, "You stop meditating.")
        RestUtils.breakRest(session, "You stop resting.")

        // Sneak attempt
        if (session.attackMode) {
            session.send(ServerMessage.SystemMessage("You cannot sneak while in combat!"))
            return
        }

        if (session.isHidden) {
            session.send(ServerMessage.StealthUpdate(true, "You are already hidden."))
            return
        }

        // Check cooldown
        val cooldown = session.skillCooldowns["SNEAK"]
        if (cooldown != null && cooldown > 0) {
            session.send(ServerMessage.SystemMessage("You must wait before sneaking again."))
            return
        }

        // Queue for resolution on next game tick
        session.pendingSkill = PendingSkill.Sneak
    }
}

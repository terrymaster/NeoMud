package com.neomud.server.game.commands

import com.neomud.server.game.MeditationUtils
import com.neomud.server.game.StealthUtils

import com.neomud.server.game.npc.NpcManager
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

        // Sneaking breaks meditation
        MeditationUtils.breakMeditation(session, "You stop meditating.")

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

        // Skill check: AGI + WIL/2 + level/2 + d20 vs 15 (using buffed stats)
        val stats = session.effectiveStats()
        val roll = (1..20).random()
        val check = stats.agility + stats.willpower / 2 + player.level / 2 + roll
        val difficulty = 15

        session.skillCooldowns["SNEAK"] = 2

        if (check < difficulty) {
            session.send(ServerMessage.StealthUpdate(false, "You fail to find cover! (roll: $roll)"))
            return
        }

        // Stealth check passed - NPCs in the room get a perception check
        val stealthDc = stats.agility + stats.willpower / 2 + player.level / 2 + 10
        val npcsInRoom = npcManager.getLivingNpcsInRoom(roomId)
        var detected = false
        var detectorName = ""

        for (npc in npcsInRoom) {
            val npcRoll = npc.perception + npc.level + (1..20).random()
            if (npcRoll >= stealthDc) {
                detected = true
                detectorName = npc.name
                break
            }
        }

        // Non-hidden players in room get perception checks
        if (!detected) {
            for (otherSession in sessionManager.getSessionsInRoom(roomId)) {
                if (otherSession == session || otherSession.isHidden) continue
                val otherPlayer = otherSession.player ?: continue
                val otherStats = otherSession.effectiveStats()
                val bonus = StealthUtils.perceptionBonus(otherPlayer.characterClass, classCatalog)
                val observerRoll = otherStats.willpower + otherStats.intellect / 2 + otherPlayer.level / 2 + bonus + (1..20).random()
                if (observerRoll >= stealthDc) {
                    detected = true
                    detectorName = otherPlayer.name
                    break
                }
            }
        }

        if (detected) {
            session.send(ServerMessage.StealthUpdate(false, "$detectorName notices your attempt to hide!"))
            return
        }

        // Successfully hidden!
        session.isHidden = true
        session.send(ServerMessage.StealthUpdate(true, "You slip into the shadows."))
        // Player vanishes from others' view
        sessionManager.broadcastToRoom(
            roomId,
            ServerMessage.PlayerLeft(playerName, roomId, com.neomud.shared.model.Direction.NORTH),
            exclude = playerName
        )
    }
}

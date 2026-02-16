package com.neomud.server.game.commands

import com.neomud.server.game.combat.CombatUtils
import com.neomud.server.game.npc.NpcManager
import com.neomud.server.session.PlayerSession
import com.neomud.server.session.SessionManager
import com.neomud.shared.protocol.ServerMessage

class HideCommand(
    private val sessionManager: SessionManager,
    private val npcManager: NpcManager
) {
    suspend fun handleToggle(session: PlayerSession, enabled: Boolean) {
        val roomId = session.currentRoomId ?: return
        val playerName = session.playerName ?: return
        val player = session.player ?: return

        if (!enabled) {
            // Unhide
            if (!session.isHidden) {
                session.send(ServerMessage.HideModeUpdate(false, "You are not hidden."))
                return
            }
            session.isHidden = false
            session.send(ServerMessage.HideModeUpdate(false, "You step out of the shadows."))
            sessionManager.broadcastToRoom(
                roomId,
                ServerMessage.PlayerEntered(playerName, roomId),
                exclude = playerName
            )
            return
        }

        // Hide attempt
        if (session.attackMode) {
            session.send(ServerMessage.SystemMessage("You cannot hide while in combat!"))
            return
        }

        if (session.isHidden) {
            session.send(ServerMessage.HideModeUpdate(true, "You are already hidden."))
            return
        }

        // Check cooldown
        val cooldown = session.skillCooldowns["HIDE"]
        if (cooldown != null && cooldown > 0) {
            session.send(ServerMessage.SystemMessage("You must wait before hiding again."))
            return
        }

        // Skill check: DEX + INT/2 + d20 vs 15 (using buffed stats)
        val stats = CombatUtils.effectiveStats(player.stats, session.activeEffects.toList())
        val roll = (1..20).random()
        val check = stats.agility + stats.intellect / 2 + roll
        val difficulty = 15

        session.skillCooldowns["HIDE"] = 2

        if (check < difficulty) {
            session.send(ServerMessage.HideModeUpdate(false, "You fail to find cover! (roll: $roll)"))
            return
        }

        // Hide check passed - now all NPCs in the room get a perception check
        val npcsInRoom = npcManager.getLivingNpcsInRoom(roomId)
        var detected = false

        for (npc in npcsInRoom) {
            // NPC perception: perception + level + d20 vs player stealth DC: DEX + INT/2 + level/2 + 10
            val npcRoll = npc.perception + npc.level + (1..20).random()
            val stealthDc = stats.agility + stats.intellect / 2 + player.level / 2 + 10

            if (npcRoll >= stealthDc) {
                session.send(ServerMessage.HideModeUpdate(false, "${npc.name} notices your attempt to hide!"))
                detected = true
                break
            }
        }

        if (detected) {
            return
        }

        // Successfully hidden!
        session.isHidden = true
        session.send(ServerMessage.HideModeUpdate(true, "You slip into the shadows."))
        // Player vanishes from others' view
        sessionManager.broadcastToRoom(
            roomId,
            ServerMessage.PlayerLeft(playerName, roomId, com.neomud.shared.model.Direction.NORTH),
            exclude = playerName
        )
    }
}

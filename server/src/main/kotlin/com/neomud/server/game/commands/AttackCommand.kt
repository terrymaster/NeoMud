package com.neomud.server.game.commands

import com.neomud.server.game.npc.NpcManager
import com.neomud.server.session.PlayerSession
import com.neomud.shared.protocol.ServerMessage

class AttackCommand(
    private val npcManager: NpcManager
) {
    suspend fun handleToggle(session: PlayerSession, enabled: Boolean) {
        val roomId = session.currentRoomId ?: return

        if (enabled) {
            val hostiles = npcManager.getLivingHostileNpcsInRoom(roomId)
            if (hostiles.isEmpty()) {
                session.send(ServerMessage.AttackModeUpdate(false))
                session.send(ServerMessage.SystemMessage("No hostile targets here."))
                return
            }

            session.attackMode = true
            session.send(ServerMessage.AttackModeUpdate(true))
        } else {
            session.attackMode = false
            session.selectedTargetId = null
            session.send(ServerMessage.AttackModeUpdate(false))
        }
    }

    suspend fun handleSelectTarget(session: PlayerSession, npcId: String?) {
        if (npcId == null) {
            session.selectedTargetId = null
            return
        }

        val roomId = session.currentRoomId ?: return
        val npc = npcManager.getNpcState(npcId)

        if (npc != null && npc.currentRoomId == roomId && npc.hostile) {
            session.selectedTargetId = npcId
        } else {
            session.selectedTargetId = null
            session.send(ServerMessage.SystemMessage("Invalid target."))
        }
    }
}

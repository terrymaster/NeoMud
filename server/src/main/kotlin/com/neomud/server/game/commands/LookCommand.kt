package com.neomud.server.game.commands

import com.neomud.server.game.npc.NpcManager
import com.neomud.server.session.PlayerSession
import com.neomud.server.session.SessionManager
import com.neomud.server.world.WorldGraph
import com.neomud.shared.protocol.ServerMessage

class LookCommand(
    private val worldGraph: WorldGraph,
    private val sessionManager: SessionManager,
    private val npcManager: NpcManager
) {
    suspend fun execute(session: PlayerSession) {
        val currentRoomId = session.currentRoomId ?: return
        val room = worldGraph.getRoom(currentRoomId)

        if (room == null) {
            session.send(ServerMessage.Error("You are in an invalid location."))
            return
        }

        val playerName = session.playerName!!
        val playersInRoom = sessionManager.getPlayerNamesInRoom(currentRoomId)
            .filter { it != playerName }
        val npcsInRoom = npcManager.getNpcsInRoom(currentRoomId)

        session.send(ServerMessage.RoomInfo(room, playersInRoom, npcsInRoom))
    }
}

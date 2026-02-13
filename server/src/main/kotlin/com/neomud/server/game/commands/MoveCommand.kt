package com.neomud.server.game.commands

import com.neomud.server.game.inventory.RoomItemManager
import com.neomud.server.game.npc.NpcManager
import com.neomud.server.persistence.repository.PlayerRepository
import com.neomud.server.session.PlayerSession
import com.neomud.server.session.SessionManager
import com.neomud.server.world.WorldGraph
import com.neomud.shared.model.Direction
import com.neomud.shared.protocol.ServerMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.coroutineScope

class MoveCommand(
    private val worldGraph: WorldGraph,
    private val sessionManager: SessionManager,
    private val npcManager: NpcManager,
    private val playerRepository: PlayerRepository,
    private val roomItemManager: RoomItemManager
) {
    suspend fun execute(session: PlayerSession, direction: Direction) {
        val currentRoomId = session.currentRoomId ?: return
        val currentRoom = worldGraph.getRoom(currentRoomId)

        if (currentRoom == null) {
            session.send(ServerMessage.MoveError("You are in an invalid location."))
            return
        }

        val targetRoomId = currentRoom.exits[direction]
        if (targetRoomId == null) {
            session.send(ServerMessage.MoveError("You cannot go ${direction.name.lowercase()}."))
            return
        }

        val targetRoom = worldGraph.getRoom(targetRoomId)
        if (targetRoom == null) {
            session.send(ServerMessage.MoveError("That exit leads nowhere."))
            return
        }

        val playerName = session.playerName!!

        // Broadcast leave to old room
        sessionManager.broadcastToRoom(
            currentRoomId,
            ServerMessage.PlayerLeft(playerName, currentRoomId, direction),
            exclude = playerName
        )

        // Update session
        session.currentRoomId = targetRoomId
        session.player = session.player?.copy(currentRoomId = targetRoomId)

        // Cancel attack mode on move
        if (session.attackMode) {
            session.attackMode = false
            session.selectedTargetId = null
            session.send(ServerMessage.AttackModeUpdate(false))
        }

        // Broadcast enter to new room
        sessionManager.broadcastToRoom(
            targetRoomId,
            ServerMessage.PlayerEntered(playerName, targetRoomId),
            exclude = playerName
        )

        // Send MoveOk + MapData to the player
        val playersInRoom = sessionManager.getPlayerNamesInRoom(targetRoomId)
            .filter { it != playerName }
        val npcsInRoom = npcManager.getNpcsInRoom(targetRoomId)

        session.send(ServerMessage.MoveOk(direction, targetRoom, playersInRoom, npcsInRoom))

        val mapRooms = worldGraph.getRoomsNear(targetRoomId).map { mapRoom ->
            mapRoom.copy(
                hasPlayers = sessionManager.getPlayerNamesInRoom(mapRoom.id).isNotEmpty(),
                hasNpcs = npcManager.getNpcsInRoom(mapRoom.id).isNotEmpty()
            )
        }
        session.send(ServerMessage.MapData(mapRooms, targetRoomId))

        // Send ground items for new room
        val groundItems = roomItemManager.getGroundItems(targetRoomId)
        val groundCoins = roomItemManager.getGroundCoins(targetRoomId)
        session.send(ServerMessage.RoomItemsUpdate(groundItems, groundCoins))

        // Persist position async
        val player = session.player
        if (player != null) {
            coroutineScope {
                launch(Dispatchers.IO) {
                    try {
                        playerRepository.savePlayerState(player)
                    } catch (_: Exception) {
                        // Fire-and-forget
                    }
                }
            }
        }
    }
}

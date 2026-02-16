package com.neomud.server.game.commands

import com.neomud.server.game.inventory.RoomItemManager
import com.neomud.server.game.npc.NpcManager
import com.neomud.server.persistence.repository.PlayerRepository
import com.neomud.server.session.PlayerSession
import com.neomud.server.session.SessionManager
import com.neomud.server.world.LockStateManager
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
    private val roomItemManager: RoomItemManager,
    private val lockStateManager: LockStateManager
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

        // Check if exit is locked
        val lockDifficulty = currentRoom.lockedExits[direction]
        if (lockDifficulty != null && !lockStateManager.isUnlocked(currentRoomId, direction)) {
            session.send(ServerMessage.MoveError("The door to the ${direction.name} is locked."))
            return
        }

        val targetRoom = worldGraph.getRoom(targetRoomId)
        if (targetRoom == null) {
            session.send(ServerMessage.MoveError("That exit leads nowhere."))
            return
        }

        val playerName = session.playerName!!
        val player = session.player

        // Stealth check on room transition
        var sneaking = false
        if (session.isHidden && player != null) {
            val stats = player.stats
            val roll = (1..20).random()
            val check = stats.agility + stats.intellect / 2 + player.level / 2 + roll
            val difficulty = 15

            if (check >= difficulty) {
                // Sneak successful - stay hidden, no broadcasts
                sneaking = true
                session.send(ServerMessage.HideModeUpdate(true, "Sneaking..."))
            } else {
                // Sneak failed - stealth breaks
                session.isHidden = false
                session.send(ServerMessage.HideModeUpdate(false, "Your movement gives you away!"))
            }
        }

        // Broadcast leave to old room (only if not sneaking)
        if (!sneaking) {
            sessionManager.broadcastToRoom(
                currentRoomId,
                ServerMessage.PlayerLeft(playerName, currentRoomId, direction),
                exclude = playerName
            )
        }

        // Update session
        session.currentRoomId = targetRoomId
        session.player = session.player?.copy(currentRoomId = targetRoomId)

        // Cancel attack mode on move
        if (session.attackMode) {
            session.attackMode = false
            session.selectedTargetId = null
            session.send(ServerMessage.AttackModeUpdate(false))
        }

        // Broadcast enter to new room (only if not sneaking)
        if (!sneaking) {
            sessionManager.broadcastToRoom(
                targetRoomId,
                ServerMessage.PlayerEntered(playerName, targetRoomId),
                exclude = playerName
            )
        } else {
            // Even if the player's sneak check passed, NPCs in the new room get perception rolls
            val npcsHere = npcManager.getLivingNpcsInRoom(targetRoomId)
            if (npcsHere.isNotEmpty() && player != null) {
                val stats = player.stats
                val stealthDc = stats.agility + stats.intellect / 2 + player.level / 2 + 10
                for (npc in npcsHere) {
                    if (!session.isHidden) break // already detected by a prior NPC
                    val npcRoll = npc.perception + npc.level + (1..20).random()
                    if (npcRoll >= stealthDc) {
                        session.isHidden = false
                        sneaking = false
                        session.send(ServerMessage.HideModeUpdate(false, "${npc.name} notices you sneaking in!"))
                        sessionManager.broadcastToRoom(
                            targetRoomId,
                            ServerMessage.PlayerEntered(playerName, targetRoomId),
                            exclude = playerName
                        )
                        break
                    }
                }
            }
        }

        // Send MoveOk + MapData to the player
        val playersInRoom = sessionManager.getVisiblePlayerNamesInRoom(targetRoomId)
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

        // Auto-detect trainer in room
        if (npcManager.getTrainerInRoom(targetRoomId) != null) {
            session.send(ServerMessage.SystemMessage("A trainer is here. You can interact to train your skills."))
        }

        // Persist position async
        val playerState = session.player
        if (playerState != null) {
            coroutineScope {
                launch(Dispatchers.IO) {
                    try {
                        playerRepository.savePlayerState(playerState)
                    } catch (_: Exception) {
                        // Fire-and-forget
                    }
                }
            }
        }
    }
}

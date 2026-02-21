package com.neomud.server.game.commands

import com.neomud.server.game.RoomFilter
import com.neomud.server.game.StealthUtils

import com.neomud.server.game.inventory.RoomItemManager
import com.neomud.server.game.npc.NpcManager
import com.neomud.server.session.PlayerSession
import com.neomud.server.session.SessionManager
import com.neomud.server.world.ClassCatalog
import com.neomud.server.world.SkillCatalog
import com.neomud.server.world.WorldGraph
import com.neomud.shared.protocol.ServerMessage

class LookCommand(
    private val worldGraph: WorldGraph,
    private val sessionManager: SessionManager,
    private val npcManager: NpcManager,
    private val roomItemManager: RoomItemManager,
    private val skillCatalog: SkillCatalog,
    private val classCatalog: ClassCatalog
) {
    suspend fun execute(session: PlayerSession) {
        val currentRoomId = session.currentRoomId ?: return
        val room = worldGraph.getRoom(currentRoomId)

        if (room == null) {
            session.send(ServerMessage.Error("You are in an invalid location."))
            return
        }

        val playerName = session.playerName!!
        val player = session.player

        // Passive perception check for hidden exits and interactables before sending room info
        if (player != null) {
            checkHiddenExits(session, currentRoomId)
            checkHiddenInteractables(session, currentRoomId)
        }

        val filteredRoom = RoomFilter.forPlayer(room, session, worldGraph)

        val playersInRoom = sessionManager.getVisiblePlayerInfosInRoom(currentRoomId)
            .filter { it.name != playerName }
        val npcsInRoom = npcManager.getNpcsInRoom(currentRoomId)

        session.send(ServerMessage.RoomInfo(filteredRoom, playersInRoom, npcsInRoom))

        val mapRooms = worldGraph.getRoomsNear(currentRoomId).map { mapRoom ->
            mapRoom.copy(
                hasPlayers = sessionManager.getPlayerNamesInRoom(mapRoom.id).isNotEmpty(),
                hasNpcs = npcManager.getNpcsInRoom(mapRoom.id).isNotEmpty()
            )
        }
        session.send(ServerMessage.MapData(mapRooms, currentRoomId))

        // Send ground items
        val groundItems = roomItemManager.getGroundItems(currentRoomId)
        val groundCoins = roomItemManager.getGroundCoins(currentRoomId)
        session.send(ServerMessage.RoomItemsUpdate(groundItems, groundCoins))

        // Active perception check against hidden players in room
        if (player != null && !session.isHidden) {
            val hiddenSessions = sessionManager.getSessionsInRoom(currentRoomId)
                .filter { it.isHidden && it != session }
            if (hiddenSessions.isNotEmpty()) {
                val effStats = session.effectiveStats()
                val bonus = StealthUtils.perceptionBonus(player.characterClass, classCatalog)
                val observerRoll = effStats.willpower + effStats.intellect / 2 + player.level / 2 + bonus + (1..20).random()

                for (hiddenSession in hiddenSessions) {
                    if (!hiddenSession.isHidden) continue
                    val hiddenPlayer = hiddenSession.player ?: continue
                    val hiddenStats = hiddenSession.effectiveStats()
                    val stealthDc = hiddenStats.agility + hiddenStats.willpower / 2 + hiddenPlayer.level / 2 + 10

                    if (observerRoll >= stealthDc) {
                        StealthUtils.breakStealth(hiddenSession, sessionManager, "${player.name}'s keen eyes spot you lurking in the shadows!")
                        session.send(ServerMessage.SystemMessage("Your keen eyes spot ${hiddenPlayer.name} lurking in the shadows!"))
                    }
                }
            }
        }
    }

    private suspend fun checkHiddenInteractables(session: PlayerSession, roomId: String) {
        val player = session.player ?: return
        val defs = worldGraph.getInteractableDefs(roomId).filter { it.perceptionDC > 0 }
        if (defs.isEmpty()) return

        val effStats = session.effectiveStats()
        val bonus = StealthUtils.perceptionBonus(player.characterClass, classCatalog)
        val roll = effStats.willpower + effStats.intellect / 2 + player.level / 2 + bonus + (1..20).random()

        for (feat in defs) {
            if (session.hasDiscoveredInteractable(roomId, feat.id)) continue
            if (roll >= feat.perceptionDC) {
                session.discoverInteractable(roomId, feat.id)
                session.send(ServerMessage.SystemMessage("You notice something: ${feat.label}"))
            }
        }
    }

    private suspend fun checkHiddenExits(session: PlayerSession, roomId: String) {
        val player = session.player ?: return
        val hiddenDefs = worldGraph.getHiddenExitDefs(roomId)
        if (hiddenDefs.isEmpty()) return

        val effStats = session.effectiveStats()
        val bonus = StealthUtils.perceptionBonus(player.characterClass, classCatalog)
        val roll = effStats.willpower + effStats.intellect / 2 + player.level / 2 + bonus + (1..20).random()

        for ((dir, data) in hiddenDefs) {
            if (session.hasDiscoveredExit(roomId, dir)) continue
            if (roll >= data.perceptionDC) {
                session.discoverExit(roomId, dir)
                worldGraph.revealHiddenExit(roomId, dir)
                session.send(ServerMessage.SystemMessage("You notice a hidden passage to the ${dir.name.lowercase()}!"))
            }
        }
    }
}

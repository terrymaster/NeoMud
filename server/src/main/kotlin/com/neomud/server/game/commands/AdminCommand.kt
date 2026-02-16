package com.neomud.server.game.commands

import com.neomud.server.game.inventory.RoomItemManager
import com.neomud.server.game.npc.NpcManager
import com.neomud.server.game.progression.XpCalculator
import com.neomud.server.persistence.repository.InventoryRepository
import com.neomud.server.persistence.repository.PlayerRepository
import com.neomud.server.session.PlayerSession
import com.neomud.server.session.SessionManager
import com.neomud.server.world.ClassCatalog
import com.neomud.server.world.ItemCatalog
import com.neomud.server.world.RaceCatalog
import com.neomud.server.world.WorldGraph
import com.neomud.shared.model.Direction
import com.neomud.shared.protocol.ServerMessage
import org.slf4j.LoggerFactory

class AdminCommand(
    private val sessionManager: SessionManager,
    private val playerRepository: PlayerRepository,
    private val npcManager: NpcManager,
    private val worldGraph: WorldGraph,
    private val inventoryCommand: InventoryCommand,
    private val inventoryRepository: InventoryRepository,
    private val itemCatalog: ItemCatalog,
    private val classCatalog: ClassCatalog,
    private val raceCatalog: RaceCatalog,
    private val roomItemManager: RoomItemManager
) {
    private val logger = LoggerFactory.getLogger(AdminCommand::class.java)

    suspend fun execute(session: PlayerSession, rawMessage: String) {
        val parts = rawMessage.trimStart('/').split(" ")
        val command = parts[0].lowercase()
        val args = parts.drop(1)

        when (command) {
            "grantxp" -> handleGrantXp(session, args)
            "setlevel" -> handleSetLevel(session, args)
            "heal" -> handleHeal(session, args)
            "teleport" -> handleTeleport(session, args)
            "spawn" -> handleSpawn(session, args)
            "kill" -> handleKill(session, args)
            "setstat" -> handleSetStat(session, args)
            "grantcp" -> handleGrantCp(session, args)
            "grantitem" -> handleGrantItem(session, args)
            "broadcast" -> handleBroadcast(session, args)
            "godmode" -> handleGodMode(session)
            "help" -> handleHelp(session)
            else -> session.send(ServerMessage.SystemMessage("Unknown admin command: /$command. Type /help for a list."))
        }
    }

    private fun resolveTarget(session: PlayerSession, playerName: String?): PlayerSession? {
        if (playerName == null) return session
        return sessionManager.getSession(playerName)
    }

    private suspend fun handleGrantXp(session: PlayerSession, args: List<String>) {
        if (args.isEmpty()) {
            session.send(ServerMessage.SystemMessage("Usage: /grantxp <amount> [player]"))
            return
        }
        val amount = args[0].toLongOrNull()
        if (amount == null || amount <= 0) {
            session.send(ServerMessage.SystemMessage("Invalid XP amount."))
            return
        }
        val target = resolveTarget(session, args.getOrNull(1))
        if (target == null) {
            session.send(ServerMessage.SystemMessage("Player not found."))
            return
        }
        val player = target.player ?: return
        val newXp = player.currentXp + amount
        target.player = player.copy(currentXp = newXp)
        playerRepository.savePlayerState(target.player!!)

        target.send(ServerMessage.XpGained(amount, newXp, player.xpToNextLevel))
        if (XpCalculator.isReadyToLevel(newXp, player.xpToNextLevel, player.level)) {
            target.send(ServerMessage.SystemMessage("You have enough experience to level up! Visit a trainer."))
        }
        session.send(ServerMessage.SystemMessage("Granted $amount XP to ${player.name}. Total: $newXp"))
        logger.info("Admin ${session.playerName} granted $amount XP to ${player.name}")
    }

    private suspend fun handleSetLevel(session: PlayerSession, args: List<String>) {
        if (args.isEmpty()) {
            session.send(ServerMessage.SystemMessage("Usage: /setlevel <level> [player]"))
            return
        }
        val level = args[0].toIntOrNull()
        if (level == null || level < 1 || level > 30) {
            session.send(ServerMessage.SystemMessage("Level must be 1-30."))
            return
        }
        val target = resolveTarget(session, args.getOrNull(1))
        if (target == null) {
            session.send(ServerMessage.SystemMessage("Player not found."))
            return
        }
        val player = target.player ?: return
        val classDef = classCatalog.getClass(player.characterClass)

        // Calculate cumulative CP for this level
        var totalCp = 0
        for (l in 2..level) {
            totalCp += XpCalculator.cpForLevel(l)
        }

        // Calculate HP/MP for this level (use max rolls for admin set)
        var maxHp = (classDef?.hpPerLevelMax ?: 10) + (player.stats.health / 10) * 4
        var maxMp = if ((classDef?.mpPerLevelMax ?: 0) > 0) (classDef?.mpPerLevelMax ?: 0) + (player.stats.willpower / 10) * 2 else 0
        for (l in 2..level) {
            maxHp += classDef?.hpPerLevelMax ?: 10
            maxMp += classDef?.mpPerLevelMax ?: 0
        }

        val xpToNext = XpCalculator.xpForLevel(level)

        target.player = player.copy(
            level = level,
            currentHp = maxHp,
            maxHp = maxHp,
            currentMp = maxMp,
            maxMp = maxMp,
            currentXp = 0,
            xpToNextLevel = xpToNext,
            totalCpEarned = totalCp,
            unspentCp = totalCp
        )
        playerRepository.savePlayerState(target.player!!)
        target.send(ServerMessage.LoginOk(target.player!!))
        session.send(ServerMessage.SystemMessage("Set ${player.name} to level $level (HP: $maxHp, MP: $maxMp, CP: $totalCp)."))
        logger.info("Admin ${session.playerName} set ${player.name} to level $level")
    }

    private suspend fun handleHeal(session: PlayerSession, args: List<String>) {
        val target = resolveTarget(session, args.getOrNull(0))
        if (target == null) {
            session.send(ServerMessage.SystemMessage("Player not found."))
            return
        }
        val player = target.player ?: return
        target.player = player.copy(currentHp = player.maxHp, currentMp = player.maxMp)
        playerRepository.savePlayerState(target.player!!)
        target.send(ServerMessage.SystemMessage("You have been fully healed!"))
        session.send(ServerMessage.SystemMessage("Healed ${player.name} to full HP/MP."))
        logger.info("Admin ${session.playerName} healed ${player.name}")
    }

    private suspend fun handleTeleport(session: PlayerSession, args: List<String>) {
        if (args.isEmpty()) {
            session.send(ServerMessage.SystemMessage("Usage: /teleport <roomId> [player]"))
            return
        }
        val roomId = args[0]
        val room = worldGraph.getRoom(roomId)
        if (room == null) {
            session.send(ServerMessage.SystemMessage("Room '$roomId' not found."))
            return
        }
        val target = resolveTarget(session, args.getOrNull(1))
        if (target == null) {
            session.send(ServerMessage.SystemMessage("Player not found."))
            return
        }
        val player = target.player ?: return
        val playerName = target.playerName ?: return
        val oldRoomId = target.currentRoomId

        // Broadcast leave from old room
        if (oldRoomId != null) {
            sessionManager.broadcastToRoom(
                oldRoomId,
                ServerMessage.PlayerLeft(playerName, oldRoomId, Direction.NORTH),
                exclude = playerName
            )
        }

        // Update position
        target.currentRoomId = roomId
        target.player = player.copy(currentRoomId = roomId)
        playerRepository.savePlayerState(target.player!!)

        // Broadcast enter to new room
        sessionManager.broadcastToRoom(
            roomId,
            ServerMessage.PlayerEntered(playerName, roomId),
            exclude = playerName
        )

        // Send room info to teleported player
        val playersInRoom = sessionManager.getVisiblePlayerNamesInRoom(roomId).filter { it != playerName }
        val npcsInRoom = npcManager.getNpcsInRoom(roomId)
        target.send(ServerMessage.RoomInfo(room, playersInRoom, npcsInRoom))

        val mapRooms = worldGraph.getRoomsNear(roomId).map { mapRoom ->
            mapRoom.copy(
                hasPlayers = sessionManager.getPlayerNamesInRoom(mapRoom.id).isNotEmpty(),
                hasNpcs = npcManager.getNpcsInRoom(mapRoom.id).isNotEmpty()
            )
        }
        target.send(ServerMessage.MapData(mapRooms, roomId))

        // Send ground items
        val groundItems = roomItemManager.getGroundItems(roomId)
        val groundCoins = roomItemManager.getGroundCoins(roomId)
        target.send(ServerMessage.RoomItemsUpdate(groundItems, groundCoins))

        session.send(ServerMessage.SystemMessage("Teleported ${playerName} to $roomId."))
        logger.info("Admin ${session.playerName} teleported $playerName to $roomId")
    }

    private suspend fun handleSpawn(session: PlayerSession, args: List<String>) {
        if (args.isEmpty()) {
            session.send(ServerMessage.SystemMessage("Usage: /spawn <npcTemplateId>"))
            return
        }
        val templateId = args[0]
        val roomId = session.currentRoomId
        if (roomId == null) {
            session.send(ServerMessage.SystemMessage("You are not in a room."))
            return
        }
        val spawned = npcManager.spawnAdminNpc(templateId, roomId)
        if (spawned == null) {
            session.send(ServerMessage.SystemMessage("NPC template '$templateId' not found."))
            return
        }
        sessionManager.broadcastToRoom(
            roomId,
            ServerMessage.NpcEntered(
                spawned.name, roomId, spawned.id, spawned.hostile,
                spawned.currentHp, spawned.maxHp, spawned = true, templateId = spawned.templateId
            )
        )
        session.send(ServerMessage.SystemMessage("Spawned ${spawned.name} (${spawned.id})."))
        logger.info("Admin ${session.playerName} spawned ${spawned.name} at $roomId")
    }

    private suspend fun handleKill(session: PlayerSession, args: List<String>) {
        val roomId = session.currentRoomId
        if (roomId == null) {
            session.send(ServerMessage.SystemMessage("You are not in a room."))
            return
        }
        val npcId = args.getOrNull(0) ?: session.selectedTargetId
        if (npcId == null) {
            session.send(ServerMessage.SystemMessage("Usage: /kill <npcId> or select a target first."))
            return
        }
        val npc = npcManager.getNpcState(npcId)
        if (npc == null) {
            session.send(ServerMessage.SystemMessage("NPC '$npcId' not found or already dead."))
            return
        }
        if (!npcManager.markDead(npcId)) {
            session.send(ServerMessage.SystemMessage("NPC already dead."))
            return
        }
        sessionManager.broadcastToRoom(
            npc.currentRoomId,
            ServerMessage.NpcDied(npcId, npc.name, session.playerName ?: "admin", npc.currentRoomId)
        )
        session.send(ServerMessage.SystemMessage("Killed ${npc.name} ($npcId)."))
        logger.info("Admin ${session.playerName} killed ${npc.name} ($npcId)")
    }

    private suspend fun handleSetStat(session: PlayerSession, args: List<String>) {
        if (args.size < 2) {
            session.send(ServerMessage.SystemMessage("Usage: /setstat <stat> <value> [player]"))
            return
        }
        val statName = args[0].lowercase()
        val value = args[1].toIntOrNull()
        if (value == null || value < 1) {
            session.send(ServerMessage.SystemMessage("Invalid stat value."))
            return
        }
        val target = resolveTarget(session, args.getOrNull(2))
        if (target == null) {
            session.send(ServerMessage.SystemMessage("Player not found."))
            return
        }
        val player = target.player ?: return
        val stats = player.stats
        val newStats = when (statName) {
            "strength", "str" -> stats.copy(strength = value)
            "agility", "agi" -> stats.copy(agility = value)
            "intellect", "int" -> stats.copy(intellect = value)
            "willpower", "wil" -> stats.copy(willpower = value)
            "health", "hea" -> stats.copy(health = value)
            "charm", "cha" -> stats.copy(charm = value)
            else -> {
                session.send(ServerMessage.SystemMessage("Unknown stat: $statName. Use: strength, agility, intellect, willpower, health, charm"))
                return
            }
        }

        // Recompute max HP/MP based on health/willpower thresholds
        val classDef = classCatalog.getClass(player.characterClass)
        val hpBase = (classDef?.hpPerLevelMax ?: 10) + (newStats.health / 10) * 4
        val mpBase = if ((classDef?.mpPerLevelMax ?: 0) > 0) (classDef?.mpPerLevelMax ?: 0) + (newStats.willpower / 10) * 2 else 0
        var maxHp = hpBase
        var maxMp = mpBase
        for (l in 2..player.level) {
            maxHp += classDef?.hpPerLevelMax ?: 10
            maxMp += classDef?.mpPerLevelMax ?: 0
        }

        target.player = player.copy(
            stats = newStats,
            maxHp = maxHp,
            currentHp = maxHp,
            maxMp = maxMp,
            currentMp = maxMp
        )
        playerRepository.savePlayerState(target.player!!)
        target.send(ServerMessage.LoginOk(target.player!!))
        session.send(ServerMessage.SystemMessage("Set ${player.name}'s $statName to $value. (HP: $maxHp, MP: $maxMp)"))
        logger.info("Admin ${session.playerName} set ${player.name}'s $statName to $value")
    }

    private suspend fun handleGrantCp(session: PlayerSession, args: List<String>) {
        if (args.isEmpty()) {
            session.send(ServerMessage.SystemMessage("Usage: /grantcp <amount> [player]"))
            return
        }
        val amount = args[0].toIntOrNull()
        if (amount == null || amount <= 0) {
            session.send(ServerMessage.SystemMessage("Invalid CP amount."))
            return
        }
        val target = resolveTarget(session, args.getOrNull(1))
        if (target == null) {
            session.send(ServerMessage.SystemMessage("Player not found."))
            return
        }
        val player = target.player ?: return
        target.player = player.copy(
            unspentCp = player.unspentCp + amount,
            totalCpEarned = player.totalCpEarned + amount
        )
        playerRepository.savePlayerState(target.player!!)
        target.send(ServerMessage.LoginOk(target.player!!))
        session.send(ServerMessage.SystemMessage("Granted $amount CP to ${player.name}. Unspent: ${target.player!!.unspentCp}"))
        logger.info("Admin ${session.playerName} granted $amount CP to ${player.name}")
    }

    private suspend fun handleGrantItem(session: PlayerSession, args: List<String>) {
        if (args.isEmpty()) {
            session.send(ServerMessage.SystemMessage("Usage: /grantitem <itemId> [quantity] [player]"))
            return
        }
        val itemId = args[0]
        if (itemCatalog.getItem(itemId) == null) {
            session.send(ServerMessage.SystemMessage("Item '$itemId' not found in catalog."))
            return
        }

        // Parse optional quantity and player name
        val qty: Int
        val playerArg: String?
        if (args.size >= 2) {
            val maybeQty = args[1].toIntOrNull()
            if (maybeQty != null) {
                qty = maybeQty
                playerArg = args.getOrNull(2)
            } else {
                qty = 1
                playerArg = args[1]
            }
        } else {
            qty = 1
            playerArg = null
        }

        val target = resolveTarget(session, playerArg)
        if (target == null) {
            session.send(ServerMessage.SystemMessage("Player not found."))
            return
        }
        val playerName = target.playerName ?: return
        val added = inventoryRepository.addItem(playerName, itemId, qty)
        if (!added) {
            session.send(ServerMessage.SystemMessage("Failed to add item."))
            return
        }
        inventoryCommand.sendInventoryUpdate(target)
        session.send(ServerMessage.SystemMessage("Granted ${qty}x $itemId to $playerName."))
        logger.info("Admin ${session.playerName} granted ${qty}x $itemId to $playerName")
    }

    private suspend fun handleBroadcast(session: PlayerSession, args: List<String>) {
        if (args.isEmpty()) {
            session.send(ServerMessage.SystemMessage("Usage: /broadcast <message>"))
            return
        }
        val message = args.joinToString(" ")
        sessionManager.broadcastToAll(ServerMessage.SystemMessage("[BROADCAST] $message"))
        logger.info("Admin ${session.playerName} broadcast: $message")
    }

    private suspend fun handleGodMode(session: PlayerSession) {
        session.godMode = !session.godMode
        val status = if (session.godMode) "ENABLED" else "DISABLED"
        session.send(ServerMessage.SystemMessage("God mode $status."))
        logger.info("Admin ${session.playerName} toggled god mode: $status")
    }

    private suspend fun handleHelp(session: PlayerSession) {
        val help = buildString {
            appendLine("=== Admin Commands ===")
            appendLine("/grantxp <amount> [player] - Grant XP")
            appendLine("/setlevel <1-30> [player] - Set level (full heal + CP)")
            appendLine("/heal [player] - Full heal HP/MP")
            appendLine("/teleport <roomId> [player] - Teleport to room")
            appendLine("/spawn <npcTemplateId> - Spawn NPC in your room")
            appendLine("/kill [npcId] - Kill targeted or specified NPC")
            appendLine("/setstat <stat> <value> [player] - Set stat value")
            appendLine("/grantcp <amount> [player] - Grant CP")
            appendLine("/grantitem <itemId> [qty] [player] - Grant item")
            appendLine("/broadcast <message> - Message all players")
            appendLine("/godmode - Toggle invincibility")
        }
        session.send(ServerMessage.SystemMessage(help))
    }
}

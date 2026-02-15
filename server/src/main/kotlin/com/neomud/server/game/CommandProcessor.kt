package com.neomud.server.game

import com.neomud.server.game.commands.AttackCommand
import com.neomud.server.game.commands.BackstabCommand
import com.neomud.server.game.commands.HideCommand
import com.neomud.server.game.commands.InventoryCommand
import com.neomud.server.game.commands.LookCommand
import com.neomud.server.game.commands.MoveCommand
import com.neomud.server.game.commands.PickupCommand
import com.neomud.server.game.commands.SayCommand
import com.neomud.server.game.commands.SpellCommand
import com.neomud.server.game.commands.TrainerCommand
import com.neomud.server.game.commands.VendorCommand
import com.neomud.server.game.inventory.LootService
import com.neomud.server.game.inventory.RoomItemManager
import com.neomud.server.game.npc.NpcManager
import com.neomud.server.persistence.repository.PlayerRepository
import com.neomud.server.session.PlayerSession
import com.neomud.server.session.SessionManager
import com.neomud.server.world.ClassCatalog
import com.neomud.server.world.ItemCatalog
import com.neomud.server.world.LootTableCatalog
import com.neomud.server.world.RaceCatalog
import com.neomud.server.world.SkillCatalog
import com.neomud.server.world.SpellCatalog
import com.neomud.server.world.WorldGraph
import com.neomud.shared.protocol.ClientMessage
import com.neomud.shared.protocol.ServerMessage
import org.slf4j.LoggerFactory

class CommandProcessor(
    private val worldGraph: WorldGraph,
    private val sessionManager: SessionManager,
    private val npcManager: NpcManager,
    private val playerRepository: PlayerRepository,
    private val classCatalog: ClassCatalog,
    private val itemCatalog: ItemCatalog,
    private val skillCatalog: SkillCatalog,
    private val raceCatalog: RaceCatalog,
    private val inventoryCommand: InventoryCommand,
    private val pickupCommand: PickupCommand,
    private val roomItemManager: RoomItemManager,
    private val trainerCommand: TrainerCommand,
    private val spellCommand: SpellCommand,
    private val spellCatalog: SpellCatalog,
    private val vendorCommand: VendorCommand,
    private val lootService: LootService,
    private val lootTableCatalog: LootTableCatalog
) {
    private val logger = LoggerFactory.getLogger(CommandProcessor::class.java)
    private val moveCommand = MoveCommand(worldGraph, sessionManager, npcManager, playerRepository, roomItemManager)
    private val lookCommand = LookCommand(worldGraph, sessionManager, npcManager, roomItemManager)
    private val sayCommand = SayCommand(sessionManager)
    private val attackCommand = AttackCommand(npcManager)
    private val hideCommand = HideCommand(sessionManager, npcManager)
    private val backstabCommand = BackstabCommand(npcManager, sessionManager, playerRepository, lootService, lootTableCatalog, roomItemManager)

    suspend fun sendCatalogSync(session: PlayerSession) {
        session.send(ServerMessage.ClassCatalogSync(classCatalog.getAllClasses()))
        session.send(ServerMessage.ItemCatalogSync(itemCatalog.getAllItems()))
        session.send(ServerMessage.SkillCatalogSync(skillCatalog.getAllSkills()))
        session.send(ServerMessage.RaceCatalogSync(raceCatalog.getAllRaces()))
        session.send(ServerMessage.SpellCatalogSync(spellCatalog.getAllSpells()))
    }

    suspend fun process(session: PlayerSession, message: ClientMessage) {
        when (message) {
            is ClientMessage.Register -> handleRegister(session, message)
            is ClientMessage.Login -> handleLogin(session, message)
            is ClientMessage.Move -> {
                requireAuth(session) { moveCommand.execute(session, message.direction) }
            }
            is ClientMessage.Look -> {
                requireAuth(session) { lookCommand.execute(session) }
            }
            is ClientMessage.Say -> {
                requireAuth(session) { sayCommand.execute(session, message.message) }
            }
            is ClientMessage.AttackToggle -> {
                requireAuth(session) { attackCommand.handleToggle(session, message.enabled) }
            }
            is ClientMessage.SelectTarget -> {
                requireAuth(session) { attackCommand.handleSelectTarget(session, message.npcId) }
            }
            is ClientMessage.Ping -> {
                session.send(ServerMessage.Pong)
            }
            is ClientMessage.ViewInventory -> {
                requireAuth(session) { inventoryCommand.handleViewInventory(session) }
            }
            is ClientMessage.EquipItem -> {
                requireAuth(session) { inventoryCommand.handleEquipItem(session, message.itemId, message.slot) }
            }
            is ClientMessage.UnequipItem -> {
                requireAuth(session) { inventoryCommand.handleUnequipItem(session, message.slot) }
            }
            is ClientMessage.UseItem -> {
                requireAuth(session) { inventoryCommand.handleUseItem(session, message.itemId) }
            }
            is ClientMessage.PickupItem -> {
                requireAuth(session) { pickupCommand.handlePickupItem(session, message.itemId, message.quantity) }
            }
            is ClientMessage.PickupCoins -> {
                requireAuth(session) { pickupCommand.handlePickupCoins(session, message.coinType) }
            }
            is ClientMessage.HideToggle -> {
                requireAuth(session) { hideCommand.handleToggle(session, message.enabled) }
            }
            is ClientMessage.UseSkill -> {
                requireAuth(session) {
                    when (message.skillId.uppercase()) {
                        "BACKSTAB" -> backstabCommand.execute(session, message.targetId)
                        else -> session.send(ServerMessage.SystemMessage("Unknown skill: ${message.skillId}"))
                    }
                }
            }
            is ClientMessage.InteractTrainer -> {
                requireAuth(session) { trainerCommand.handleInteract(session) }
            }
            is ClientMessage.TrainLevelUp -> {
                requireAuth(session) { trainerCommand.handleLevelUp(session) }
            }
            is ClientMessage.TrainStat -> {
                requireAuth(session) { trainerCommand.handleTrainStat(session, message.stat, message.points) }
            }
            is ClientMessage.CastSpell -> {
                requireAuth(session) { spellCommand.execute(session, message.spellId, message.targetId) }
            }
            is ClientMessage.InteractVendor -> {
                requireAuth(session) { vendorCommand.handleInteract(session) }
            }
            is ClientMessage.BuyItem -> {
                requireAuth(session) { vendorCommand.handleBuy(session, message.itemId, message.quantity) }
            }
            is ClientMessage.SellItem -> {
                requireAuth(session) { vendorCommand.handleSell(session, message.itemId, message.quantity) }
            }
        }
    }

    private suspend fun handleRegister(session: PlayerSession, msg: ClientMessage.Register) {
        if (session.isAuthenticated) {
            session.send(ServerMessage.AuthError("Already logged in"))
            return
        }

        val nameRegex = Regex("^[a-zA-Z0-9_]{3,20}$")
        if (!nameRegex.matches(msg.username)) {
            session.send(ServerMessage.AuthError("Username must be 3-20 alphanumeric characters or underscores."))
            return
        }
        if (msg.password.length < 4 || msg.password.length > 64) {
            session.send(ServerMessage.AuthError("Password must be 4-64 characters."))
            return
        }
        if (!Regex("^[a-zA-Z][a-zA-Z0-9_ ]{1,19}$").matches(msg.characterName)) {
            session.send(ServerMessage.AuthError("Character name must be 2-20 characters, start with a letter, and contain only letters, numbers, spaces, or underscores."))
            return
        }

        val result = playerRepository.createPlayer(
            username = msg.username,
            password = msg.password,
            characterName = msg.characterName,
            characterClass = msg.characterClass,
            race = msg.race,
            allocatedStats = msg.allocatedStats,
            spawnRoomId = worldGraph.defaultSpawnRoom,
            classCatalog = classCatalog,
            raceCatalog = raceCatalog
        )

        result.fold(
            onSuccess = {
                session.send(ServerMessage.RegisterOk)
                logger.info("Player registered: ${msg.characterName}")
            },
            onFailure = {
                session.send(ServerMessage.AuthError(it.message ?: "Registration failed"))
            }
        )
    }

    private suspend fun handleLogin(session: PlayerSession, msg: ClientMessage.Login) {
        if (session.isAuthenticated) {
            session.send(ServerMessage.AuthError("Already logged in"))
            return
        }

        if (sessionManager.isLoggedIn(msg.username)) {
            session.send(ServerMessage.AuthError("Account already logged in"))
            return
        }

        val result = playerRepository.authenticate(msg.username, msg.password)

        result.fold(
            onSuccess = { player ->
                session.player = player
                session.playerName = player.name
                session.currentRoomId = player.currentRoomId
                sessionManager.addSession(player.name, session)

                session.send(ServerMessage.LoginOk(player))

                // Send initial room info
                val room = worldGraph.getRoom(player.currentRoomId)
                if (room != null) {
                    val playersInRoom = sessionManager.getVisiblePlayerNamesInRoom(player.currentRoomId)
                        .filter { it != player.name }
                    val npcsInRoom = npcManager.getNpcsInRoom(player.currentRoomId)
                    session.send(ServerMessage.RoomInfo(room, playersInRoom, npcsInRoom))

                    val mapRooms = worldGraph.getRoomsNear(player.currentRoomId).map { mapRoom ->
                        mapRoom.copy(
                            hasPlayers = sessionManager.getPlayerNamesInRoom(mapRoom.id).isNotEmpty(),
                            hasNpcs = npcManager.getNpcsInRoom(mapRoom.id).isNotEmpty()
                        )
                    }
                    session.send(ServerMessage.MapData(mapRooms, player.currentRoomId))

                    // Broadcast to others in room
                    sessionManager.broadcastToRoom(
                        player.currentRoomId,
                        ServerMessage.PlayerEntered(player.name, player.currentRoomId),
                        exclude = player.name
                    )
                }

                // Send initial inventory
                inventoryCommand.sendInventoryUpdate(session)

                // Send ground items for current room
                val groundItems = roomItemManager.getGroundItems(player.currentRoomId)
                val groundCoins = roomItemManager.getGroundCoins(player.currentRoomId)
                session.send(ServerMessage.RoomItemsUpdate(groundItems, groundCoins))

                logger.info("Player logged in: ${player.name}")
            },
            onFailure = {
                session.send(ServerMessage.AuthError(it.message ?: "Login failed"))
            }
        )
    }

    private suspend inline fun requireAuth(session: PlayerSession, block: () -> Unit) {
        if (!session.isAuthenticated) {
            session.send(ServerMessage.Error("You must log in first"))
            return
        }
        block()
    }
}

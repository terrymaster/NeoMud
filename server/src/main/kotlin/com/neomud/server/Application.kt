package com.neomud.server

import com.neomud.server.game.CommandProcessor
import com.neomud.server.game.GameLoop
import com.neomud.server.game.MovementTrailManager
import com.neomud.server.game.combat.CombatManager
import com.neomud.server.game.commands.InventoryCommand
import com.neomud.server.game.commands.PickupCommand
import com.neomud.server.game.commands.SpellCommand
import com.neomud.server.game.commands.TrainerCommand
import com.neomud.server.game.commands.VendorCommand
import com.neomud.server.game.inventory.EquipmentService
import com.neomud.server.game.inventory.LootService
import com.neomud.server.game.inventory.RoomItemManager
import com.neomud.server.game.npc.NpcManager
import com.neomud.server.persistence.DatabaseFactory
import com.neomud.server.persistence.repository.CoinRepository
import com.neomud.server.persistence.repository.DiscoveryRepository
import com.neomud.server.persistence.repository.InventoryRepository
import com.neomud.server.persistence.repository.PlayerRepository
import com.neomud.server.plugins.configureRouting
import com.neomud.server.plugins.configureWebSockets
import com.neomud.server.session.SessionManager
import com.neomud.server.world.NmdBundleDataSource
import com.neomud.server.world.WorldDataSource
import com.neomud.server.world.WorldLoader
import io.ktor.server.application.*
import com.neomud.shared.NeoMudVersion
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.io.File
import java.net.BindException
import java.net.ServerSocket
import java.util.zip.ZipFile
import com.neomud.server.game.GameConfig
import kotlin.system.exitProcess

private val logger = LoggerFactory.getLogger("NeoMud")
private val PORT = GameConfig.Server.PORT

fun checkPortAvailable(port: Int): Boolean {
    return try {
        ServerSocket(port).use { true }
    } catch (e: BindException) {
        false
    }
}

fun main() {
    logger.info("Starting ${NeoMudVersion.DISPLAY}")

    if (!checkPortAvailable(PORT)) {
        logger.error("Port $PORT is already in use. Is another NeoMud server running? Exiting.")
        exitProcess(1)
    }

    val worldFile = System.getenv("NEOMUD_WORLD")
        ?: "build/worlds/default-world.nmd"

    embeddedServer(Netty, port = PORT, host = "0.0.0.0") {
        module(worldFile = worldFile)
    }.start(wait = true)
}

fun Application.module(jdbcUrl: String = "jdbc:sqlite:neomud.db", worldFile: String = "build/worlds/default-world.nmd", adminUsernamesOverride: Set<String>? = null) {
    // Initialize database
    DatabaseFactory.init(jdbcUrl)

    // Load world from .nmd bundle
    val file = File(worldFile)
    if (!file.exists()) {
        error("World bundle not found at $worldFile. Run './gradlew packageWorld' or set NEOMUD_WORLD to a valid .nmd file.")
    }
    logger.info("Loading world from bundle: $worldFile")
    val dataSource: WorldDataSource = NmdBundleDataSource(ZipFile(file))

    // Load world
    val loadResult = WorldLoader.load(dataSource)
    val worldGraph = loadResult.worldGraph
    val classCatalog = loadResult.classCatalog
    val itemCatalog = loadResult.itemCatalog
    val lootTableCatalog = loadResult.lootTableCatalog
    val skillCatalog = loadResult.skillCatalog
    val raceCatalog = loadResult.raceCatalog
    val spellCatalog = loadResult.spellCatalog
    val pcSpriteCatalog = loadResult.pcSpriteCatalog
    val npcManager = NpcManager(worldGraph, loadResult.zoneSpawnConfigs, loadResult.roomMaxHostileNpcs)
    npcManager.loadNpcs(loadResult.npcDataList)

    // Create dependencies
    val sessionManager = SessionManager()
    val playerRepository = PlayerRepository()
    val inventoryRepository = InventoryRepository(itemCatalog)
    val coinRepository = CoinRepository()
    val discoveryRepository = DiscoveryRepository()
    val lootService = LootService(itemCatalog)
    val equipmentService = EquipmentService(inventoryRepository, itemCatalog)
    val roomItemManager = RoomItemManager()
    val inventoryCommand = InventoryCommand(inventoryRepository, itemCatalog, coinRepository, worldGraph, sessionManager)
    val pickupCommand = PickupCommand(roomItemManager, inventoryRepository, coinRepository, itemCatalog, sessionManager)
    val movementTrailManager = MovementTrailManager()
    val spellCommand = SpellCommand(spellCatalog, classCatalog, npcManager, sessionManager, playerRepository)
    val combatManager = CombatManager(npcManager, sessionManager, worldGraph, equipmentService, skillCatalog, spellCommand, spellCatalog, movementTrailManager)
    val trainerCommand = TrainerCommand(classCatalog, raceCatalog, playerRepository, sessionManager, npcManager)
    val vendorCommand = VendorCommand(npcManager, itemCatalog, inventoryRepository, coinRepository, inventoryCommand, sessionManager, skillCatalog)
    val adminUsernames = adminUsernamesOverride ?: (System.getenv("NEOMUD_ADMINS")
        ?.split(",")
        ?.map { it.trim().lowercase() }
        ?.filter { it.isNotEmpty() }
        ?.toSet()
        ?: emptySet())
    if (adminUsernames.isNotEmpty()) {
        logger.info("Admin usernames: $adminUsernames")
    }

    val commandProcessor = CommandProcessor(
        worldGraph, sessionManager, npcManager, playerRepository,
        classCatalog, itemCatalog, skillCatalog, raceCatalog, inventoryCommand, pickupCommand, roomItemManager,
        trainerCommand, spellCommand, spellCatalog, vendorCommand, lootService, lootTableCatalog,
        inventoryRepository, coinRepository, discoveryRepository, adminUsernames, movementTrailManager,
        pcSpriteCatalog
    )
    val gameLoop = GameLoop(sessionManager, npcManager, combatManager, worldGraph, lootService, lootTableCatalog, roomItemManager, playerRepository, skillCatalog, classCatalog, movementTrailManager)
    commandProcessor.setGameLoop(gameLoop)

    // Install plugins
    configureWebSockets()
    configureRouting(sessionManager, commandProcessor, playerRepository, discoveryRepository, dataSource)

    // Launch game loop
    launch {
        gameLoop.run()

        // Game loop exited — graceful shutdown
        logger.info("Game loop ended. Saving player states and shutting down...")
        for (session in sessionManager.getAllAuthenticatedSessions()) {
            try {
                session.player?.let { playerRepository.savePlayerState(it) }
            } catch (e: Exception) {
                logger.error("Failed to save player ${session.playerName}: ${e.message}")
            }
            try {
                session.webSocketSession.outgoing.send(
                    io.ktor.websocket.Frame.Close(io.ktor.websocket.CloseReason(
                        io.ktor.websocket.CloseReason.Codes.NORMAL,
                        "Server shutting down"
                    ))
                )
            } catch (_: Exception) { }
        }
        logger.info("All players saved. Server exiting.")
        exitProcess(0)
    }
}

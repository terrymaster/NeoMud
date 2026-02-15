package com.neomud.server

import com.neomud.server.game.CommandProcessor
import com.neomud.server.game.GameLoop
import com.neomud.server.game.combat.CombatManager
import com.neomud.server.game.commands.InventoryCommand
import com.neomud.server.game.commands.PickupCommand
import com.neomud.server.game.commands.SpellCommand
import com.neomud.server.game.commands.TrainerCommand
import com.neomud.server.game.inventory.EquipmentService
import com.neomud.server.game.inventory.LootService
import com.neomud.server.game.inventory.RoomItemManager
import com.neomud.server.game.npc.NpcManager
import com.neomud.server.persistence.DatabaseFactory
import com.neomud.server.persistence.repository.CoinRepository
import com.neomud.server.persistence.repository.InventoryRepository
import com.neomud.server.persistence.repository.PlayerRepository
import com.neomud.server.plugins.configureRouting
import com.neomud.server.plugins.configureWebSockets
import com.neomud.server.session.SessionManager
import com.neomud.server.world.WorldLoader
import io.ktor.server.application.*
import com.neomud.shared.NeoMudVersion
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.net.BindException
import java.net.ServerSocket
import kotlin.system.exitProcess

private val logger = LoggerFactory.getLogger("NeoMud")
private const val PORT = 8080

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

    embeddedServer(Netty, port = PORT, host = "0.0.0.0") {
        module()
    }.start(wait = true)
}

fun Application.module(jdbcUrl: String = "jdbc:sqlite:neomud.db") {
    // Initialize database
    DatabaseFactory.init(jdbcUrl)

    // Load world
    val loadResult = WorldLoader.load()
    val worldGraph = loadResult.worldGraph
    val classCatalog = loadResult.classCatalog
    val itemCatalog = loadResult.itemCatalog
    val lootTableCatalog = loadResult.lootTableCatalog
    val promptTemplateCatalog = loadResult.promptTemplateCatalog
    val skillCatalog = loadResult.skillCatalog
    val raceCatalog = loadResult.raceCatalog
    val spellCatalog = loadResult.spellCatalog
    val npcManager = NpcManager(worldGraph)
    npcManager.loadNpcs(loadResult.npcDataList)

    // Create dependencies
    val sessionManager = SessionManager()
    val playerRepository = PlayerRepository()
    val inventoryRepository = InventoryRepository(itemCatalog)
    val coinRepository = CoinRepository()
    val lootService = LootService(itemCatalog)
    val equipmentService = EquipmentService(inventoryRepository, itemCatalog)
    val roomItemManager = RoomItemManager()
    val inventoryCommand = InventoryCommand(inventoryRepository, itemCatalog, coinRepository)
    val pickupCommand = PickupCommand(roomItemManager, inventoryRepository, coinRepository, itemCatalog, sessionManager)
    val combatManager = CombatManager(npcManager, sessionManager, worldGraph, equipmentService)
    val trainerCommand = TrainerCommand(classCatalog, raceCatalog, playerRepository, sessionManager, npcManager)
    val spellCommand = SpellCommand(spellCatalog, classCatalog, npcManager, sessionManager, playerRepository)
    val commandProcessor = CommandProcessor(
        worldGraph, sessionManager, npcManager, playerRepository,
        classCatalog, itemCatalog, skillCatalog, raceCatalog, inventoryCommand, pickupCommand, roomItemManager,
        trainerCommand, spellCommand, spellCatalog
    )
    val gameLoop = GameLoop(sessionManager, npcManager, combatManager, worldGraph, lootService, lootTableCatalog, roomItemManager, playerRepository)

    // Install plugins
    configureWebSockets()
    configureRouting(sessionManager, commandProcessor, playerRepository)

    // Launch game loop
    launch {
        gameLoop.run()
    }
}

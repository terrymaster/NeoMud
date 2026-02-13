package com.neomud.server

import com.neomud.server.game.CommandProcessor
import com.neomud.server.game.GameLoop
import com.neomud.server.game.combat.CombatManager
import com.neomud.server.game.commands.InventoryCommand
import com.neomud.server.game.inventory.EquipmentService
import com.neomud.server.game.inventory.LootService
import com.neomud.server.game.npc.NpcManager
import com.neomud.server.persistence.DatabaseFactory
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
    val npcManager = NpcManager(worldGraph)
    npcManager.loadNpcs(loadResult.npcDataList)

    // Create dependencies
    val sessionManager = SessionManager()
    val playerRepository = PlayerRepository()
    val inventoryRepository = InventoryRepository(itemCatalog)
    val lootService = LootService(inventoryRepository, itemCatalog)
    val equipmentService = EquipmentService(inventoryRepository, itemCatalog)
    val inventoryCommand = InventoryCommand(inventoryRepository, itemCatalog)
    val combatManager = CombatManager(npcManager, sessionManager, worldGraph, equipmentService)
    val commandProcessor = CommandProcessor(
        worldGraph, sessionManager, npcManager, playerRepository,
        classCatalog, itemCatalog, inventoryCommand
    )
    val gameLoop = GameLoop(sessionManager, npcManager, combatManager, worldGraph, lootService, lootTableCatalog)

    // Install plugins
    configureWebSockets()
    configureRouting(sessionManager, commandProcessor)

    // Launch game loop
    launch {
        gameLoop.run()
    }
}

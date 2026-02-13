package com.neomud.server

import com.neomud.server.game.CommandProcessor
import com.neomud.server.game.GameLoop
import com.neomud.server.game.combat.CombatManager
import com.neomud.server.game.npc.NpcManager
import com.neomud.server.persistence.DatabaseFactory
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

private val logger = LoggerFactory.getLogger("NeoMud")

fun main() {
    logger.info("Starting ${NeoMudVersion.DISPLAY}")
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        module()
    }.start(wait = true)
}

fun Application.module(jdbcUrl: String = "jdbc:sqlite:neomud.db") {
    // Initialize database
    DatabaseFactory.init(jdbcUrl)

    // Load world
    val loadResult = WorldLoader.load()
    val worldGraph = loadResult.worldGraph
    val npcManager = NpcManager(worldGraph)
    npcManager.loadNpcs(loadResult.npcDataList)

    // Create dependencies
    val sessionManager = SessionManager()
    val playerRepository = PlayerRepository()
    val combatManager = CombatManager(npcManager, sessionManager, worldGraph)
    val commandProcessor = CommandProcessor(worldGraph, sessionManager, npcManager, playerRepository)
    val gameLoop = GameLoop(sessionManager, npcManager, combatManager, worldGraph)

    // Install plugins
    configureWebSockets()
    configureRouting(sessionManager, commandProcessor)

    // Launch game loop
    launch {
        gameLoop.run()
    }
}

package com.neomud.server

import com.neomud.server.game.CommandProcessor
import com.neomud.server.game.GameLoop
import com.neomud.server.game.npc.NpcManager
import com.neomud.server.persistence.DatabaseFactory
import com.neomud.server.persistence.repository.PlayerRepository
import com.neomud.server.plugins.configureRouting
import com.neomud.server.plugins.configureWebSockets
import com.neomud.server.session.SessionManager
import com.neomud.server.world.WorldLoader
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.launch

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        module()
    }.start(wait = true)
}

fun Application.module() {
    // Initialize database
    DatabaseFactory.init()

    // Load world
    val loadResult = WorldLoader.load()
    val worldGraph = loadResult.worldGraph
    val npcManager = NpcManager(worldGraph)
    npcManager.loadNpcs(loadResult.npcDataList)

    // Create dependencies
    val sessionManager = SessionManager()
    val playerRepository = PlayerRepository()
    val commandProcessor = CommandProcessor(worldGraph, sessionManager, npcManager, playerRepository)
    val gameLoop = GameLoop(sessionManager, npcManager)

    // Install plugins
    configureWebSockets()
    configureRouting(sessionManager, commandProcessor)

    // Launch game loop
    launch {
        gameLoop.run()
    }
}

package com.neomud.server.plugins

import com.neomud.server.game.CommandProcessor
import com.neomud.server.persistence.repository.PlayerRepository
import com.neomud.server.session.PlayerSession
import com.neomud.server.session.SessionManager
import com.neomud.server.world.WorldDataSource
import com.neomud.shared.protocol.MessageSerializer
import com.neomud.shared.protocol.ServerMessage
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("Routing")

private val extensionToContentType = mapOf(
    "webp" to ContentType.Image.Any,
    "png" to ContentType.Image.PNG,
    "jpg" to ContentType.Image.JPEG,
    "jpeg" to ContentType.Image.JPEG,
    "ogg" to ContentType.parse("audio/ogg"),
    "mp3" to ContentType.parse("audio/mpeg"),
    "wav" to ContentType.parse("audio/wav"),
    "json" to ContentType.Application.Json
)

fun Application.configureRouting(
    sessionManager: SessionManager,
    commandProcessor: CommandProcessor,
    playerRepository: PlayerRepository,
    dataSource: WorldDataSource
) {
    routing {
        get("/assets/{path...}") {
            val path = call.parameters.getAll("path")?.joinToString("/") ?: ""
            val fullPath = "assets/$path"

            // Reject path traversal attempts
            if (".." in path) {
                call.respond(HttpStatusCode.BadRequest)
                return@get
            }

            val bytes = dataSource.readBytes(fullPath)
            if (bytes == null) {
                call.respond(HttpStatusCode.NotFound)
                return@get
            }

            val ext = path.substringAfterLast('.', "").lowercase()
            val contentType = extensionToContentType[ext] ?: ContentType.Application.OctetStream
            call.respondBytes(bytes, contentType)
        }

        get("/health") {
            call.respondText("OK", ContentType.Text.Plain)
        }

        webSocket("/game") {
            val session = PlayerSession(this)
            logger.info("New WebSocket connection")

            try {
                // Send catalog data before auth so registration screen can populate
                commandProcessor.sendCatalogSync(session)

                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val text = frame.readText()
                        try {
                            val message = MessageSerializer.decodeClientMessage(text)
                            if (!session.tryConsumeMessage()) {
                                session.send(ServerMessage.Error("Too many commands, slow down."))
                                continue
                            }
                            commandProcessor.process(session, message)
                        } catch (e: Exception) {
                            logger.error("Failed to process message: $text", e)
                            session.send(ServerMessage.Error("Invalid message format"))
                        }
                    }
                }
            } catch (e: Exception) {
                logger.info("WebSocket error: ${e.message}")
            } finally {
                val playerName = session.playerName
                if (playerName != null) {
                    val roomId = session.currentRoomId
                    // Save player state before removing session
                    val player = session.player
                    if (player != null) {
                        try {
                            playerRepository.savePlayerState(player)
                        } catch (e: Exception) {
                            logger.error("Failed to save player state on disconnect: ${e.message}")
                        }
                    }
                    sessionManager.removeSession(playerName)
                    if (roomId != null) {
                        sessionManager.broadcastToRoom(
                            roomId,
                            ServerMessage.SystemMessage("$playerName has disconnected.")
                        )
                    }
                    logger.info("Player $playerName disconnected")
                }
            }
        }
    }
}

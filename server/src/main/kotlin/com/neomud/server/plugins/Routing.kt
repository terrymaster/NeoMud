package com.neomud.server.plugins

import com.neomud.server.game.CommandProcessor
import com.neomud.server.session.PlayerSession
import com.neomud.server.session.SessionManager
import com.neomud.shared.protocol.ClientMessage
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

fun Application.configureRouting(
    sessionManager: SessionManager,
    commandProcessor: CommandProcessor
) {
    routing {
        get("/health") {
            call.respondText("OK", ContentType.Text.Plain)
        }

        webSocket("/game") {
            val session = PlayerSession(this)
            logger.info("New WebSocket connection")

            try {
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val text = frame.readText()
                        try {
                            val message = MessageSerializer.decodeClientMessage(text)
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

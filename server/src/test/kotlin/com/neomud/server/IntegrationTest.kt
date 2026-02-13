package com.neomud.server

import com.neomud.shared.model.Direction
import com.neomud.shared.protocol.ClientMessage
import com.neomud.shared.protocol.MessageSerializer
import com.neomud.shared.protocol.ServerMessage
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.ktor.websocket.*
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class IntegrationTest {

    private fun testDbUrl(): String {
        val tmpFile = File.createTempFile("neomud_test_", ".db")
        tmpFile.deleteOnExit()
        tmpFile.delete() // SQLite will create it
        return "jdbc:sqlite:${tmpFile.absolutePath}"
    }

    /** Consume the two catalog sync messages sent on connect */
    private suspend fun DefaultClientWebSocketSession.consumeCatalogSync() {
        val msg1 = receiveServerMessage()
        assertIs<ServerMessage.ClassCatalogSync>(msg1)
        val msg2 = receiveServerMessage()
        assertIs<ServerMessage.ItemCatalogSync>(msg2)
    }

    @Test
    fun testHealthEndpoint() = testApplication {
        application { module(jdbcUrl = testDbUrl()) }

        val response = client.get("/health")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("OK", response.bodyAsText())
    }

    @Test
    fun testRegisterAndLogin() = testApplication {
        application { module(jdbcUrl = testDbUrl()) }

        val wsClient = createClient {
            install(WebSockets)
        }

        wsClient.webSocket("/game") {
            consumeCatalogSync()

            // Register
            send(Frame.Text(MessageSerializer.encodeClientMessage(
                ClientMessage.Register("testuser", "testpass", "TestHero", "FIGHTER")
            )))

            val registerResponse = receiveServerMessage()
            assertIs<ServerMessage.RegisterOk>(registerResponse)
        }

        // Login in new session
        wsClient.webSocket("/game") {
            consumeCatalogSync()

            send(Frame.Text(MessageSerializer.encodeClientMessage(
                ClientMessage.Login("testuser", "testpass")
            )))

            val loginResponse = receiveServerMessage()
            assertIs<ServerMessage.LoginOk>(loginResponse)
            assertEquals("TestHero", loginResponse.player.name)
            assertEquals("FIGHTER", loginResponse.player.characterClass)
            assertEquals("town:square", loginResponse.player.currentRoomId)
            assertTrue(loginResponse.player.maxMp > 0, "maxMp should be > 0")
            assertEquals(loginResponse.player.maxMp, loginResponse.player.currentMp, "New character should have full MP")

            // Should receive RoomInfo
            val roomInfo = receiveServerMessage()
            assertIs<ServerMessage.RoomInfo>(roomInfo)
            assertEquals("Town Square", roomInfo.room.name)

            // Should receive MapData
            val mapData = receiveServerMessage()
            assertIs<ServerMessage.MapData>(mapData)
            assertEquals("town:square", mapData.playerRoomId)
            assertTrue(mapData.rooms.isNotEmpty())

            // Should receive InventoryUpdate
            val invUpdate = receiveServerMessage()
            assertIs<ServerMessage.InventoryUpdate>(invUpdate)

            // Should receive RoomItemsUpdate (ground items for current room)
            val roomItems = receiveServerMessage()
            assertIs<ServerMessage.RoomItemsUpdate>(roomItems)
        }
    }

    @Test
    fun testMoveCommand() = testApplication {
        application { module(jdbcUrl = testDbUrl()) }

        val wsClient = createClient {
            install(WebSockets)
        }

        // Register first
        wsClient.webSocket("/game") {
            consumeCatalogSync()
            send(Frame.Text(MessageSerializer.encodeClientMessage(
                ClientMessage.Register("mover", "pass123", "Mover", "ROGUE")
            )))
            receiveServerMessage() // RegisterOk
        }

        // Login and move
        wsClient.webSocket("/game") {
            consumeCatalogSync()
            send(Frame.Text(MessageSerializer.encodeClientMessage(
                ClientMessage.Login("mover", "pass123")
            )))
            receiveServerMessage() // LoginOk
            receiveServerMessage() // RoomInfo
            receiveServerMessage() // MapData
            receiveServerMessage() // InventoryUpdate
            receiveServerMessage() // RoomItemsUpdate

            // Move north to gate
            send(Frame.Text(MessageSerializer.encodeClientMessage(
                ClientMessage.Move(Direction.NORTH)
            )))

            val moveOk = receiveServerMessage()
            assertIs<ServerMessage.MoveOk>(moveOk)
            assertEquals(Direction.NORTH, moveOk.direction)
            assertEquals("North Gate", moveOk.room.name)

            val mapData = receiveServerMessage()
            assertIs<ServerMessage.MapData>(mapData)
            assertEquals("town:gate", mapData.playerRoomId)

            val roomItems = receiveServerMessage()
            assertIs<ServerMessage.RoomItemsUpdate>(roomItems)
        }
    }

    @Test
    fun testInvalidMove() = testApplication {
        application { module(jdbcUrl = testDbUrl()) }

        val wsClient = createClient {
            install(WebSockets)
        }

        // Register
        wsClient.webSocket("/game") {
            consumeCatalogSync()
            send(Frame.Text(MessageSerializer.encodeClientMessage(
                ClientMessage.Register("stuck", "pass123", "Stuck", "CLERIC")
            )))
            receiveServerMessage() // RegisterOk
        }

        // Login and try invalid move
        wsClient.webSocket("/game") {
            consumeCatalogSync()
            send(Frame.Text(MessageSerializer.encodeClientMessage(
                ClientMessage.Login("stuck", "pass123")
            )))
            receiveServerMessage() // LoginOk
            receiveServerMessage() // RoomInfo
            receiveServerMessage() // MapData
            receiveServerMessage() // InventoryUpdate
            receiveServerMessage() // RoomItemsUpdate

            // Town Square has no UP exit
            send(Frame.Text(MessageSerializer.encodeClientMessage(
                ClientMessage.Move(Direction.UP)
            )))

            val moveError = receiveServerMessage()
            assertIs<ServerMessage.MoveError>(moveError)
        }
    }

    @Test
    fun testUnauthenticatedAccess() = testApplication {
        application { module(jdbcUrl = testDbUrl()) }

        val wsClient = createClient {
            install(WebSockets)
        }

        wsClient.webSocket("/game") {
            consumeCatalogSync()
            // Try to move without logging in
            send(Frame.Text(MessageSerializer.encodeClientMessage(
                ClientMessage.Move(Direction.NORTH)
            )))

            val error = receiveServerMessage()
            assertIs<ServerMessage.Error>(error)
            assertTrue(error.message.contains("log in"))
        }
    }

    private suspend fun DefaultClientWebSocketSession.receiveServerMessage(): ServerMessage {
        val frame = incoming.receive()
        assertTrue(frame is Frame.Text, "Expected text frame")
        return MessageSerializer.decodeServerMessage(frame.readText())
    }
}

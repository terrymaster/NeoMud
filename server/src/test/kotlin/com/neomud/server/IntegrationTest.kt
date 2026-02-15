package com.neomud.server

import com.neomud.shared.model.Direction
import com.neomud.shared.model.Stats
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

    /** Consume the catalog sync messages sent on connect */
    private suspend fun DefaultClientWebSocketSession.consumeCatalogSync() {
        val msg1 = receiveServerMessage()
        assertIs<ServerMessage.ClassCatalogSync>(msg1)
        val msg2 = receiveServerMessage()
        assertIs<ServerMessage.ItemCatalogSync>(msg2)
        val msg3 = receiveServerMessage()
        assertIs<ServerMessage.SkillCatalogSync>(msg3)
        val msg4 = receiveServerMessage()
        assertIs<ServerMessage.RaceCatalogSync>(msg4)
        val msg5 = receiveServerMessage()
        assertIs<ServerMessage.SpellCatalogSync>(msg5)
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

            // Register — WARRIOR min: str=20,agi=12,int=8,wil=8,hlt=20,chm=8; +10 each = 60CP
            send(Frame.Text(MessageSerializer.encodeClientMessage(
                ClientMessage.Register("testuser", "testpass", "TestHero", "WARRIOR",
                    allocatedStats = Stats(strength = 30, agility = 22, intellect = 18, willpower = 18, health = 30, charm = 18))
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
            assertEquals("WARRIOR", loginResponse.player.characterClass)
            assertEquals("town:temple", loginResponse.player.currentRoomId)

            // Should receive RoomInfo
            val roomInfo = receiveServerMessage()
            assertIs<ServerMessage.RoomInfo>(roomInfo)
            assertEquals("Temple of the Dawn", roomInfo.room.name)

            // Should receive MapData
            val mapData = receiveServerMessage()
            assertIs<ServerMessage.MapData>(mapData)
            assertEquals("town:temple", mapData.playerRoomId)
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
                ClientMessage.Register("mover", "pass123", "Mover", "THIEF",
                    allocatedStats = Stats(strength = 20, agility = 30, intellect = 22, willpower = 18, health = 20, charm = 25))
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

            // Move north to town square (from temple)
            send(Frame.Text(MessageSerializer.encodeClientMessage(
                ClientMessage.Move(Direction.NORTH)
            )))

            val moveOk = receiveServerMessage()
            assertIs<ServerMessage.MoveOk>(moveOk)
            assertEquals(Direction.NORTH, moveOk.direction)
            assertEquals("Town Square", moveOk.room.name)

            val mapData = receiveServerMessage()
            assertIs<ServerMessage.MapData>(mapData)
            assertEquals("town:square", mapData.playerRoomId)

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
                ClientMessage.Register("stuck", "pass123", "Stuck", "CLERIC",
                    allocatedStats = Stats(strength = 25, agility = 20, intellect = 20, willpower = 28, health = 25, charm = 22))
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

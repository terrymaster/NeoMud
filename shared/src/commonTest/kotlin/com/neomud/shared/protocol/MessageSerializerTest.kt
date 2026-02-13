package com.neomud.shared.protocol

import com.neomud.shared.model.*
import kotlin.test.Test
import kotlin.test.assertEquals

class MessageSerializerTest {

    @Test
    fun testMoveMessageRoundTrip() {
        val original = ClientMessage.Move(Direction.NORTH)
        val json = MessageSerializer.encodeClientMessage(original)
        val decoded = MessageSerializer.decodeClientMessage(json)
        assertEquals(original, decoded)
    }

    @Test
    fun testLoginMessageRoundTrip() {
        val original = ClientMessage.Login("user1", "pass123")
        val json = MessageSerializer.encodeClientMessage(original)
        val decoded = MessageSerializer.decodeClientMessage(json)
        assertEquals(original, decoded)
    }

    @Test
    fun testRegisterMessageRoundTrip() {
        val original = ClientMessage.Register("user1", "pass123", "Gandalf", CharacterClass.WIZARD)
        val json = MessageSerializer.encodeClientMessage(original)
        val decoded = MessageSerializer.decodeClientMessage(json)
        assertEquals(original, decoded)
    }

    @Test
    fun testSayMessageRoundTrip() {
        val original = ClientMessage.Say("Hello, world!")
        val json = MessageSerializer.encodeClientMessage(original)
        val decoded = MessageSerializer.decodeClientMessage(json)
        assertEquals(original, decoded)
    }

    @Test
    fun testPingRoundTrip() {
        val original = ClientMessage.Ping
        val json = MessageSerializer.encodeClientMessage(original)
        val decoded = MessageSerializer.decodeClientMessage(json)
        assertEquals(original, decoded)
    }

    @Test
    fun testLookRoundTrip() {
        val original = ClientMessage.Look
        val json = MessageSerializer.encodeClientMessage(original)
        val decoded = MessageSerializer.decodeClientMessage(json)
        assertEquals(original, decoded)
    }

    @Test
    fun testRoomInfoRoundTrip() {
        val room = Room(
            id = "town:square",
            name = "Town Square",
            description = "A bustling town square.",
            exits = mapOf(Direction.NORTH to "town:gate", Direction.EAST to "town:market"),
            zoneId = "town",
            x = 0,
            y = 0
        )
        val npc = Npc("npc:guard", "Town Guard", "A stern guard.", "town:square", "patrol")
        val original = ServerMessage.RoomInfo(room, listOf("Gandalf"), listOf(npc))
        val json = MessageSerializer.encodeServerMessage(original)
        val decoded = MessageSerializer.decodeServerMessage(json)
        assertEquals(original, decoded)
    }

    @Test
    fun testHostileNpcRoundTrip() {
        val room = Room(
            id = "forest:path",
            name = "Forest Path",
            description = "A winding path.",
            exits = mapOf(Direction.NORTH to "forest:deep"),
            zoneId = "forest",
            x = 0,
            y = 3
        )
        val hostileNpc = Npc("npc:shadow_wolf", "Shadow Wolf", "A dark wolf.", "forest:path", "wander", hostile = true)
        val friendlyNpc = Npc("npc:guard", "Town Guard", "A stern guard.", "forest:path", "patrol", hostile = false)
        val original = ServerMessage.RoomInfo(room, emptyList(), listOf(hostileNpc, friendlyNpc))
        val json = MessageSerializer.encodeServerMessage(original)
        val decoded = MessageSerializer.decodeServerMessage(json)
        assertEquals(original, decoded)
    }

    @Test
    fun testMoveOkRoundTrip() {
        val room = Room("forest:edge", "Forest Edge", "Trees everywhere.", mapOf(Direction.SOUTH to "town:gate"), "forest", 0, 2)
        val original = ServerMessage.MoveOk(Direction.NORTH, room, emptyList(), emptyList())
        val json = MessageSerializer.encodeServerMessage(original)
        val decoded = MessageSerializer.decodeServerMessage(json)
        assertEquals(original, decoded)
    }

    @Test
    fun testMapDataRoundTrip() {
        val rooms = listOf(
            MapRoom("town:square", "Town Square", 0, 0, mapOf(Direction.NORTH to "town:gate"), hasPlayers = true),
            MapRoom("town:gate", "North Gate", 0, 1, mapOf(Direction.SOUTH to "town:square"))
        )
        val original = ServerMessage.MapData(rooms, "town:square")
        val json = MessageSerializer.encodeServerMessage(original)
        val decoded = MessageSerializer.decodeServerMessage(json)
        assertEquals(original, decoded)
    }

    @Test
    fun testPlayerEventsRoundTrip() {
        val entered = ServerMessage.PlayerEntered("Gandalf", "town:square")
        val left = ServerMessage.PlayerLeft("Frodo", "town:square", Direction.NORTH)

        assertEquals(entered, MessageSerializer.decodeServerMessage(MessageSerializer.encodeServerMessage(entered)))
        assertEquals(left, MessageSerializer.decodeServerMessage(MessageSerializer.encodeServerMessage(left)))
    }

    @Test
    fun testSystemMessagesRoundTrip() {
        val sys = ServerMessage.SystemMessage("Welcome to NeoMud!")
        val err = ServerMessage.Error("Something went wrong")
        val pong = ServerMessage.Pong

        assertEquals(sys, MessageSerializer.decodeServerMessage(MessageSerializer.encodeServerMessage(sys)))
        assertEquals(err, MessageSerializer.decodeServerMessage(MessageSerializer.encodeServerMessage(err)))
        assertEquals(pong, MessageSerializer.decodeServerMessage(MessageSerializer.encodeServerMessage(pong)))
    }

    @Test
    fun testMoveMessageJsonFormat() {
        val msg = ClientMessage.Move(Direction.NORTH)
        val json = MessageSerializer.encodeClientMessage(msg)
        assert(json.contains("\"type\":\"move\"")) { "Expected type discriminator in JSON: $json" }
        assert(json.contains("\"direction\":\"NORTH\"")) { "Expected direction in JSON: $json" }
    }

    @Test
    fun testDirectionOpposite() {
        assertEquals(Direction.SOUTH, Direction.NORTH.opposite())
        assertEquals(Direction.NORTH, Direction.SOUTH.opposite())
        assertEquals(Direction.WEST, Direction.EAST.opposite())
        assertEquals(Direction.EAST, Direction.WEST.opposite())
        assertEquals(Direction.DOWN, Direction.UP.opposite())
        assertEquals(Direction.UP, Direction.DOWN.opposite())
    }

    @Test
    fun testStatsMaxHitPoints() {
        val stats = Stats(strength = 16, dexterity = 12, constitution = 14, intelligence = 8, wisdom = 10)
        // 50 + (14 * 5) + (16 * 2) = 50 + 70 + 32 = 152
        assertEquals(152, stats.maxHitPoints)
    }

    @Test
    fun testCharacterClassBaseStats() {
        val fighter = CharacterClass.FIGHTER
        assertEquals(16, fighter.baseStats.strength)
        assertEquals(14, fighter.baseStats.constitution)

        val wizard = CharacterClass.WIZARD
        assertEquals(16, wizard.baseStats.intelligence)
        assertEquals(8, wizard.baseStats.strength)
    }
}

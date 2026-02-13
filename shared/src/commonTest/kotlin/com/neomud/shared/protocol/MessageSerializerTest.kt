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
        val original = ClientMessage.Register("user1", "pass123", "Gandalf", "WIZARD")
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
    fun testAttackToggleRoundTrip() {
        val original = ClientMessage.AttackToggle(true)
        val json = MessageSerializer.encodeClientMessage(original)
        val decoded = MessageSerializer.decodeClientMessage(json)
        assertEquals(original, decoded)
    }

    @Test
    fun testSelectTargetRoundTrip() {
        val original = ClientMessage.SelectTarget("npc:shadow_wolf")
        val json = MessageSerializer.encodeClientMessage(original)
        val decoded = MessageSerializer.decodeClientMessage(json)
        assertEquals(original, decoded)
    }

    @Test
    fun testSelectTargetNullRoundTrip() {
        val original = ClientMessage.SelectTarget(null)
        val json = MessageSerializer.encodeClientMessage(original)
        val decoded = MessageSerializer.decodeClientMessage(json)
        assertEquals(original, decoded)
    }

    @Test
    fun testCombatHitRoundTrip() {
        val original = ServerMessage.CombatHit("Hero", "Shadow Wolf", 12, 38, 50, isPlayerDefender = false)
        val json = MessageSerializer.encodeServerMessage(original)
        val decoded = MessageSerializer.decodeServerMessage(json)
        assertEquals(original, decoded)
    }

    @Test
    fun testCombatHitPlayerDefenderRoundTrip() {
        val original = ServerMessage.CombatHit("Shadow Wolf", "Hero", 8, 92, 100, isPlayerDefender = true)
        val json = MessageSerializer.encodeServerMessage(original)
        val decoded = MessageSerializer.decodeServerMessage(json)
        assertEquals(original, decoded)
    }

    @Test
    fun testNpcDiedRoundTrip() {
        val original = ServerMessage.NpcDied("npc:shadow_wolf", "Shadow Wolf", "Hero", "forest:path")
        val json = MessageSerializer.encodeServerMessage(original)
        val decoded = MessageSerializer.decodeServerMessage(json)
        assertEquals(original, decoded)
    }

    @Test
    fun testPlayerDiedRoundTrip() {
        val original = ServerMessage.PlayerDied("Shadow Wolf", "town:square", 100)
        val json = MessageSerializer.encodeServerMessage(original)
        val decoded = MessageSerializer.decodeServerMessage(json)
        assertEquals(original, decoded)
    }

    @Test
    fun testAttackModeUpdateRoundTrip() {
        val original = ServerMessage.AttackModeUpdate(true)
        val json = MessageSerializer.encodeServerMessage(original)
        val decoded = MessageSerializer.decodeServerMessage(json)
        assertEquals(original, decoded)
    }

    @Test
    fun testNpcEnteredWithCombatFieldsRoundTrip() {
        val original = ServerMessage.NpcEntered("Shadow Wolf", "forest:path", "npc:shadow_wolf", true, 45, 50)
        val json = MessageSerializer.encodeServerMessage(original)
        val decoded = MessageSerializer.decodeServerMessage(json)
        assertEquals(original, decoded)
    }

    @Test
    fun testNpcLeftWithIdRoundTrip() {
        val original = ServerMessage.NpcLeft("Shadow Wolf", "forest:path", Direction.NORTH, "npc:shadow_wolf")
        val json = MessageSerializer.encodeServerMessage(original)
        val decoded = MessageSerializer.decodeServerMessage(json)
        assertEquals(original, decoded)
    }

    @Test
    fun testNpcWithHpRoundTrip() {
        val npc = Npc("npc:wolf", "Wolf", "A grey wolf.", "forest:path", "wander", hostile = true, currentHp = 30, maxHp = 50)
        val room = Room("forest:path", "Forest Path", "A winding path.", mapOf(Direction.NORTH to "forest:deep"), "forest", 0, 3)
        val original = ServerMessage.RoomInfo(room, emptyList(), listOf(npc))
        val json = MessageSerializer.encodeServerMessage(original)
        val decoded = MessageSerializer.decodeServerMessage(json)
        assertEquals(original, decoded)
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
    fun testClassCatalogSyncRoundTrip() {
        val classes = listOf(
            CharacterClassDef("FIGHTER", "Fighter", "A master of martial combat", Stats(16, 12, 14, 10, 10)),
            CharacterClassDef("WIZARD", "Wizard", "A scholarly mage", Stats(8, 10, 12, 16, 14), mapOf("spellbook" to "true"))
        )
        val original = ServerMessage.ClassCatalogSync(classes)
        val json = MessageSerializer.encodeServerMessage(original)
        val decoded = MessageSerializer.decodeServerMessage(json)
        assertEquals(original, decoded)
    }

    @Test
    fun testItemCatalogSyncRoundTrip() {
        val items = listOf(
            Item("item:sword", "Sword", "A blade.", "weapon", "weapon", damageBonus = 3, damageRange = 6),
            Item("item:potion", "Potion", "Heals.", "consumable", useEffect = "heal:25", stackable = true, maxStack = 10)
        )
        val original = ServerMessage.ItemCatalogSync(items)
        val json = MessageSerializer.encodeServerMessage(original)
        val decoded = MessageSerializer.decodeServerMessage(json)
        assertEquals(original, decoded)
    }

    @Test
    fun testViewInventoryRoundTrip() {
        val original = ClientMessage.ViewInventory
        val json = MessageSerializer.encodeClientMessage(original)
        val decoded = MessageSerializer.decodeClientMessage(json)
        assertEquals(original, decoded)
    }

    @Test
    fun testEquipItemRoundTrip() {
        val original = ClientMessage.EquipItem("item:iron_sword", "weapon")
        val json = MessageSerializer.encodeClientMessage(original)
        val decoded = MessageSerializer.decodeClientMessage(json)
        assertEquals(original, decoded)
    }

    @Test
    fun testUnequipItemRoundTrip() {
        val original = ClientMessage.UnequipItem("weapon")
        val json = MessageSerializer.encodeClientMessage(original)
        val decoded = MessageSerializer.decodeClientMessage(json)
        assertEquals(original, decoded)
    }

    @Test
    fun testUseItemRoundTrip() {
        val original = ClientMessage.UseItem("item:health_potion")
        val json = MessageSerializer.encodeClientMessage(original)
        val decoded = MessageSerializer.decodeClientMessage(json)
        assertEquals(original, decoded)
    }

    @Test
    fun testInventoryUpdateRoundTrip() {
        val inventory = listOf(
            InventoryItem("item:iron_sword", 1, true, "weapon"),
            InventoryItem("item:health_potion", 3)
        )
        val equipment = mapOf("weapon" to "item:iron_sword")
        val original = ServerMessage.InventoryUpdate(inventory, equipment)
        val json = MessageSerializer.encodeServerMessage(original)
        val decoded = MessageSerializer.decodeServerMessage(json)
        assertEquals(original, decoded)
    }

    @Test
    fun testLootReceivedRoundTrip() {
        val items = listOf(
            LootedItem("item:wolf_pelt", "Wolf Pelt", 1),
            LootedItem("item:health_potion", "Health Potion", 1)
        )
        val original = ServerMessage.LootReceived("Shadow Wolf", items)
        val json = MessageSerializer.encodeServerMessage(original)
        val decoded = MessageSerializer.decodeServerMessage(json)
        assertEquals(original, decoded)
    }

    @Test
    fun testItemUsedRoundTrip() {
        val original = ServerMessage.ItemUsed("Health Potion", "You drink the potion.", 95)
        val json = MessageSerializer.encodeServerMessage(original)
        val decoded = MessageSerializer.decodeServerMessage(json)
        assertEquals(original, decoded)
    }

    @Test
    fun testEquipUpdateRoundTrip() {
        val original = ServerMessage.EquipUpdate("weapon", "item:iron_sword", "Iron Sword")
        val json = MessageSerializer.encodeServerMessage(original)
        val decoded = MessageSerializer.decodeServerMessage(json)
        assertEquals(original, decoded)
    }

    @Test
    fun testEquipUpdateUnequipRoundTrip() {
        val original = ServerMessage.EquipUpdate("weapon", null, null)
        val json = MessageSerializer.encodeServerMessage(original)
        val decoded = MessageSerializer.decodeServerMessage(json)
        assertEquals(original, decoded)
    }
}

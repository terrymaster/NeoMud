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
        val original = ClientMessage.Register("user1", "pass123", "Gandalf", "MAGE")
        val json = MessageSerializer.encodeClientMessage(original)
        val decoded = MessageSerializer.decodeClientMessage(json)
        assertEquals(original, decoded)
    }

    @Test
    fun testRegisterWithRaceRoundTrip() {
        val original = ClientMessage.Register("user1", "pass123", "Gandalf", "MAGE", race = "ELF")
        val json = MessageSerializer.encodeClientMessage(original)
        val decoded = MessageSerializer.decodeClientMessage(json)
        assertEquals(original, decoded)
    }

    @Test
    fun testRegisterWithAllocatedStatsRoundTrip() {
        val stats = Stats(strength = 20, agility = 15, intellect = 30, willpower = 25, health = 15, charm = 18)
        val original = ClientMessage.Register("user1", "pass123", "Gandalf", "MAGE", race = "ELF", allocatedStats = stats)
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
    fun testStatsSerializationRoundTrip() {
        val stats = Stats(strength = 40, agility = 35, intellect = 30, willpower = 25, health = 45, charm = 20)
        val player = Player(
            name = "TestHero",
            characterClass = "WARRIOR",
            stats = stats,
            currentHp = 100,
            maxHp = 120,
            currentMp = 50,
            maxMp = 60,
            level = 1,
            currentRoomId = "town:square"
        )
        val original = ServerMessage.LoginOk(player)
        val json = MessageSerializer.encodeServerMessage(original)
        val decoded = MessageSerializer.decodeServerMessage(json)
        assertEquals(original, decoded)
    }

    @Test
    fun testActiveEffectsUpdateRoundTrip() {
        val effects = listOf(
            ActiveEffect("Poison", EffectType.POISON, 3, 5),
            ActiveEffect("Regeneration", EffectType.HEAL_OVER_TIME, 5, 3)
        )
        val original = ServerMessage.ActiveEffectsUpdate(effects)
        val json = MessageSerializer.encodeServerMessage(original)
        val decoded = MessageSerializer.decodeServerMessage(json)
        assertEquals(original, decoded)
    }

    @Test
    fun testActiveEffectsUpdateEmptyRoundTrip() {
        val original = ServerMessage.ActiveEffectsUpdate(emptyList())
        val json = MessageSerializer.encodeServerMessage(original)
        val decoded = MessageSerializer.decodeServerMessage(json)
        assertEquals(original, decoded)
    }

    @Test
    fun testPlayerDiedWithRespawnMpRoundTrip() {
        val original = ServerMessage.PlayerDied("Shadow Wolf", "town:square", 100, 72)
        val json = MessageSerializer.encodeServerMessage(original)
        val decoded = MessageSerializer.decodeServerMessage(json)
        assertEquals(original, decoded)
    }

    @Test
    fun testPlayerWithMpFieldsRoundTrip() {
        val stats = Stats(strength = 40, agility = 35, intellect = 30, willpower = 25, health = 45, charm = 20)
        val player = Player(
            name = "TestHero",
            characterClass = "WARRIOR",
            stats = stats,
            currentHp = 100,
            maxHp = 120,
            currentMp = 50,
            maxMp = 60,
            level = 1,
            currentRoomId = "town:square"
        )
        val original = ServerMessage.LoginOk(player)
        val json = MessageSerializer.encodeServerMessage(original)
        val decoded = MessageSerializer.decodeServerMessage(json)
        assertEquals(original, decoded)
    }

    @Test
    fun testClassCatalogSyncRoundTrip() {
        val classes = listOf(
            CharacterClassDef("WARRIOR", "Warrior", "A master of martial combat",
                minimumStats = Stats(strength = 20, agility = 12, intellect = 8, willpower = 8, health = 20, charm = 8),
                hpPerLevelMin = 6, hpPerLevelMax = 10),
            CharacterClassDef("MAGE", "Mage", "A scholarly mage",
                minimumStats = Stats(strength = 6, agility = 8, intellect = 22, willpower = 15, health = 8, charm = 10),
                hpPerLevelMin = 3, hpPerLevelMax = 6, mpPerLevelMin = 5, mpPerLevelMax = 10,
                magicSchools = mapOf("mage" to 3))
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
        val original = ServerMessage.ItemUsed("Health Potion", "You drink the potion.", 95, 50)
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

    @Test
    fun testInventoryUpdateWithCoinsRoundTrip() {
        val inventory = listOf(InventoryItem("item:iron_sword", 1, true, "weapon"))
        val equipment = mapOf("weapon" to "item:iron_sword")
        val coins = Coins(copper = 50, silver = 10, gold = 2, platinum = 0)
        val original = ServerMessage.InventoryUpdate(inventory, equipment, coins)
        val json = MessageSerializer.encodeServerMessage(original)
        val decoded = MessageSerializer.decodeServerMessage(json)
        assertEquals(original, decoded)
    }

    @Test
    fun testRoomItemsUpdateRoundTrip() {
        val items = listOf(GroundItem("item:wolf_pelt", 2), GroundItem("item:health_potion", 1))
        val coins = Coins(copper = 15, silver = 1)
        val original = ServerMessage.RoomItemsUpdate(items, coins)
        val json = MessageSerializer.encodeServerMessage(original)
        val decoded = MessageSerializer.decodeServerMessage(json)
        assertEquals(original, decoded)
    }

    @Test
    fun testLootDroppedRoundTrip() {
        val items = listOf(LootedItem("item:wolf_pelt", "Wolf Pelt", 1))
        val coins = Coins(copper = 12, silver = 1)
        val original = ServerMessage.LootDropped("Shadow Wolf", items, coins)
        val json = MessageSerializer.encodeServerMessage(original)
        val decoded = MessageSerializer.decodeServerMessage(json)
        assertEquals(original, decoded)
    }

    @Test
    fun testPickupResultRoundTrip() {
        val original = ServerMessage.PickupResult("Wolf Pelt", 1)
        val json = MessageSerializer.encodeServerMessage(original)
        val decoded = MessageSerializer.decodeServerMessage(json)
        assertEquals(original, decoded)
    }

    @Test
    fun testPickupResultCoinRoundTrip() {
        val original = ServerMessage.PickupResult("copper", 15, isCoin = true)
        val json = MessageSerializer.encodeServerMessage(original)
        val decoded = MessageSerializer.decodeServerMessage(json)
        assertEquals(original, decoded)
    }

    @Test
    fun testPickupItemRoundTrip() {
        val original = ClientMessage.PickupItem("item:wolf_pelt", 2)
        val json = MessageSerializer.encodeClientMessage(original)
        val decoded = MessageSerializer.decodeClientMessage(json)
        assertEquals(original, decoded)
    }

    @Test
    fun testPickupCoinsRoundTrip() {
        val original = ClientMessage.PickupCoins("gold")
        val json = MessageSerializer.encodeClientMessage(original)
        val decoded = MessageSerializer.decodeClientMessage(json)
        assertEquals(original, decoded)
    }

    @Test
    fun testHideToggleRoundTrip() {
        val original = ClientMessage.HideToggle(true)
        val json = MessageSerializer.encodeClientMessage(original)
        val decoded = MessageSerializer.decodeClientMessage(json)
        assertEquals(original, decoded)
    }

    @Test
    fun testHideToggleDisableRoundTrip() {
        val original = ClientMessage.HideToggle(false)
        val json = MessageSerializer.encodeClientMessage(original)
        val decoded = MessageSerializer.decodeClientMessage(json)
        assertEquals(original, decoded)
    }

    @Test
    fun testUseSkillRoundTrip() {
        val original = ClientMessage.UseSkill("BACKSTAB", "npc:shadow_wolf")
        val json = MessageSerializer.encodeClientMessage(original)
        val decoded = MessageSerializer.decodeClientMessage(json)
        assertEquals(original, decoded)
    }

    @Test
    fun testUseSkillNoTargetRoundTrip() {
        val original = ClientMessage.UseSkill("HIDE")
        val json = MessageSerializer.encodeClientMessage(original)
        val decoded = MessageSerializer.decodeClientMessage(json)
        assertEquals(original, decoded)
    }

    @Test
    fun testHideModeUpdateRoundTrip() {
        val original = ServerMessage.HideModeUpdate(true, "You slip into the shadows.")
        val json = MessageSerializer.encodeServerMessage(original)
        val decoded = MessageSerializer.decodeServerMessage(json)
        assertEquals(original, decoded)
    }

    @Test
    fun testHideModeUpdateNoMessageRoundTrip() {
        val original = ServerMessage.HideModeUpdate(false)
        val json = MessageSerializer.encodeServerMessage(original)
        val decoded = MessageSerializer.decodeServerMessage(json)
        assertEquals(original, decoded)
    }

    @Test
    fun testSkillCatalogSyncRoundTrip() {
        val skills = listOf(
            SkillDef("HIDE", "Hide", "Slip into shadows.", "stealth", "agility", "intellect", cooldownTicks = 2, difficulty = 15),
            SkillDef("BACKSTAB", "Backstab", "Strike from shadows.", "combat", "agility", "strength", cooldownTicks = 4, properties = mapOf("damageMultiplier" to "3"))
        )
        val original = ServerMessage.SkillCatalogSync(skills)
        val json = MessageSerializer.encodeServerMessage(original)
        val decoded = MessageSerializer.decodeServerMessage(json)
        assertEquals(original, decoded)
    }

    @Test
    fun testCharacterClassDefWithSkillsRoundTrip() {
        val classDef = CharacterClassDef(
            "THIEF", "Thief", "A scoundrel",
            minimumStats = Stats(strength = 10, agility = 20, intellect = 12, willpower = 8, health = 10, charm = 15),
            skills = listOf("HIDE", "SNEAK", "BACKSTAB"),
            hpPerLevelMin = 4, hpPerLevelMax = 7
        )
        val original = ServerMessage.ClassCatalogSync(listOf(classDef))
        val json = MessageSerializer.encodeServerMessage(original)
        val decoded = MessageSerializer.decodeServerMessage(json)
        assertEquals(original, decoded)
    }

    @Test
    fun testCastSpellRoundTrip() {
        val original = ClientMessage.CastSpell("FIREBALL", "npc:shadow_wolf")
        val json = MessageSerializer.encodeClientMessage(original)
        val decoded = MessageSerializer.decodeClientMessage(json)
        assertEquals(original, decoded)
    }

    @Test
    fun testCastSpellNoTargetRoundTrip() {
        val original = ClientMessage.CastSpell("MINOR_HEAL")
        val json = MessageSerializer.encodeClientMessage(original)
        val decoded = MessageSerializer.decodeClientMessage(json)
        assertEquals(original, decoded)
    }

    @Test
    fun testSpellCatalogSyncRoundTrip() {
        val spells = listOf(
            SpellDef("FIREBALL", "Fireball", "A fireball.", "mage", SpellType.DAMAGE, 18,
                cooldownTicks = 4, levelRequired = 5, basePower = 22, targetType = TargetType.ENEMY,
                castMessage = "hurls a fireball at"),
            SpellDef("MINOR_HEAL", "Minor Heal", "Heals.", "priest", SpellType.HEAL, 5,
                cooldownTicks = 2, primaryStat = "willpower", basePower = 10, targetType = TargetType.SELF)
        )
        val original = ServerMessage.SpellCatalogSync(spells)
        val json = MessageSerializer.encodeServerMessage(original)
        val decoded = MessageSerializer.decodeServerMessage(json)
        assertEquals(original, decoded)
    }

    @Test
    fun testSpellCastResultRoundTrip() {
        val original = ServerMessage.SpellCastResult(true, "Fireball", "Hero hurls a fireball!", 42, null)
        val json = MessageSerializer.encodeServerMessage(original)
        val decoded = MessageSerializer.decodeServerMessage(json)
        assertEquals(original, decoded)
    }

    @Test
    fun testSpellCastResultWithHpRoundTrip() {
        val original = ServerMessage.SpellCastResult(true, "Minor Heal", "Hero heals!", 38, 95)
        val json = MessageSerializer.encodeServerMessage(original)
        val decoded = MessageSerializer.decodeServerMessage(json)
        assertEquals(original, decoded)
    }

    @Test
    fun testSpellEffectRoundTrip() {
        val original = ServerMessage.SpellEffect("Hero", "Shadow Wolf", "Fireball", 25, 15, 50)
        val json = MessageSerializer.encodeServerMessage(original)
        val decoded = MessageSerializer.decodeServerMessage(json)
        assertEquals(original, decoded)
    }

    @Test
    fun testSpellEffectPlayerTargetRoundTrip() {
        val original = ServerMessage.SpellEffect("Hero", "Hero", "Minor Heal", 15, 90, 100, isPlayerTarget = true)
        val json = MessageSerializer.encodeServerMessage(original)
        val decoded = MessageSerializer.decodeServerMessage(json)
        assertEquals(original, decoded)
    }

    @Test
    fun testRaceDefRoundTrip() {
        val raceDef = RaceDef(
            id = "HUMAN",
            name = "Human",
            description = "Balanced and adaptable.",
            statModifiers = Stats(0, 0, 0, 0, 0, 0),
            xpModifier = 1.0
        )
        val original = ServerMessage.RaceCatalogSync(listOf(raceDef))
        val json = MessageSerializer.encodeServerMessage(original)
        val decoded = MessageSerializer.decodeServerMessage(json)
        assertEquals(original, decoded)
    }
}

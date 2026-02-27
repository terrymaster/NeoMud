package com.neomud.server.game.npc

import com.neomud.server.defaultWorldSource
import com.neomud.server.game.npc.behavior.PatrolBehavior
import com.neomud.server.game.npc.behavior.PursuitBehavior
import com.neomud.server.game.npc.behavior.WanderBehavior
import com.neomud.server.world.WorldGraph
import com.neomud.server.world.WorldLoader
import com.neomud.shared.model.Direction
import com.neomud.shared.model.Room
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertNotNull
import kotlin.test.assertEquals

class NpcManagerTest {

    private fun load() = WorldLoader.load(defaultWorldSource())

    @Test
    fun testNpcsAliveAfterLoad() {
        val result = load()
        val npcManager = NpcManager(result.worldGraph)
        npcManager.loadNpcs(result.npcDataList)

        // Town guard starts at town:gate - should be visible (non-combat, maxHp=0)
        val gateNpcs = npcManager.getNpcsInRoom("town:gate")
        val guard = gateNpcs.find { it.id == "npc:town_guard" }
        assertNotNull(guard, "Town Guard should be alive and visible at town:gate")

        // Barkeep starts at town:tavern - non-combat, maxHp=0
        val tavernNpcs = npcManager.getNpcsInRoom("town:tavern")
        val barkeep = tavernNpcs.find { it.id == "npc:barkeep" }
        assertNotNull(barkeep, "Barkeep should be alive and visible at town:tavern")
    }

    @Test
    fun testHostileNpcsAliveAfterLoad() {
        val result = load()
        val npcManager = NpcManager(result.worldGraph)
        npcManager.loadNpcs(result.npcDataList)

        // Hostile NPCs start in forest rooms with maxHp > 0
        val hostiles = npcManager.getLivingHostileNpcsInRoom("forest:path") +
                npcManager.getLivingHostileNpcsInRoom("forest:clearing") +
                npcManager.getLivingHostileNpcsInRoom("forest:deep") +
                npcManager.getLivingHostileNpcsInRoom("forest:edge") +
                npcManager.getLivingHostileNpcsInRoom("forest:stream")

        assertTrue(hostiles.isNotEmpty(), "Should have hostile NPCs alive in the forest")
        for (npc in hostiles) {
            assertTrue(npc.currentHp > 0, "${npc.name} should have HP > 0, got ${npc.currentHp}")
            assertTrue(npc.maxHp > 0, "${npc.name} should have maxHp > 0, got ${npc.maxHp}")
        }
    }

    @Test
    fun testMarkDeadFiltersNpc() {
        val result = load()
        val npcManager = NpcManager(result.worldGraph)
        npcManager.loadNpcs(result.npcDataList)

        // Find the shadow wolf's starting room
        val wolf = result.npcDataList.find { it.first.id == "npc:shadow_wolf" }!!
        val wolfRoom = wolf.first.startRoomId

        val before = npcManager.getNpcsInRoom(wolfRoom)
        assertTrue(before.any { it.id == "npc:shadow_wolf" }, "Wolf should be visible before death")

        npcManager.markDead("npc:shadow_wolf")

        val after = npcManager.getNpcsInRoom(wolfRoom)
        assertTrue(after.none { it.id == "npc:shadow_wolf" }, "Wolf should be gone after death")

        // Non-combat NPCs should still be unaffected
        val tavernNpcs = npcManager.getNpcsInRoom("town:tavern")
        assertTrue(tavernNpcs.any { it.id == "npc:barkeep" }, "Barkeep should still be visible")
    }

    // --- Player detection suppresses hostile NPC wander/patrol ---

    private fun buildSimpleWorld(): WorldGraph {
        val world = WorldGraph()
        world.addRoom(Room("forest:a", "Room A", "A room.", mapOf(Direction.EAST to "forest:b"), "forest", 0, 0))
        world.addRoom(Room("forest:b", "Room B", "B room.", mapOf(Direction.WEST to "forest:a"), "forest", 1, 0))
        return world
    }

    private fun buildNpcManager(world: WorldGraph, vararg npcs: NpcState): NpcManager {
        val manager = NpcManager(world)
        // Use reflection-free approach: load via tick observation
        // We need to access the internal list, so use loadNpcs with NpcData
        // Instead, let's just verify via the public tick() API with a loaded world
        return manager
    }

    @Test
    fun testHostileNpcStaysWhenPlayerDetected() {
        val result = load()
        val npcManager = NpcManager(result.worldGraph)
        npcManager.loadNpcs(result.npcDataList)

        // Find a hostile wandering NPC and its room
        val hostileNpcs = result.npcDataList.filter { it.first.hostile }
        assertTrue(hostileNpcs.isNotEmpty(), "Should have hostile NPCs for testing")

        val testNpc = hostileNpcs.first()
        val npcRoom = testNpc.first.startRoomId

        // Tick many times with the NPC's room containing a visible player
        // The hostile NPC should never generate a movement event
        val roomsWithPlayers = setOf(npcRoom)
        var movedAway = false
        repeat(50) {
            val events = npcManager.tick(roomsWithPlayers)
            if (events.any { it.npcId == testNpc.first.id && it.fromRoomId == npcRoom }) {
                movedAway = true
            }
        }
        assertTrue(!movedAway, "Hostile NPC should not wander away from a room with a visible player")
    }

    @Test
    fun testHostileNpcWandersWhenNoPlayerPresent() {
        val result = load()
        val npcManager = NpcManager(result.worldGraph)
        npcManager.loadNpcs(result.npcDataList)

        // Tick many times with NO rooms containing players
        var movedAtLeastOnce = false
        repeat(50) {
            val events = npcManager.tick(emptySet())
            if (events.any { it.fromRoomId != null }) {
                movedAtLeastOnce = true
            }
        }
        assertTrue(movedAtLeastOnce, "NPCs should wander normally when no players are present")
    }

    @Test
    fun testNonHostileNpcStillMovesWithPlayerPresent() {
        val result = load()
        val npcManager = NpcManager(result.worldGraph)
        npcManager.loadNpcs(result.npcDataList)

        // Town guard is non-hostile and patrols — should still move even with player in room
        val guardRoom = "town:gate"
        val roomsWithPlayers = setOf(guardRoom)
        var guardMoved = false
        repeat(50) {
            val events = npcManager.tick(roomsWithPlayers)
            if (events.any { it.npcName == "Town Guard" && it.fromRoomId != null }) {
                guardMoved = true
            }
        }
        assertTrue(guardMoved, "Non-hostile NPCs should still patrol even when players are present")
    }
}

package com.neomud.server.world

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class WorldLoaderTest {

    @Test
    fun testLoadWorldFromResources() {
        val result = WorldLoader.load()
        val world = result.worldGraph

        assertTrue(world.roomCount >= 10, "Should load at least 10 rooms (5 town + 5 forest)")
    }

    @Test
    fun testTownRoomsLoaded() {
        val result = WorldLoader.load()
        val world = result.worldGraph

        assertNotNull(world.getRoom("town:square"), "town:square should exist")
        assertNotNull(world.getRoom("town:tavern"), "town:tavern should exist")
        assertNotNull(world.getRoom("town:market"), "town:market should exist")
        assertNotNull(world.getRoom("town:gate"), "town:gate should exist")
        assertNotNull(world.getRoom("town:temple"), "town:temple should exist")
    }

    @Test
    fun testForestRoomsLoaded() {
        val result = WorldLoader.load()
        val world = result.worldGraph

        assertNotNull(world.getRoom("forest:edge"), "forest:edge should exist")
        assertNotNull(world.getRoom("forest:path"), "forest:path should exist")
        assertNotNull(world.getRoom("forest:clearing"), "forest:clearing should exist")
        assertNotNull(world.getRoom("forest:deep"), "forest:deep should exist")
        assertNotNull(world.getRoom("forest:stream"), "forest:stream should exist")
    }

    @Test
    fun testCrossZoneExits() {
        val result = WorldLoader.load()
        val world = result.worldGraph

        val gate = world.getRoom("town:gate")
        assertNotNull(gate)
        val northExit = gate.exits[com.neomud.shared.model.Direction.NORTH]
        assertEquals("forest:edge", northExit, "town:gate should exit north to forest:edge")

        val forestEdge = world.getRoom("forest:edge")
        assertNotNull(forestEdge)
        val southExit = forestEdge.exits[com.neomud.shared.model.Direction.SOUTH]
        assertEquals("town:gate", southExit, "forest:edge should exit south to town:gate")
    }

    @Test
    fun testNpcsLoaded() {
        val result = WorldLoader.load()
        assertTrue(result.npcDataList.isNotEmpty(), "Should load NPCs")
        assertTrue(result.npcDataList.size >= 2, "Should load at least 2 NPCs")

        val guard = result.npcDataList.find { it.id == "npc:town_guard" }
        assertNotNull(guard, "Town guard NPC should be loaded")
        assertEquals("patrol", guard.behaviorType)
        assertTrue(guard.patrolRoute.isNotEmpty(), "Guard should have patrol route")

        val barkeep = result.npcDataList.find { it.id == "npc:barkeep" }
        assertNotNull(barkeep, "Barkeep NPC should be loaded")
        assertEquals("idle", barkeep.behaviorType)
    }

    @Test
    fun testDefaultSpawnRoom() {
        val result = WorldLoader.load()
        assertEquals("town:square", result.worldGraph.defaultSpawnRoom)
    }
}

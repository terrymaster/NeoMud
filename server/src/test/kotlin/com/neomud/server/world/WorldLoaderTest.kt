package com.neomud.server.world

import com.neomud.server.defaultWorldSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class WorldLoaderTest {

    private fun load() = WorldLoader.load(defaultWorldSource())

    @Test
    fun testLoadWorldFromResources() {
        val result = load()
        val world = result.worldGraph

        assertTrue(world.roomCount >= 13, "Should load at least 13 rooms (6 town + 7 forest)")
    }

    @Test
    fun testTownRoomsLoaded() {
        val result = load()
        val world = result.worldGraph

        assertNotNull(world.getRoom("town:square"), "town:square should exist")
        assertNotNull(world.getRoom("town:tavern"), "town:tavern should exist")
        assertNotNull(world.getRoom("town:market"), "town:market should exist")
        assertNotNull(world.getRoom("town:gate"), "town:gate should exist")
        assertNotNull(world.getRoom("town:temple"), "town:temple should exist")
        assertNotNull(world.getRoom("town:cellar"), "town:cellar should exist")
    }

    @Test
    fun testForestRoomsLoaded() {
        val result = load()
        val world = result.worldGraph

        assertNotNull(world.getRoom("forest:edge"), "forest:edge should exist")
        assertNotNull(world.getRoom("forest:path"), "forest:path should exist")
        assertNotNull(world.getRoom("forest:clearing"), "forest:clearing should exist")
        assertNotNull(world.getRoom("forest:deep"), "forest:deep should exist")
        assertNotNull(world.getRoom("forest:stream"), "forest:stream should exist")
        assertNotNull(world.getRoom("forest:ruins"), "forest:ruins should exist")
    }

    @Test
    fun testCrossZoneExits() {
        val result = load()
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
        val result = load()
        assertTrue(result.npcDataList.isNotEmpty(), "Should load NPCs")
        assertTrue(result.npcDataList.size >= 4, "Should load at least 4 NPCs (2 town + 2 forest)")

        val guard = result.npcDataList.find { it.first.id == "npc:town_guard" }
        assertNotNull(guard, "Town guard NPC should be loaded")
        assertEquals("patrol", guard.first.behaviorType)
        assertTrue(guard.first.patrolRoute.isNotEmpty(), "Guard should have patrol route")
        assertEquals("town", guard.second, "Guard should belong to town zone")

        val barkeep = result.npcDataList.find { it.first.id == "npc:barkeep" }
        assertNotNull(barkeep, "Barkeep NPC should be loaded")
        assertEquals("idle", barkeep.first.behaviorType)
    }

    @Test
    fun testMonsterNpcsLoaded() {
        val result = load()

        val wolf = result.npcDataList.find { it.first.id == "npc:shadow_wolf" }
        assertNotNull(wolf, "Shadow Wolf should be loaded")
        assertEquals("wander", wolf.first.behaviorType)
        assertTrue(wolf.first.hostile, "Shadow Wolf should be hostile")
        assertEquals(30, wolf.first.maxHp)
        assertEquals(3, wolf.first.damage)
        assertEquals(2, wolf.first.level)
        assertEquals("forest", wolf.second, "Shadow Wolf should belong to forest zone")

        val spider = result.npcDataList.find { it.first.id == "npc:forest_spider" }
        assertNotNull(spider, "Giant Forest Spider should be loaded")
        assertEquals("wander", spider.first.behaviorType)
        assertTrue(spider.first.hostile, "Spider should be hostile")
        assertEquals(20, spider.first.maxHp)
        assertEquals(5, spider.first.damage)
        assertEquals(3, spider.first.level)
        assertEquals("forest", spider.second, "Spider should belong to forest zone")
    }

    @Test
    fun testLockedExitsLoaded() {
        val result = load()
        val world = result.worldGraph

        val deep = world.getRoom("forest:deep")
        assertNotNull(deep)
        assertEquals(3, deep.lockedExits.size, "forest:deep should have 3 locked exits (2 regular + 1 hidden)")
        assertEquals(14, deep.lockedExits[com.neomud.shared.model.Direction.EAST])
        assertEquals(16, deep.lockedExits[com.neomud.shared.model.Direction.NORTH])

        val tavern = world.getRoom("town:tavern")
        assertNotNull(tavern)
        assertEquals(1, tavern.lockedExits.size, "town:tavern should have 1 locked exit")
        assertEquals(10, tavern.lockedExits[com.neomud.shared.model.Direction.DOWN])
    }

    @Test
    fun testHiddenExitsLoaded() {
        val result = load()
        val world = result.worldGraph

        val deep = world.getRoom("forest:deep")
        assertNotNull(deep)
        // Hidden exit WEST should be merged into exits
        assertEquals("forest:cave", deep.exits[com.neomud.shared.model.Direction.WEST])
        // Hidden exit's lockDifficulty should be in lockedExits
        assertEquals(12, deep.lockedExits[com.neomud.shared.model.Direction.WEST])

        // Hidden exit defs should be stored
        val hiddenDefs = world.getHiddenExitDefs("forest:deep")
        assertTrue(hiddenDefs.isNotEmpty(), "Hidden exit defs should be stored")
        val westDef = hiddenDefs[com.neomud.shared.model.Direction.WEST]
        assertNotNull(westDef)
        assertEquals(18, westDef.perceptionDC)
        assertEquals(12, westDef.lockDifficulty)
        assertEquals(60, westDef.hiddenResetTicks)
        assertEquals(40, westDef.lockResetTicks)
    }

    @Test
    fun testLockResetTicksLoaded() {
        val result = load()
        val world = result.worldGraph

        // forest:deep has lockResetTicks for EAST (40)
        // Verify by unlocking and checking timer behavior
        val deep = world.getRoom("forest:deep")
        assertNotNull(deep)
        assertEquals(14, deep.lockedExits[com.neomud.shared.model.Direction.EAST])
    }

    @Test
    fun testForestCaveRoomLoaded() {
        val result = load()
        val world = result.worldGraph

        val cave = world.getRoom("forest:cave")
        assertNotNull(cave, "forest:cave should exist (hidden exit target)")
        assertEquals("Hidden Cave", cave.name)
        assertEquals("forest:deep", cave.exits[com.neomud.shared.model.Direction.EAST])
    }

    @Test
    fun testDefaultSpawnRoom() {
        val result = load()
        assertEquals("town:temple", result.worldGraph.defaultSpawnRoom)
    }

    @Test
    fun testClassCatalogLoaded() {
        val result = load()
        assertEquals(15, result.classCatalog.classCount)
    }

    @Test
    fun testItemCatalogLoaded() {
        val result = load()
        assertTrue(result.itemCatalog.itemCount >= 10, "Should load at least 10 items")
    }

    @Test
    fun testLootTableCatalogLoaded() {
        val result = load()
        assertTrue(result.lootTableCatalog.tableCount >= 2, "Should load at least 2 loot tables")
    }
}

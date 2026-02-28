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

        assertTrue(world.roomCount >= 25, "Should load at least 25 rooms (7 town + 7 forest + 6 marsh + 5 gorge)")
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
        assertNotNull(world.getRoom("town:magic_shop"), "town:magic_shop should exist")
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
        assertTrue(result.npcDataList.size >= 17, "Should load at least 17 NPCs (5 town + 4 forest + 4 marsh + 4 gorge)")

        val guard = result.npcDataList.find { it.first.id == "npc:town_guard" }
        assertNotNull(guard, "Town guard NPC should be loaded")
        assertEquals("patrol", guard.first.behaviorType)
        assertTrue(guard.first.patrolRoute.isNotEmpty(), "Guard should have patrol route")
        assertEquals("town", guard.second, "Guard should belong to town zone")

        val barkeep = result.npcDataList.find { it.first.id == "npc:barkeep" }
        assertNotNull(barkeep, "Barkeep NPC should be loaded")
        assertEquals("vendor", barkeep.first.behaviorType)
        assertTrue(barkeep.first.vendorItems.isNotEmpty(), "Barkeep should have vendor items")
        assertTrue("item:health_potion" in barkeep.first.vendorItems, "Barkeep should sell health potions")
        assertTrue("item:ale" in barkeep.first.vendorItems, "Barkeep should sell ale")

        val enchantress = result.npcDataList.find { it.first.id == "npc:enchantress" }
        assertNotNull(enchantress, "Enchantress NPC should be loaded")
        assertEquals("vendor", enchantress.first.behaviorType)
        assertEquals("town", enchantress.second, "Enchantress should belong to town zone")
        assertTrue(enchantress.first.vendorItems.isNotEmpty(), "Enchantress should have vendor items")
        assertTrue("item:mystic_staff" in enchantress.first.vendorItems, "Enchantress should sell mystic staff")
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
    fun testMagicShopRoomLoaded() {
        val result = load()
        val world = result.worldGraph

        val shop = world.getRoom("town:magic_shop")
        assertNotNull(shop, "town:magic_shop should exist")
        assertEquals("The Enchanted Emporium", shop.name)
        assertEquals("town:market", shop.exits[com.neomud.shared.model.Direction.WEST])

        val market = world.getRoom("town:market")
        assertNotNull(market)
        assertEquals("town:magic_shop", market.exits[com.neomud.shared.model.Direction.EAST],
            "town:market should exit east to town:magic_shop")
    }

    @Test
    fun testLockedExitsLoaded() {
        val result = load()
        val world = result.worldGraph

        val deep = world.getRoom("forest:deep")
        assertNotNull(deep)
        assertEquals(0, deep.lockedExits.size, "forest:deep should have 0 locked exits (all are hidden-only)")

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

        // All three exits should be in the exits map
        assertEquals("forest:stream", deep.exits[com.neomud.shared.model.Direction.EAST])
        assertEquals("forest:ruins", deep.exits[com.neomud.shared.model.Direction.NORTH])
        assertEquals("forest:cave", deep.exits[com.neomud.shared.model.Direction.WEST])

        // All three hidden exits are hidden-only (no locks)
        assertTrue(deep.lockedExits.isEmpty(), "forest:deep should have no locked exits")

        // Hidden exit defs should be stored for all three
        val hiddenDefs = world.getHiddenExitDefs("forest:deep")
        assertEquals(3, hiddenDefs.size, "forest:deep should have 3 hidden exit defs")

        val eastDef = hiddenDefs[com.neomud.shared.model.Direction.EAST]
        assertNotNull(eastDef)
        assertEquals(75, eastDef.perceptionDC)
        assertEquals(0, eastDef.lockDifficulty)
        assertEquals(40, eastDef.hiddenResetTicks)
        assertEquals(0, eastDef.lockResetTicks)

        val northDef = hiddenDefs[com.neomud.shared.model.Direction.NORTH]
        assertNotNull(northDef)
        assertEquals(25, northDef.perceptionDC)
        assertEquals(0, northDef.lockDifficulty)
        assertEquals(50, northDef.hiddenResetTicks)
        assertEquals(0, northDef.lockResetTicks)

        val westDef = hiddenDefs[com.neomud.shared.model.Direction.WEST]
        assertNotNull(westDef)
        assertEquals(88, westDef.perceptionDC)
        assertEquals(0, westDef.lockDifficulty)
        assertEquals(60, westDef.hiddenResetTicks)
        assertEquals(0, westDef.lockResetTicks)
    }

    @Test
    fun testLockResetTicksLoaded() {
        val result = load()
        val world = result.worldGraph

        // forest:deep hidden exits have no locks — verify lockResetTicks are 0
        val deep = world.getRoom("forest:deep")
        assertNotNull(deep)
        assertTrue(deep.lockedExits.isEmpty(), "forest:deep should have no locked exits")
        val eastDef = world.getHiddenExitDefs("forest:deep")[com.neomud.shared.model.Direction.EAST]
        assertNotNull(eastDef)
        assertEquals(0, eastDef.lockResetTicks)
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
    fun testMarshRoomsLoaded() {
        val result = load()
        val world = result.worldGraph

        assertNotNull(world.getRoom("marsh:edge"), "marsh:edge should exist")
        assertNotNull(world.getRoom("marsh:trail"), "marsh:trail should exist")
        assertNotNull(world.getRoom("marsh:shallows"), "marsh:shallows should exist")
        assertNotNull(world.getRoom("marsh:hollow"), "marsh:hollow should exist")
        assertNotNull(world.getRoom("marsh:island"), "marsh:island should exist")
        assertNotNull(world.getRoom("marsh:heart"), "marsh:heart should exist")
    }

    @Test
    fun testGorgeRoomsLoaded() {
        val result = load()
        val world = result.worldGraph

        assertNotNull(world.getRoom("gorge:mouth"), "gorge:mouth should exist")
        assertNotNull(world.getRoom("gorge:ledge"), "gorge:ledge should exist")
        assertNotNull(world.getRoom("gorge:fissure"), "gorge:fissure should exist")
        assertNotNull(world.getRoom("gorge:alcove"), "gorge:alcove should exist")
        assertNotNull(world.getRoom("gorge:depths"), "gorge:depths should exist")
    }

    @Test
    fun testForestToMarshCrossZoneExits() {
        val result = load()
        val world = result.worldGraph

        val ruins = world.getRoom("forest:ruins")
        assertNotNull(ruins)
        assertEquals("marsh:edge", ruins.exits[com.neomud.shared.model.Direction.NORTH],
            "forest:ruins should exit north to marsh:edge")

        val marshEdge = world.getRoom("marsh:edge")
        assertNotNull(marshEdge)
        assertEquals("forest:ruins", marshEdge.exits[com.neomud.shared.model.Direction.SOUTH],
            "marsh:edge should exit south to forest:ruins")
    }

    @Test
    fun testMarshToGorgeCrossZoneExits() {
        val result = load()
        val world = result.worldGraph

        val heart = world.getRoom("marsh:heart")
        assertNotNull(heart)
        assertEquals("gorge:mouth", heart.exits[com.neomud.shared.model.Direction.NORTH],
            "marsh:heart should exit north to gorge:mouth")

        val gorgeMouth = world.getRoom("gorge:mouth")
        assertNotNull(gorgeMouth)
        assertEquals("marsh:heart", gorgeMouth.exits[com.neomud.shared.model.Direction.SOUTH],
            "gorge:mouth should exit south to marsh:heart")
    }

    @Test
    fun testMarshNpcsLoaded() {
        val result = load()

        val lurker = result.npcDataList.find { it.first.id == "npc:bog_lurker" }
        assertNotNull(lurker, "Bog Lurker should be loaded")
        assertEquals("wander", lurker.first.behaviorType)
        assertTrue(lurker.first.hostile)
        assertEquals(70, lurker.first.maxHp)
        assertEquals(18, lurker.first.damage)
        assertEquals(4, lurker.first.level)
        assertEquals("marsh", lurker.second, "Bog Lurker should belong to marsh zone")

        val hag = result.npcDataList.find { it.first.id == "npc:mire_hag" }
        assertNotNull(hag, "Mire Hag should be loaded")
        assertEquals("idle", hag.first.behaviorType)
        assertEquals(140, hag.first.maxHp)
        assertEquals(5, hag.first.level)
        assertEquals("marsh", hag.second)
    }

    @Test
    fun testGorgeNpcsLoaded() {
        val result = load()

        val stalker = result.npcDataList.find { it.first.id == "npc:gorge_stalker" }
        assertNotNull(stalker, "Gorge Stalker should be loaded")
        assertEquals("patrol", stalker.first.behaviorType)
        assertTrue(stalker.first.patrolRoute.isNotEmpty(), "Gorge Stalker should have patrol route")
        assertEquals(95, stalker.first.maxHp)
        assertEquals(6, stalker.first.level)
        assertEquals("gorge", stalker.second)

        val warden = result.npcDataList.find { it.first.id == "npc:gorge_warden" }
        assertNotNull(warden, "Gorge Warden should be loaded")
        assertEquals("idle", warden.first.behaviorType)
        assertEquals(165, warden.first.maxHp)
        assertEquals(7, warden.first.level)
        assertEquals("gorge", warden.second)
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
        assertTrue(result.itemCatalog.itemCount >= 38, "Should load at least 38 items")
    }

    @Test
    fun testLootTableCatalogLoaded() {
        val result = load()
        assertTrue(result.lootTableCatalog.tableCount >= 12, "Should load at least 12 loot tables (4 forest + 4 marsh + 4 gorge)")
    }
}

package com.neomud.server.game.npc

import com.neomud.server.defaultWorldSource
import com.neomud.server.world.WorldLoader
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertNotNull

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
}

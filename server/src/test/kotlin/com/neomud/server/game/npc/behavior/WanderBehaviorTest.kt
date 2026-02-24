package com.neomud.server.game.npc.behavior

import com.neomud.server.game.GameConfig
import com.neomud.server.game.npc.NpcState
import com.neomud.server.world.WorldGraph
import com.neomud.shared.model.Direction
import com.neomud.shared.model.Room
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class WanderBehaviorTest {

    private val wanderTicks = GameConfig.Npc.WANDER_MOVE_TICKS

    private fun buildTestWorld(): WorldGraph {
        val world = WorldGraph()
        world.addRoom(Room("forest:path", "Path", "A path.", mapOf(Direction.NORTH to "forest:deep", Direction.EAST to "forest:clearing"), "forest", 0, 3))
        world.addRoom(Room("forest:deep", "Deep", "Deep woods.", mapOf(Direction.SOUTH to "forest:path"), "forest", 0, 4))
        world.addRoom(Room("forest:clearing", "Clearing", "A clearing.", mapOf(Direction.WEST to "forest:path"), "forest", 1, 3))
        // Town room connected to forest — should NOT be wandered into
        world.addRoom(Room("town:gate", "Gate", "A gate.", mapOf(Direction.NORTH to "forest:path"), "town", 0, 1))
        return world
    }

    private fun makeNpc(behavior: BehaviorNode, roomId: String = "forest:path"): NpcState {
        return NpcState(
            id = "npc:test_wolf",
            name = "Test Wolf",
            description = "A test wolf.",
            currentRoomId = roomId,
            behavior = behavior,
            hostile = true,
            maxHp = 30,
            currentHp = 30,
            damage = 5,
            level = 2,
            zoneId = "forest"
        )
    }

    @Test
    fun testWanderDoesNotMoveBeforeInterval() {
        val behavior = WanderBehavior(moveEveryNTicks = wanderTicks)
        val world = buildTestWorld()
        val npc = makeNpc(behavior)

        // All ticks before the interval should produce None
        for (i in 1 until wanderTicks) {
            val action = behavior.tick(npc, world)
            assertIs<NpcAction.None>(action, "Tick $i should be None")
        }
    }

    @Test
    fun testWanderMovesAtInterval() {
        val behavior = WanderBehavior(moveEveryNTicks = wanderTicks)
        val world = buildTestWorld()
        val npc = makeNpc(behavior)

        // Tick (wanderTicks - 1) times (no move)
        repeat(wanderTicks - 1) { behavior.tick(npc, world) }

        // Next tick should move
        val action = behavior.tick(npc, world)
        assertIs<NpcAction.MoveTo>(action, "Should move on tick $wanderTicks")
    }

    @Test
    fun testWanderStaysInSameZone() {
        val behavior = WanderBehavior(moveEveryNTicks = 1)
        val world = buildTestWorld()
        // Add a forest room adjacent to town
        world.addRoom(Room("forest:edge", "Edge", "Forest edge.", mapOf(Direction.SOUTH to "town:gate", Direction.NORTH to "forest:path"), "forest", 0, 2))

        val npc = makeNpc(behavior, roomId = "forest:edge")

        // Run many ticks — the wolf should only ever pick forest:path (not town:gate)
        val destinations = mutableSetOf<String>()
        repeat(100) {
            val action = behavior.tick(npc, world)
            if (action is NpcAction.MoveTo) {
                destinations.add(action.targetRoomId)
            }
        }

        assertTrue(destinations.isNotEmpty(), "Wolf should have moved at least once")
        assertTrue(destinations.all { it.startsWith("forest:") }, "Wolf should only move to forest rooms, but moved to: $destinations")
    }

    @Test
    fun testWanderFromDeadEndRoom() {
        // A room with only a cross-zone exit should produce no movement
        val world = WorldGraph()
        world.addRoom(Room("forest:isolated", "Isolated", "No same-zone exits.", mapOf(Direction.SOUTH to "town:gate"), "forest", 0, 5))
        world.addRoom(Room("town:gate", "Gate", "A gate.", mapOf(Direction.NORTH to "forest:isolated"), "town", 0, 1))

        val behavior = WanderBehavior(moveEveryNTicks = 1)
        val npc = makeNpc(behavior, roomId = "forest:isolated")

        val action = behavior.tick(npc, world)
        assertIs<NpcAction.None>(action, "Should not move when all exits lead to different zones")
    }

    @Test
    fun testWanderResetsTickCounter() {
        val behavior = WanderBehavior(moveEveryNTicks = 3)
        val world = buildTestWorld()
        val npc = makeNpc(behavior)

        // First cycle: ticks 1,2 = None, tick 3 = Move
        assertIs<NpcAction.None>(behavior.tick(npc, world))
        assertIs<NpcAction.None>(behavior.tick(npc, world))
        assertIs<NpcAction.MoveTo>(behavior.tick(npc, world))

        // Second cycle should reset: ticks 4,5 = None, tick 6 = Move
        assertIs<NpcAction.None>(behavior.tick(npc, world))
        assertIs<NpcAction.None>(behavior.tick(npc, world))
        assertIs<NpcAction.MoveTo>(behavior.tick(npc, world))
    }
}

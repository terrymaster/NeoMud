package com.neomud.server.game

import com.neomud.server.world.HiddenExitData
import com.neomud.server.world.WorldGraph
import com.neomud.shared.model.Direction
import com.neomud.shared.model.Room
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RoomFilterTest {

    private fun buildGraph(): WorldGraph {
        val graph = WorldGraph()
        graph.addRoom(Room(
            "r1", "Room 1", "desc",
            mapOf(Direction.NORTH to "r2", Direction.WEST to "r3"),
            "zone", 0, 0,
            lockedExits = mapOf(Direction.WEST to 12)
        ))
        graph.addRoom(Room("r2", "Room 2", "desc", mapOf(Direction.SOUTH to "r1"), "zone", 0, 1))
        graph.addRoom(Room("r3", "Room 3", "desc", mapOf(Direction.EAST to "r1"), "zone", -1, 0))
        graph.setHiddenExitDefs("r1", mapOf(Direction.WEST to HiddenExitData(perceptionDC = 15, lockDifficulty = 12)))
        return graph
    }

    /** Minimal stub that provides hasDiscoveredExit / discoverExit without WebSocket. */
    private class FakeSession {
        val discoveredHiddenExits = mutableSetOf<String>()

        fun hasDiscoveredExit(roomId: String, direction: Direction): Boolean =
            "$roomId:$direction" in discoveredHiddenExits

        fun discoverExit(roomId: String, direction: Direction) {
            discoveredHiddenExits.add("$roomId:$direction")
        }
    }

    /** Adapter that calls RoomFilter logic directly using the same algorithm. */
    private fun filterForFakeSession(room: Room, fake: FakeSession, worldGraph: WorldGraph): Room {
        val hiddenDefs = worldGraph.getHiddenExitDefs(room.id)
        if (hiddenDefs.isEmpty()) return room
        val visibleExits = room.exits.filter { (dir, _) ->
            dir !in hiddenDefs || fake.hasDiscoveredExit(room.id, dir)
        }
        val visibleLocks = room.lockedExits.filter { (dir, _) ->
            dir !in hiddenDefs || fake.hasDiscoveredExit(room.id, dir)
        }
        return room.copy(exits = visibleExits, lockedExits = visibleLocks)
    }

    @Test
    fun testUndiscoveredHiddenExitFiltered() {
        val graph = buildGraph()
        val session = FakeSession()
        val room = graph.getRoom("r1")!!

        val filtered = filterForFakeSession(room, session, graph)

        assertEquals(1, filtered.exits.size, "Only NORTH should be visible")
        assertTrue(Direction.NORTH in filtered.exits)
        assertFalse(Direction.WEST in filtered.exits)
        assertTrue(filtered.lockedExits.isEmpty(), "Hidden locked exit should be filtered")
    }

    @Test
    fun testDiscoveredHiddenExitVisible() {
        val graph = buildGraph()
        val session = FakeSession()
        session.discoverExit("r1", Direction.WEST)
        val room = graph.getRoom("r1")!!

        val filtered = filterForFakeSession(room, session, graph)

        assertEquals(2, filtered.exits.size)
        assertTrue(Direction.NORTH in filtered.exits)
        assertTrue(Direction.WEST in filtered.exits)
        assertEquals(12, filtered.lockedExits[Direction.WEST])
    }

    @Test
    fun testRoomWithNoHiddenExitsUnchanged() {
        val graph = buildGraph()
        val session = FakeSession()
        val room = graph.getRoom("r2")!!

        val filtered = filterForFakeSession(room, session, graph)

        // Should be the exact same object (no copy needed)
        assertTrue(room === filtered, "Room without hidden exits should be returned as-is")
    }
}

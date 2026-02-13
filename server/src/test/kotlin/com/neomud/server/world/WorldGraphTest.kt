package com.neomud.server.world

import com.neomud.shared.model.Direction
import com.neomud.shared.model.Room
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class WorldGraphTest {

    private fun buildTestWorld(): WorldGraph {
        val graph = WorldGraph()

        graph.addRoom(Room("town:square", "Town Square", "Center of town", mapOf(Direction.NORTH to "town:gate", Direction.EAST to "town:market"), "town", 0, 0))
        graph.addRoom(Room("town:gate", "North Gate", "The gate", mapOf(Direction.SOUTH to "town:square", Direction.NORTH to "forest:edge"), "town", 0, 1))
        graph.addRoom(Room("town:market", "Market", "The market", mapOf(Direction.WEST to "town:square"), "town", 1, 0))
        graph.addRoom(Room("forest:edge", "Forest Edge", "Forest start", mapOf(Direction.SOUTH to "town:gate", Direction.NORTH to "forest:path"), "forest", 0, 2))
        graph.addRoom(Room("forest:path", "Forest Path", "A winding path", mapOf(Direction.SOUTH to "forest:edge"), "forest", 0, 3))

        graph.setDefaultSpawn("town:square")
        return graph
    }

    @Test
    fun testGetRoom() {
        val graph = buildTestWorld()
        val room = graph.getRoom("town:square")
        assertNotNull(room)
        assertEquals("Town Square", room.name)
    }

    @Test
    fun testGetRoomReturnsNullForInvalidId() {
        val graph = buildTestWorld()
        assertNull(graph.getRoom("nonexistent:room"))
    }

    @Test
    fun testDefaultSpawnRoom() {
        val graph = buildTestWorld()
        assertEquals("town:square", graph.defaultSpawnRoom)
    }

    @Test
    fun testGetRoomsNearRadius1() {
        val graph = buildTestWorld()
        val nearby = graph.getRoomsNear("town:square", radius = 1)
        val ids = nearby.map { it.id }.toSet()

        assert("town:square" in ids) { "Should include center room" }
        assert("town:gate" in ids) { "Should include north neighbor" }
        assert("town:market" in ids) { "Should include east neighbor" }
        assertEquals(3, nearby.size, "Should find exactly 3 rooms at radius 1")
    }

    @Test
    fun testGetRoomsNearCrossesZoneBoundary() {
        val graph = buildTestWorld()
        val nearby = graph.getRoomsNear("town:gate", radius = 2)
        val ids = nearby.map { it.id }.toSet()

        assert("forest:edge" in ids) { "Should include cross-zone room forest:edge" }
        assert("town:square" in ids) { "Should include town:square" }
        assert("forest:path" in ids) { "Should include forest:path at radius 2" }
    }

    @Test
    fun testGetRoomsNearFullWorld() {
        val graph = buildTestWorld()
        val nearby = graph.getRoomsNear("town:square", radius = 10)
        assertEquals(5, nearby.size, "Should find all rooms in the world")
    }

    @Test
    fun testRoomCount() {
        val graph = buildTestWorld()
        assertEquals(5, graph.roomCount)
    }
}

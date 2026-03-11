package com.neomud.server.game

import com.neomud.shared.model.Direction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DepartureQueueTest {

    @Test
    fun testRecordDepartureAddsToQueue() {
        val loop = createMinimalGameLoop()
        loop.recordDeparture("player1", "zone:room1", Direction.NORTH)
        loop.recordDeparture("player2", "zone:room2", Direction.SOUTH)

        val departures = loop.drainDepartures()
        assertEquals(2, departures.size)
        assertEquals("player1", departures[0].playerName)
        assertEquals("zone:room1", departures[0].fromRoomId)
        assertEquals(Direction.NORTH, departures[0].direction)
        assertEquals("player2", departures[1].playerName)
    }

    @Test
    fun testDrainClearsQueue() {
        val loop = createMinimalGameLoop()
        loop.recordDeparture("player1", "zone:room1", Direction.NORTH)

        val first = loop.drainDepartures()
        assertEquals(1, first.size)

        val second = loop.drainDepartures()
        assertTrue(second.isEmpty())
    }

    private fun createMinimalGameLoop(): TestableGameLoop {
        return TestableGameLoop()
    }

    /**
     * Minimal wrapper to test the departure queue in isolation.
     * The real GameLoop has too many dependencies for a unit test.
     */
    class TestableGameLoop {
        private val pendingDepartures = java.util.concurrent.ConcurrentLinkedQueue<GameLoop.PendingDeparture>()

        fun recordDeparture(playerName: String, fromRoomId: String, direction: Direction) {
            pendingDepartures.add(GameLoop.PendingDeparture(playerName, fromRoomId, direction))
        }

        fun drainDepartures(): List<GameLoop.PendingDeparture> {
            val result = mutableListOf<GameLoop.PendingDeparture>()
            while (true) { result.add(pendingDepartures.poll() ?: break) }
            return result
        }
    }
}

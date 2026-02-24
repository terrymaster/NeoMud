package com.neomud.server.game

import com.neomud.shared.model.Direction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MovementTrailManagerTest {

    private val defaultLifetimeMs = GameConfig.Trails.LIFETIME_MS
    private val maxStaleness = GameConfig.Trails.STALENESS_PENALTY_MAX

    @Test
    fun `recordTrail stores and retrieves trail`() {
        val mgr = MovementTrailManager()
        val entry = TrailEntry("Wolf", "npc:wolf", Direction.NORTH, 1000L, false)
        mgr.recordTrail("room:1", entry)

        val trails = mgr.getTrails("room:1", now = 1000L)
        assertEquals(1, trails.size)
        assertEquals("Wolf", trails[0].entityName)
        assertEquals(Direction.NORTH, trails[0].direction)
    }

    @Test
    fun `getTrails filters by entityId`() {
        val mgr = MovementTrailManager()
        mgr.recordTrail("room:1", TrailEntry("Wolf", "npc:wolf", Direction.NORTH, 1000L, false))
        mgr.recordTrail("room:1", TrailEntry("Bear", "npc:bear", Direction.SOUTH, 1001L, false))

        val wolfTrails = mgr.getTrails("room:1", entityId = "npc:wolf", now = 1001L)
        assertEquals(1, wolfTrails.size)
        assertEquals("Wolf", wolfTrails[0].entityName)
    }

    @Test
    fun `getTrails returns freshest first`() {
        val mgr = MovementTrailManager()
        mgr.recordTrail("room:1", TrailEntry("Wolf", "npc:wolf", Direction.NORTH, 1000L, false))
        mgr.recordTrail("room:1", TrailEntry("Wolf", "npc:wolf", Direction.EAST, 2000L, false))

        val trails = mgr.getTrails("room:1", entityId = "npc:wolf", now = 2000L)
        assertEquals(2, trails.size)
        assertEquals(Direction.EAST, trails[0].direction)
        assertEquals(Direction.NORTH, trails[1].direction)
    }

    @Test
    fun `getTrails excludes expired entries`() {
        val mgr = MovementTrailManager(trailLifetimeMs = 5000)
        mgr.recordTrail("room:1", TrailEntry("Wolf", "npc:wolf", Direction.NORTH, 1000L, false))

        val trails = mgr.getTrails("room:1", now = 7000L) // 6000ms old > 5000ms lifetime
        assertTrue(trails.isEmpty())
    }

    @Test
    fun `getTrails returns empty for unknown room`() {
        val mgr = MovementTrailManager()
        val trails = mgr.getTrails("room:nonexistent")
        assertTrue(trails.isEmpty())
    }

    @Test
    fun `recordTrail caps at maxEntriesPerRoom`() {
        val mgr = MovementTrailManager(maxEntriesPerRoom = 3)
        mgr.recordTrail("room:1", TrailEntry("A", "a", Direction.NORTH, 1000L, false))
        mgr.recordTrail("room:1", TrailEntry("B", "b", Direction.SOUTH, 2000L, false))
        mgr.recordTrail("room:1", TrailEntry("C", "c", Direction.EAST, 3000L, false))
        mgr.recordTrail("room:1", TrailEntry("D", "d", Direction.WEST, 4000L, false))

        val trails = mgr.getTrails("room:1", now = 4000L)
        assertEquals(3, trails.size)
        // Oldest (A) should have been dropped
        assertFalse(trails.any { it.entityName == "A" })
    }

    @Test
    fun `stalenessPenalty is 0 for fresh trails`() {
        val mgr = MovementTrailManager(trailLifetimeMs = defaultLifetimeMs)
        val entry = TrailEntry("Wolf", "npc:wolf", Direction.NORTH, 1000L, false)
        assertEquals(0, mgr.stalenessPenalty(entry, now = 1000L))
    }

    @Test
    fun `stalenessPenalty increases with age`() {
        val mgr = MovementTrailManager(trailLifetimeMs = 100_000)
        val entry = TrailEntry("Wolf", "npc:wolf", Direction.NORTH, 0L, false)
        // At 50% age (50000ms / 100000ms) -> penalty = (0.5 * maxStaleness).toInt()
        val penalty = mgr.stalenessPenalty(entry, now = 50_000L)
        val expectedPenalty = (0.5 * maxStaleness).toInt()
        assertTrue(penalty in expectedPenalty..(expectedPenalty + 1))
    }

    @Test
    fun `stalenessPenalty caps at max`() {
        val mgr = MovementTrailManager(trailLifetimeMs = 10_000)
        val entry = TrailEntry("Wolf", "npc:wolf", Direction.NORTH, 0L, false)
        val penalty = mgr.stalenessPenalty(entry, now = 20_000L)
        assertEquals(maxStaleness, penalty)
    }

    @Test
    fun `pruneStale removes old entries`() {
        val mgr = MovementTrailManager(trailLifetimeMs = 5000)
        mgr.recordTrail("room:1", TrailEntry("Wolf", "npc:wolf", Direction.NORTH, 1000L, false))
        mgr.recordTrail("room:1", TrailEntry("Bear", "npc:bear", Direction.SOUTH, 5000L, false))

        mgr.pruneStale(now = 7000L) // cutoff at 2000 — wolf (1000) is stale, bear (5000) is fine

        val trails = mgr.getTrails("room:1", now = 7000L)
        assertEquals(1, trails.size)
        assertEquals("Bear", trails[0].entityName)
    }

    @Test
    fun `pruneStale removes empty room entries`() {
        val mgr = MovementTrailManager(trailLifetimeMs = 5000)
        mgr.recordTrail("room:1", TrailEntry("Wolf", "npc:wolf", Direction.NORTH, 1000L, false))

        mgr.pruneStale(now = 10_000L)

        val trails = mgr.getTrails("room:1", now = 10_000L)
        assertTrue(trails.isEmpty())
    }

    @Test
    fun `player trails are recorded and retrievable`() {
        val mgr = MovementTrailManager()
        mgr.recordTrail("room:1", TrailEntry("Hero", "Hero", Direction.EAST, 1000L, true))

        val trails = mgr.getTrails("room:1", entityId = "Hero", now = 1000L)
        assertEquals(1, trails.size)
        assertTrue(trails[0].isPlayer)
    }
}

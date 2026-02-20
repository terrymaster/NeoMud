package com.neomud.server.game

import com.neomud.shared.model.Direction
import com.neomud.shared.model.RoomId

data class TrailEntry(
    val entityName: String,
    val entityId: String,
    val direction: Direction,
    val timestamp: Long,
    val isPlayer: Boolean
)

class MovementTrailManager(
    private val maxEntriesPerRoom: Int = 20,
    private val trailLifetimeMs: Long = 90_000
) {
    private val trails = HashMap<RoomId, MutableList<TrailEntry>>()

    fun recordTrail(roomId: RoomId, entry: TrailEntry) {
        val list = trails.getOrPut(roomId) { mutableListOf() }
        list.add(entry)
        if (list.size > maxEntriesPerRoom) {
            list.removeAt(0)
        }
    }

    fun getTrails(roomId: RoomId, entityId: String? = null, now: Long = System.currentTimeMillis()): List<TrailEntry> {
        val list = trails[roomId] ?: return emptyList()
        val cutoff = now - trailLifetimeMs
        val fresh = list.filter { it.timestamp >= cutoff }
        val filtered = if (entityId != null) fresh.filter { it.entityId == entityId } else fresh
        return filtered.sortedByDescending { it.timestamp }
    }

    /**
     * Returns a staleness penalty from 0 (fresh) to 5 (near expiry) based on age.
     */
    fun stalenessPenalty(entry: TrailEntry, now: Long = System.currentTimeMillis()): Int {
        val age = now - entry.timestamp
        if (age <= 0) return 0
        val ratio = age.toDouble() / trailLifetimeMs
        return (ratio * 5).toInt().coerceIn(0, 5)
    }

    fun pruneStale(now: Long = System.currentTimeMillis()) {
        val cutoff = now - trailLifetimeMs
        val iter = trails.iterator()
        while (iter.hasNext()) {
            val (_, list) = iter.next()
            list.removeAll { it.timestamp < cutoff }
            if (list.isEmpty()) iter.remove()
        }
    }
}

package com.neomud.server.persistence.repository

import com.neomud.server.persistence.tables.PlayerDiscoveryTable
import com.neomud.shared.model.RoomId
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

data class PlayerDiscoveryData(
    val visitedRooms: Set<RoomId>,
    val discoveredHiddenExits: Set<String>,
    val discoveredLockedExits: Set<String>,
    val discoveredInteractables: Set<String>
)

class DiscoveryRepository {

    fun loadPlayerDiscovery(playerName: String): PlayerDiscoveryData = transaction {
        val rows = PlayerDiscoveryTable.selectAll().where {
            PlayerDiscoveryTable.playerName eq playerName
        }.toList()

        val visitedRooms = mutableSetOf<RoomId>()
        val hiddenExits = mutableSetOf<String>()
        val lockedExits = mutableSetOf<String>()
        val interactables = mutableSetOf<String>()

        for (row in rows) {
            val key = row[PlayerDiscoveryTable.discoveryKey]
            when (row[PlayerDiscoveryTable.discoveryType]) {
                "visited_room" -> visitedRooms.add(key)
                "hidden_exit" -> hiddenExits.add(key)
                "locked_exit" -> lockedExits.add(key)
                "interactable" -> interactables.add(key)
            }
        }

        PlayerDiscoveryData(visitedRooms, hiddenExits, lockedExits, interactables)
    }

    fun savePlayerDiscovery(playerName: String, data: PlayerDiscoveryData): Unit = transaction {
        val entries = mutableListOf<Pair<String, String>>()
        for (room in data.visitedRooms) entries.add("visited_room" to room)
        for (exit in data.discoveredHiddenExits) entries.add("hidden_exit" to exit)
        for (exit in data.discoveredLockedExits) entries.add("locked_exit" to exit)
        for (inter in data.discoveredInteractables) entries.add("interactable" to inter)

        for ((type, key) in entries) {
            PlayerDiscoveryTable.insertIgnore {
                it[PlayerDiscoveryTable.playerName] = playerName
                it[discoveryType] = type
                it[discoveryKey] = key
            }
        }
    }
}

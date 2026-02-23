package com.neomud.server

import com.neomud.server.persistence.repository.DiscoveryRepository
import com.neomud.server.persistence.repository.PlayerDiscoveryData
import com.neomud.server.persistence.tables.PlayerDiscoveryTable
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DiscoveryRepositoryTest {

    private fun withTestDb(block: () -> Unit) {
        val tmpFile = File.createTempFile("neomud_discovery_test_", ".db")
        tmpFile.deleteOnExit()
        tmpFile.delete()
        Database.connect("jdbc:sqlite:${tmpFile.absolutePath}", driver = "org.sqlite.JDBC")
        transaction { SchemaUtils.create(PlayerDiscoveryTable) }
        block()
    }

    @Test
    fun testLoadEmptyDiscovery() = withTestDb {
        val repo = DiscoveryRepository()
        val data = repo.loadPlayerDiscovery("nobody")
        assertTrue(data.visitedRooms.isEmpty())
        assertTrue(data.discoveredHiddenExits.isEmpty())
        assertTrue(data.discoveredLockedExits.isEmpty())
        assertTrue(data.discoveredInteractables.isEmpty())
    }

    @Test
    fun testSaveAndLoadVisitedRooms() = withTestDb {
        val repo = DiscoveryRepository()
        val data = PlayerDiscoveryData(
            visitedRooms = setOf("town:square", "town:gate", "forest:path"),
            discoveredHiddenExits = emptySet(),
            discoveredLockedExits = emptySet(),
            discoveredInteractables = emptySet()
        )
        repo.savePlayerDiscovery("bob", data)
        val loaded = repo.loadPlayerDiscovery("bob")
        assertEquals(setOf("town:square", "town:gate", "forest:path"), loaded.visitedRooms)
    }

    @Test
    fun testSaveAndLoadAllTypes() = withTestDb {
        val repo = DiscoveryRepository()
        val data = PlayerDiscoveryData(
            visitedRooms = setOf("town:square", "town:gate"),
            discoveredHiddenExits = setOf("town:square:EAST", "forest:path:NORTH"),
            discoveredLockedExits = setOf("town:gate:NORTH"),
            discoveredInteractables = setOf("town:square::lever_1", "dungeon:entry::chest_2")
        )
        repo.savePlayerDiscovery("alice", data)
        val loaded = repo.loadPlayerDiscovery("alice")
        assertEquals(data.visitedRooms, loaded.visitedRooms)
        assertEquals(data.discoveredHiddenExits, loaded.discoveredHiddenExits)
        assertEquals(data.discoveredLockedExits, loaded.discoveredLockedExits)
        assertEquals(data.discoveredInteractables, loaded.discoveredInteractables)
    }

    @Test
    fun testSaveIsIdempotent() = withTestDb {
        val repo = DiscoveryRepository()
        val data = PlayerDiscoveryData(
            visitedRooms = setOf("town:square"),
            discoveredHiddenExits = emptySet(),
            discoveredLockedExits = emptySet(),
            discoveredInteractables = emptySet()
        )
        repo.savePlayerDiscovery("bob", data)
        // Save again with same + new rooms — should not fail or duplicate
        val updated = data.copy(visitedRooms = setOf("town:square", "town:gate"))
        repo.savePlayerDiscovery("bob", updated)
        val loaded = repo.loadPlayerDiscovery("bob")
        assertEquals(setOf("town:square", "town:gate"), loaded.visitedRooms)
    }

    @Test
    fun testPlayersAreIsolated() = withTestDb {
        val repo = DiscoveryRepository()
        repo.savePlayerDiscovery("alice", PlayerDiscoveryData(
            visitedRooms = setOf("town:square", "town:gate"),
            discoveredHiddenExits = setOf("town:square:EAST"),
            discoveredLockedExits = emptySet(),
            discoveredInteractables = emptySet()
        ))
        repo.savePlayerDiscovery("bob", PlayerDiscoveryData(
            visitedRooms = setOf("forest:path"),
            discoveredHiddenExits = emptySet(),
            discoveredLockedExits = setOf("dungeon:door:NORTH"),
            discoveredInteractables = emptySet()
        ))

        val aliceData = repo.loadPlayerDiscovery("alice")
        assertEquals(setOf("town:square", "town:gate"), aliceData.visitedRooms)
        assertEquals(setOf("town:square:EAST"), aliceData.discoveredHiddenExits)
        assertTrue(aliceData.discoveredLockedExits.isEmpty())

        val bobData = repo.loadPlayerDiscovery("bob")
        assertEquals(setOf("forest:path"), bobData.visitedRooms)
        assertTrue(bobData.discoveredHiddenExits.isEmpty())
        assertEquals(setOf("dungeon:door:NORTH"), bobData.discoveredLockedExits)
    }
}

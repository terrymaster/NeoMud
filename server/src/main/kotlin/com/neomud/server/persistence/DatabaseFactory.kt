package com.neomud.server.persistence

import com.neomud.server.persistence.tables.InventoryTable
import com.neomud.server.persistence.tables.PlayerCoinsTable
import com.neomud.server.persistence.tables.PlayersTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("DatabaseFactory")

private const val SCHEMA_VERSION = 2 // Bumped for 6-stat progression system

object DatabaseFactory {
    fun init(jdbcUrl: String = "jdbc:sqlite:neomud.db") {
        val database = Database.connect(
            url = jdbcUrl,
            driver = "org.sqlite.JDBC"
        )

        transaction(database) {
            // Check if we need a fresh start by looking for old schema columns
            val needsFreshStart = try {
                exec("SELECT dexterity FROM players LIMIT 1") { true }
                true // Old schema detected
            } catch (_: Exception) {
                false
            }

            if (needsFreshStart) {
                logger.warn("Old schema detected — dropping all tables for fresh start (progression overhaul)")
                SchemaUtils.drop(PlayersTable, InventoryTable, PlayerCoinsTable)
            }

            SchemaUtils.create(PlayersTable, InventoryTable, PlayerCoinsTable)
        }

        logger.info("Database initialized (schema v$SCHEMA_VERSION)")
    }
}

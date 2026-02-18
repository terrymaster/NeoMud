package com.neomud.server.persistence

import com.neomud.server.persistence.tables.InventoryTable
import com.neomud.server.persistence.tables.PlayerCoinsTable
import com.neomud.server.persistence.tables.PlayersTable
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("DatabaseFactory")

private const val SCHEMA_VERSION = 4 // Added is_admin column

object DatabaseFactory {
    fun init(jdbcUrl: String = "jdbc:sqlite:neomud.db") {
        val database = Database.connect(
            url = jdbcUrl,
            driver = "org.sqlite.JDBC"
        )

        transaction(database) {
            // Check if schema is outdated — drop and recreate
            val needsFreshStart = try {
                exec("SELECT base_strength FROM players LIMIT 1") { true }
                false
            } catch (_: Exception) {
                // Missing columns from current schema — wipe and recreate
                true
            }

            if (needsFreshStart) {
                logger.warn("Outdated schema detected — dropping all tables for fresh start")
                SchemaUtils.drop(PlayersTable, InventoryTable, PlayerCoinsTable)
            }

            SchemaUtils.create(PlayersTable, InventoryTable, PlayerCoinsTable)

            // Incremental migration: add is_admin if missing
            try {
                exec("SELECT is_admin FROM players LIMIT 1") { true }
            } catch (_: Exception) {
                logger.info("Migrating schema: adding is_admin column")
                exec("ALTER TABLE players ADD COLUMN is_admin BOOLEAN NOT NULL DEFAULT 0")
            }
        }

        logger.info("Database initialized (schema v$SCHEMA_VERSION)")
    }
}

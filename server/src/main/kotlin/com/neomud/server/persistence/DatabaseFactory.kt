package com.neomud.server.persistence

import com.neomud.server.persistence.tables.InventoryTable
import com.neomud.server.persistence.tables.PlayerCoinsTable
import com.neomud.server.persistence.tables.PlayerDiscoveryTable
import com.neomud.server.persistence.tables.PlayersTable
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("DatabaseFactory")

private const val SCHEMA_VERSION = 6 // Added player_discovery table

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
                SchemaUtils.drop(PlayersTable, InventoryTable, PlayerCoinsTable, PlayerDiscoveryTable)
            }

            SchemaUtils.create(PlayersTable, InventoryTable, PlayerCoinsTable, PlayerDiscoveryTable)

            // Incremental migration: add is_admin if missing
            try {
                exec("SELECT is_admin FROM players LIMIT 1") { true }
            } catch (_: Exception) {
                logger.info("Migrating schema: adding is_admin column")
                exec("ALTER TABLE players ADD COLUMN is_admin BOOLEAN NOT NULL DEFAULT 0")
            }

            // Incremental migration: add gender if missing
            try {
                exec("SELECT gender FROM players LIMIT 1") { true }
            } catch (_: Exception) {
                logger.info("Migrating schema: adding gender column")
                exec("ALTER TABLE players ADD COLUMN gender VARCHAR(20) NOT NULL DEFAULT 'neutral'")
            }

            // Incremental migration: add image_prompt if missing
            try {
                exec("SELECT image_prompt FROM players LIMIT 1") { true }
            } catch (_: Exception) {
                logger.info("Migrating schema: adding image_prompt column")
                exec("ALTER TABLE players ADD COLUMN image_prompt TEXT NOT NULL DEFAULT ''")
            }
        }

        logger.info("Database initialized (schema v$SCHEMA_VERSION)")
    }
}

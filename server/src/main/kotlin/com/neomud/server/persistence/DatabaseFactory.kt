package com.neomud.server.persistence

import com.neomud.server.persistence.tables.InventoryTable
import com.neomud.server.persistence.tables.PlayerCoinsTable
import com.neomud.server.persistence.tables.PlayersTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("DatabaseFactory")

private const val SCHEMA_VERSION = 3 // Bumped for base stat columns (CP allocation at registration)

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
        }

        logger.info("Database initialized (schema v$SCHEMA_VERSION)")
    }
}

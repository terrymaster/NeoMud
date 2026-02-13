package com.neomud.server.persistence

import com.neomud.server.persistence.tables.PlayersTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("DatabaseFactory")

object DatabaseFactory {
    fun init() {
        val database = Database.connect(
            url = "jdbc:sqlite:neomud.db",
            driver = "org.sqlite.JDBC"
        )

        transaction(database) {
            SchemaUtils.create(PlayersTable)
        }

        logger.info("Database initialized")
    }
}

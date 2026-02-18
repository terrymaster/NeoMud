package com.neomud.server.persistence.repository

import com.neomud.server.persistence.tables.PlayerCoinsTable
import com.neomud.shared.model.Coins
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.slf4j.LoggerFactory

class CoinRepository {
    private val logger = LoggerFactory.getLogger(CoinRepository::class.java)

    fun getCoins(playerName: String): Coins = transaction {
        PlayerCoinsTable.selectAll().where {
            PlayerCoinsTable.playerName eq playerName
        }.firstOrNull()?.let { row ->
            Coins(
                copper = row[PlayerCoinsTable.copper],
                silver = row[PlayerCoinsTable.silver],
                gold = row[PlayerCoinsTable.gold],
                platinum = row[PlayerCoinsTable.platinum]
            )
        } ?: Coins()
    }

    fun subtractCoins(playerName: String, cost: Coins): Boolean = transaction {
        val current = getCoins(playerName)
        val currentTotal = current.totalCopper()
        val costTotal = cost.totalCopper()
        if (currentTotal < costTotal) {
            false
        } else {
            val remaining = Coins.fromCopper(currentTotal - costTotal)
            PlayerCoinsTable.update({ PlayerCoinsTable.playerName eq playerName }) {
                it[copper] = remaining.copper
                it[silver] = remaining.silver
                it[gold] = remaining.gold
                it[platinum] = remaining.platinum
            }
            logger.info("Subtracted ${cost.displayString()} from $playerName, remaining: ${remaining.displayString()}")
            true
        }
    }

    fun addCoins(playerName: String, coins: Coins): Unit = transaction {
        // Ensure row exists
        PlayerCoinsTable.insertIgnore {
            it[PlayerCoinsTable.playerName] = playerName
        }
        val current = PlayerCoinsTable.selectAll().where {
            PlayerCoinsTable.playerName eq playerName
        }.first()
        PlayerCoinsTable.update({ PlayerCoinsTable.playerName eq playerName }) {
            it[copper] = current[copper] + coins.copper
            it[silver] = current[silver] + coins.silver
            it[gold] = current[gold] + coins.gold
            it[platinum] = current[platinum] + coins.platinum
        }
    }
}

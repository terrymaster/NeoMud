package com.neomud.server.persistence.repository

import com.neomud.server.persistence.tables.PlayerCoinsTable
import com.neomud.shared.model.Coins
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

class CoinRepository {

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

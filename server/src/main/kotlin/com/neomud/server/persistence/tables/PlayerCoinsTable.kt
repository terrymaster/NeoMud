package com.neomud.server.persistence.tables

import org.jetbrains.exposed.sql.Table

object PlayerCoinsTable : Table("player_coins") {
    val playerName = varchar("player_name", 50).uniqueIndex()
    val copper = integer("copper").default(0)
    val silver = integer("silver").default(0)
    val gold = integer("gold").default(0)
    val platinum = integer("platinum").default(0)

    override val primaryKey = PrimaryKey(playerName)
}

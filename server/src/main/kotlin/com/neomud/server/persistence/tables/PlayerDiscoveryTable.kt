package com.neomud.server.persistence.tables

import org.jetbrains.exposed.v1.core.Table

object PlayerDiscoveryTable : Table("player_discovery") {
    val id = integer("id").autoIncrement()
    val playerName = varchar("player_name", 50).index()
    val discoveryType = varchar("discovery_type", 20)
    val discoveryKey = varchar("discovery_key", 200)

    override val primaryKey = PrimaryKey(id)

    init {
        index(isUnique = true, playerName, discoveryType, discoveryKey)
    }
}

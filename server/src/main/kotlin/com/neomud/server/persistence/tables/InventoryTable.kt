package com.neomud.server.persistence.tables

import org.jetbrains.exposed.v1.core.Table

object InventoryTable : Table("inventory") {
    val id = integer("id").autoIncrement()
    val playerName = varchar("player_name", 50).index()
    val itemId = varchar("item_id", 100)
    val quantity = integer("quantity").default(1)
    val equipped = bool("equipped").default(false)
    val slot = varchar("slot", 20).default("")

    override val primaryKey = PrimaryKey(id)
}

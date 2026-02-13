package com.neomud.server.persistence.tables

import org.jetbrains.exposed.sql.Table

object PlayersTable : Table("players") {
    val id = integer("id").autoIncrement()
    val username = varchar("username", 50).uniqueIndex()
    val passwordHash = varchar("password_hash", 128)
    val characterName = varchar("character_name", 50).uniqueIndex()
    val characterClass = varchar("character_class", 20)
    val strength = integer("strength")
    val dexterity = integer("dexterity")
    val constitution = integer("constitution")
    val intelligence = integer("intelligence")
    val wisdom = integer("wisdom")
    val currentHp = integer("current_hp")
    val currentMp = integer("current_mp").default(0)
    val level = integer("level").default(1)
    val currentRoomId = varchar("current_room_id", 100)

    override val primaryKey = PrimaryKey(id)
}

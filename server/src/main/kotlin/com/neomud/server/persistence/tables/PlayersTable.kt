package com.neomud.server.persistence.tables

import org.jetbrains.exposed.sql.Table

object PlayersTable : Table("players") {
    val id = integer("id").autoIncrement()
    val username = varchar("username", 50).uniqueIndex()
    val passwordHash = varchar("password_hash", 128)
    val characterName = varchar("character_name", 50).uniqueIndex()
    val characterClass = varchar("character_class", 20)
    val race = varchar("race", 20).default("")
    val strength = integer("strength")
    val agility = integer("agility")
    val intellect = integer("intellect")
    val willpower = integer("willpower")
    val health = integer("health")
    val charm = integer("charm")
    val currentHp = integer("current_hp")
    val maxHp = integer("max_hp").default(100)
    val currentMp = integer("current_mp").default(0)
    val maxMp = integer("max_mp").default(0)
    val level = integer("level").default(1)
    val currentRoomId = varchar("current_room_id", 100)
    val currentXp = long("current_xp").default(0)
    val xpToNextLevel = long("xp_to_next_level").default(100)
    val unspentCp = integer("unspent_cp").default(0)
    val totalCpEarned = integer("total_cp_earned").default(0)

    override val primaryKey = PrimaryKey(id)
}

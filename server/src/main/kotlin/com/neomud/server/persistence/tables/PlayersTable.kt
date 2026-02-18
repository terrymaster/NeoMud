package com.neomud.server.persistence.tables

import org.jetbrains.exposed.v1.core.Table

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
    val baseStrength = integer("base_strength").default(30)
    val baseAgility = integer("base_agility").default(30)
    val baseIntellect = integer("base_intellect").default(30)
    val baseWillpower = integer("base_willpower").default(30)
    val baseHealth = integer("base_health").default(30)
    val baseCharm = integer("base_charm").default(30)
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
    val isAdmin = bool("is_admin").default(false)
    val gender = varchar("gender", 20).default("neutral")
    val imagePrompt = text("image_prompt").default("")

    override val primaryKey = PrimaryKey(id)
}

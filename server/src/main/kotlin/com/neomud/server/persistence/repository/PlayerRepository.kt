package com.neomud.server.persistence.repository

import com.neomud.server.persistence.tables.PlayersTable
import com.neomud.server.world.ClassCatalog
import com.neomud.shared.model.Player
import com.neomud.shared.model.RoomId
import com.neomud.shared.model.Stats
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.security.MessageDigest

class PlayerRepository {

    fun createPlayer(
        username: String,
        password: String,
        characterName: String,
        characterClass: String,
        spawnRoomId: RoomId,
        classCatalog: ClassCatalog
    ): Result<Player> = runCatching {
        transaction {
            val existing = PlayersTable.selectAll().where {
                (PlayersTable.username eq username) or (PlayersTable.characterName eq characterName)
            }.firstOrNull()

            if (existing != null) {
                if (existing[PlayersTable.username] == username) {
                    error("Username already taken")
                } else {
                    error("Character name already taken")
                }
            }

            val classDef = classCatalog.getClass(characterClass)
                ?: error("Unknown character class: $characterClass")
            val stats = classDef.baseStats
            val maxHp = stats.maxHitPoints
            val maxMp = stats.maxManaPoints

            PlayersTable.insert {
                it[PlayersTable.username] = username
                it[passwordHash] = hashPassword(password)
                it[PlayersTable.characterName] = characterName
                it[PlayersTable.characterClass] = characterClass
                it[strength] = stats.strength
                it[dexterity] = stats.dexterity
                it[constitution] = stats.constitution
                it[intelligence] = stats.intelligence
                it[wisdom] = stats.wisdom
                it[currentHp] = maxHp
                it[currentMp] = maxMp
                it[level] = 1
                it[currentRoomId] = spawnRoomId
            }

            Player(
                name = characterName,
                characterClass = characterClass,
                stats = stats,
                currentHp = maxHp,
                maxHp = maxHp,
                currentMp = maxMp,
                maxMp = maxMp,
                level = 1,
                currentRoomId = spawnRoomId
            )
        }
    }

    fun authenticate(username: String, password: String): Result<Player> = runCatching {
        transaction {
            val row = PlayersTable.selectAll().where {
                PlayersTable.username eq username
            }.firstOrNull() ?: error("Invalid username or password")

            val storedHash = row[PlayersTable.passwordHash]
            if (storedHash != hashPassword(password)) {
                error("Invalid username or password")
            }

            val stats = Stats(
                strength = row[PlayersTable.strength],
                dexterity = row[PlayersTable.dexterity],
                constitution = row[PlayersTable.constitution],
                intelligence = row[PlayersTable.intelligence],
                wisdom = row[PlayersTable.wisdom]
            )

            Player(
                name = row[PlayersTable.characterName],
                characterClass = row[PlayersTable.characterClass],
                stats = stats,
                currentHp = row[PlayersTable.currentHp],
                maxHp = stats.maxHitPoints,
                currentMp = row[PlayersTable.currentMp],
                maxMp = stats.maxManaPoints,
                level = row[PlayersTable.level],
                currentRoomId = row[PlayersTable.currentRoomId]
            )
        }
    }

    fun savePlayerState(player: Player) {
        transaction {
            PlayersTable.update({ PlayersTable.characterName eq player.name }) {
                it[currentHp] = player.currentHp
                it[currentMp] = player.currentMp
                it[level] = player.level
                it[currentRoomId] = player.currentRoomId
                it[strength] = player.stats.strength
                it[dexterity] = player.stats.dexterity
                it[constitution] = player.stats.constitution
                it[intelligence] = player.stats.intelligence
                it[wisdom] = player.stats.wisdom
            }
        }
    }

    private fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(password.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}

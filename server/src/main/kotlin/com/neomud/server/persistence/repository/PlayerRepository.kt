package com.neomud.server.persistence.repository

import com.neomud.server.persistence.tables.PlayersTable
import com.neomud.server.world.ClassCatalog
import com.neomud.server.world.RaceCatalog
import com.neomud.shared.model.Player
import com.neomud.shared.model.RoomId
import com.neomud.shared.model.StatAllocator
import com.neomud.shared.model.Stats
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.security.MessageDigest

class PlayerRepository {

    fun createPlayer(
        username: String,
        password: String,
        characterName: String,
        characterClass: String,
        race: String,
        allocatedStats: Stats,
        spawnRoomId: RoomId,
        classCatalog: ClassCatalog,
        raceCatalog: RaceCatalog?
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
            val raceDef = raceCatalog?.getRace(race)

            // Effective minimums: class mins + race mods (already baked into client allocation)
            val baseStats = if (raceDef != null) {
                Stats(
                    strength = maxOf(1, classDef.minimumStats.strength + raceDef.statModifiers.strength),
                    agility = maxOf(1, classDef.minimumStats.agility + raceDef.statModifiers.agility),
                    intellect = maxOf(1, classDef.minimumStats.intellect + raceDef.statModifiers.intellect),
                    willpower = maxOf(1, classDef.minimumStats.willpower + raceDef.statModifiers.willpower),
                    health = maxOf(1, classDef.minimumStats.health + raceDef.statModifiers.health),
                    charm = maxOf(1, classDef.minimumStats.charm + raceDef.statModifiers.charm)
                )
            } else {
                classDef.minimumStats
            }

            // Validate CP allocation against effective minimums
            if (!StatAllocator.isValidAllocation(allocatedStats, baseStats)) {
                error("Invalid stat allocation")
            }

            // Stats already include race mods from client allocation
            val stats = allocatedStats

            // Level 1 gets max HP roll for fairness
            val maxHp = classDef.hpPerLevelMax + (stats.health / 10) * 4
            val maxMp = if (classDef.mpPerLevelMax > 0) classDef.mpPerLevelMax + (stats.willpower / 10) * 2 else 0
            val initialXpToNext = (100 * Math.pow(1.0, 2.2)).toLong().coerceAtLeast(100)

            PlayersTable.insert {
                it[PlayersTable.username] = username
                it[passwordHash] = hashPassword(password)
                it[PlayersTable.characterName] = characterName
                it[PlayersTable.characterClass] = characterClass
                it[PlayersTable.race] = race
                it[strength] = stats.strength
                it[agility] = stats.agility
                it[intellect] = stats.intellect
                it[willpower] = stats.willpower
                it[health] = stats.health
                it[charm] = stats.charm
                it[baseStrength] = baseStats.strength
                it[baseAgility] = baseStats.agility
                it[baseIntellect] = baseStats.intellect
                it[baseWillpower] = baseStats.willpower
                it[baseHealth] = baseStats.health
                it[baseCharm] = baseStats.charm
                it[currentHp] = maxHp
                it[PlayersTable.maxHp] = maxHp
                it[currentMp] = maxMp
                it[PlayersTable.maxMp] = maxMp
                it[level] = 1
                it[currentRoomId] = spawnRoomId
                it[currentXp] = 0
                it[xpToNextLevel] = initialXpToNext
                it[unspentCp] = 0
                it[totalCpEarned] = StatAllocator.CP_POOL
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
                currentRoomId = spawnRoomId,
                race = race,
                currentXp = 0,
                xpToNextLevel = initialXpToNext,
                unspentCp = 0,
                totalCpEarned = StatAllocator.CP_POOL
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
                agility = row[PlayersTable.agility],
                intellect = row[PlayersTable.intellect],
                willpower = row[PlayersTable.willpower],
                health = row[PlayersTable.health],
                charm = row[PlayersTable.charm]
            )

            Player(
                name = row[PlayersTable.characterName],
                characterClass = row[PlayersTable.characterClass],
                stats = stats,
                currentHp = row[PlayersTable.currentHp],
                maxHp = row[PlayersTable.maxHp],
                currentMp = row[PlayersTable.currentMp],
                maxMp = row[PlayersTable.maxMp],
                level = row[PlayersTable.level],
                currentRoomId = row[PlayersTable.currentRoomId],
                race = row[PlayersTable.race],
                currentXp = row[PlayersTable.currentXp],
                xpToNextLevel = row[PlayersTable.xpToNextLevel],
                unspentCp = row[PlayersTable.unspentCp],
                totalCpEarned = row[PlayersTable.totalCpEarned],
                isAdmin = row[PlayersTable.isAdmin]
            )
        }
    }

    fun getBaseStats(characterName: String): Stats? = transaction {
        PlayersTable.selectAll().where {
            PlayersTable.characterName eq characterName
        }.firstOrNull()?.let { row ->
            Stats(
                strength = row[PlayersTable.baseStrength],
                agility = row[PlayersTable.baseAgility],
                intellect = row[PlayersTable.baseIntellect],
                willpower = row[PlayersTable.baseWillpower],
                health = row[PlayersTable.baseHealth],
                charm = row[PlayersTable.baseCharm]
            )
        }
    }

    fun saveBaseStats(characterName: String, baseStats: Stats) {
        transaction {
            PlayersTable.update({ PlayersTable.characterName eq characterName }) {
                it[baseStrength] = baseStats.strength
                it[baseAgility] = baseStats.agility
                it[baseIntellect] = baseStats.intellect
                it[baseWillpower] = baseStats.willpower
                it[baseHealth] = baseStats.health
                it[baseCharm] = baseStats.charm
            }
        }
    }

    fun savePlayerState(player: Player) {
        transaction {
            PlayersTable.update({ PlayersTable.characterName eq player.name }) {
                it[currentHp] = player.currentHp
                it[maxHp] = player.maxHp
                it[currentMp] = player.currentMp
                it[maxMp] = player.maxMp
                it[level] = player.level
                it[currentRoomId] = player.currentRoomId
                it[strength] = player.stats.strength
                it[agility] = player.stats.agility
                it[intellect] = player.stats.intellect
                it[willpower] = player.stats.willpower
                it[health] = player.stats.health
                it[charm] = player.stats.charm
                it[currentXp] = player.currentXp
                it[xpToNextLevel] = player.xpToNextLevel
                it[unspentCp] = player.unspentCp
                it[totalCpEarned] = player.totalCpEarned
            }
        }
    }

    fun promoteAdmin(username: String) {
        transaction {
            PlayersTable.update({ PlayersTable.username eq username }) {
                it[isAdmin] = true
            }
        }
    }

    private fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(password.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}

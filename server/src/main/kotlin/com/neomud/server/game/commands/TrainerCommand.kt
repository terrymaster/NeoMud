package com.neomud.server.game.commands

import com.neomud.server.game.GameConfig
import com.neomud.server.game.MeditationUtils
import com.neomud.server.game.StealthUtils
import com.neomud.server.game.npc.NpcManager
import com.neomud.server.game.progression.CpAllocator
import com.neomud.server.game.progression.ThresholdBonuses
import com.neomud.server.game.progression.XpCalculator
import com.neomud.server.persistence.repository.PlayerRepository
import com.neomud.server.session.PlayerSession
import com.neomud.server.session.SessionManager
import com.neomud.server.world.ClassCatalog
import com.neomud.server.world.RaceCatalog
import com.neomud.shared.model.Stats
import com.neomud.shared.protocol.ServerMessage

class TrainerCommand(
    private val classCatalog: ClassCatalog,
    private val raceCatalog: RaceCatalog,
    private val playerRepository: PlayerRepository,
    private val sessionManager: SessionManager,
    private val npcManager: NpcManager
) {
    suspend fun handleInteract(session: PlayerSession) {
        val roomId = session.currentRoomId ?: return
        val player = session.player ?: return

        MeditationUtils.breakMeditation(session, "You stop meditating.")
        StealthUtils.breakStealth(session, sessionManager, "Interacting with a trainer reveals your presence!")

        val trainer = npcManager.getTrainerInRoom(roomId)
        if (trainer == null) {
            session.send(ServerMessage.SystemMessage("There is no trainer here."))
            return
        }

        val baseStats = playerRepository.getBaseStats(player.name) ?: player.stats
        val canLevelUp = XpCalculator.isReadyToLevel(player.currentXp, player.xpToNextLevel, player.level)

        session.send(ServerMessage.TrainerInfo(
            canLevelUp = canLevelUp,
            unspentCp = player.unspentCp,
            totalCpEarned = player.totalCpEarned,
            baseStats = baseStats,
            currentStats = player.stats,
            interactSound = trainer.interactSound
        ))
    }

    suspend fun handleLevelUp(session: PlayerSession) {
        val roomId = session.currentRoomId ?: return
        val player = session.player ?: return

        val trainer = npcManager.getTrainerInRoom(roomId)
        if (trainer == null) {
            session.send(ServerMessage.SystemMessage("There is no trainer here."))
            return
        }

        if (!XpCalculator.isReadyToLevel(player.currentXp, player.xpToNextLevel, player.level)) {
            session.send(ServerMessage.SystemMessage("You are not ready to level up yet."))
            return
        }

        if (player.level >= GameConfig.Progression.MAX_LEVEL) {
            session.send(ServerMessage.SystemMessage("You have reached the maximum level."))
            return
        }

        val classDef = classCatalog.getClass(player.characterClass) ?: run {
            session.send(ServerMessage.SystemMessage("Class not found."))
            return
        }

        val raceDef = raceCatalog.getRace(player.race)

        val newLevel = player.level + 1
        val hpRoll = (classDef.hpPerLevelMin..classDef.hpPerLevelMax).random()
        val mpRoll = if (classDef.mpPerLevelMax > 0)
            (classDef.mpPerLevelMin..classDef.mpPerLevelMax).random() else 0

        val thresholds = ThresholdBonuses.compute(player.stats)
        val newMaxHp = player.maxHp + hpRoll + (if (newLevel == 2) thresholds.hpBonus else 0) // Only apply HP threshold bonus once at first level-up
        val newMaxMp = player.maxMp + mpRoll + (if (newLevel == 2) thresholds.mpBonus else 0)

        val cpGained = XpCalculator.cpForLevel(newLevel)
        val newUnspentCp = player.unspentCp + cpGained
        val newTotalCpEarned = player.totalCpEarned + cpGained

        // Calculate XP needed for next level
        val raceXpMod = raceDef?.xpModifier ?: 1.0
        val classXpMod = classDef.xpModifier
        val newXpToNext = XpCalculator.adjustedXpForLevel(newLevel, raceXpMod, classXpMod)

        val updatedPlayer = player.copy(
            level = newLevel,
            maxHp = newMaxHp,
            currentHp = newMaxHp, // Full heal on level up
            maxMp = newMaxMp,
            currentMp = newMaxMp,
            xpToNextLevel = newXpToNext,
            unspentCp = newUnspentCp,
            totalCpEarned = newTotalCpEarned
        )
        session.player = updatedPlayer

        try {
            playerRepository.savePlayerState(updatedPlayer)
        } catch (_: Exception) { }

        session.send(ServerMessage.LevelUp(
            newLevel = newLevel,
            hpRoll = hpRoll,
            newMaxHp = newMaxHp,
            mpRoll = mpRoll,
            newMaxMp = newMaxMp,
            cpGained = cpGained,
            totalUnspentCp = newUnspentCp,
            xpToNextLevel = newXpToNext
        ))

        // Broadcast to room
        sessionManager.broadcastToRoom(
            roomId,
            ServerMessage.SystemMessage("${player.name} has reached level $newLevel!"),
            exclude = player.name
        )
    }

    suspend fun handleTrainStat(session: PlayerSession, stat: String, points: Int) {
        val clampedPoints = points.coerceIn(1, 50)
        val roomId = session.currentRoomId ?: return
        val player = session.player ?: return

        val trainer = npcManager.getTrainerInRoom(roomId)
        if (trainer == null) {
            session.send(ServerMessage.SystemMessage("There is no trainer here."))
            return
        }

        val baseStats = playerRepository.getBaseStats(player.name) ?: return

        val (currentValue, baseValue) = when (stat.lowercase()) {
            "strength" -> player.stats.strength to baseStats.strength
            "agility" -> player.stats.agility to baseStats.agility
            "intellect" -> player.stats.intellect to baseStats.intellect
            "willpower" -> player.stats.willpower to baseStats.willpower
            "health" -> player.stats.health to baseStats.health
            "charm" -> player.stats.charm to baseStats.charm
            else -> {
                session.send(ServerMessage.SystemMessage("Unknown stat: $stat"))
                return
            }
        }

        val result = CpAllocator.allocate(currentValue, baseValue, player.unspentCp, clampedPoints)
        if (result == null) {
            session.send(ServerMessage.SystemMessage("Not enough CP to train $stat."))
            return
        }

        val newStats = when (stat.lowercase()) {
            "strength" -> player.stats.copy(strength = result.newValue)
            "agility" -> player.stats.copy(agility = result.newValue)
            "intellect" -> player.stats.copy(intellect = result.newValue)
            "willpower" -> player.stats.copy(willpower = result.newValue)
            "health" -> player.stats.copy(health = result.newValue)
            "charm" -> player.stats.copy(charm = result.newValue)
            else -> player.stats
        }

        // Recalculate threshold-based maxHp/maxMp
        val oldThresholds = ThresholdBonuses.compute(player.stats)
        val newThresholds = ThresholdBonuses.compute(newStats)
        val hpDelta = newThresholds.hpBonus - oldThresholds.hpBonus
        val mpDelta = newThresholds.mpBonus - oldThresholds.mpBonus

        val updatedPlayer = player.copy(
            stats = newStats,
            unspentCp = result.remainingCp,
            maxHp = player.maxHp + hpDelta,
            currentHp = (player.currentHp + hpDelta).coerceAtMost(player.maxHp + hpDelta),
            maxMp = player.maxMp + mpDelta,
            currentMp = (player.currentMp + mpDelta).coerceAtMost(player.maxMp + mpDelta)
        )
        session.player = updatedPlayer

        try {
            playerRepository.savePlayerState(updatedPlayer)
        } catch (_: Exception) { }

        session.send(ServerMessage.StatTrained(
            stat = stat,
            newValue = result.newValue,
            cpSpent = result.totalCpSpent,
            remainingCp = result.remainingCp
        ))
    }

    suspend fun handleAllocateTrainedStats(session: PlayerSession, newStats: Stats) {
        val roomId = session.currentRoomId ?: return
        val player = session.player ?: return

        val trainer = npcManager.getTrainerInRoom(roomId)
        if (trainer == null) {
            session.send(ServerMessage.SystemMessage("There is no trainer here."))
            return
        }

        val baseStats = playerRepository.getBaseStats(player.name) ?: return

        // Validate no stat goes below base
        if (newStats.strength < baseStats.strength ||
            newStats.agility < baseStats.agility ||
            newStats.intellect < baseStats.intellect ||
            newStats.willpower < baseStats.willpower ||
            newStats.health < baseStats.health ||
            newStats.charm < baseStats.charm
        ) {
            session.send(ServerMessage.SystemMessage("Stats cannot go below base values."))
            return
        }

        val cpUsed = CpAllocator.totalCpUsed(newStats, baseStats)
        if (cpUsed > player.totalCpEarned) {
            session.send(ServerMessage.SystemMessage("Not enough CP for that allocation."))
            return
        }

        // Compute threshold bonus deltas
        val oldThresholds = ThresholdBonuses.compute(player.stats)
        val newThresholds = ThresholdBonuses.compute(newStats)
        val hpDelta = newThresholds.hpBonus - oldThresholds.hpBonus
        val mpDelta = newThresholds.mpBonus - oldThresholds.mpBonus

        val updatedPlayer = player.copy(
            stats = newStats,
            unspentCp = player.totalCpEarned - cpUsed,
            maxHp = player.maxHp + hpDelta,
            currentHp = (player.currentHp + hpDelta).coerceAtMost(player.maxHp + hpDelta),
            maxMp = player.maxMp + mpDelta,
            currentMp = (player.currentMp + mpDelta).coerceAtMost(player.maxMp + mpDelta)
        )
        session.player = updatedPlayer

        try {
            playerRepository.savePlayerState(updatedPlayer)
        } catch (_: Exception) { }

        // Send fresh trainer info
        val canLevelUp = XpCalculator.isReadyToLevel(updatedPlayer.currentXp, updatedPlayer.xpToNextLevel, updatedPlayer.level)
        session.send(ServerMessage.TrainerInfo(
            canLevelUp = canLevelUp,
            unspentCp = updatedPlayer.unspentCp,
            totalCpEarned = updatedPlayer.totalCpEarned,
            baseStats = baseStats,
            currentStats = updatedPlayer.stats,
            interactSound = trainer.interactSound
        ))
    }
}

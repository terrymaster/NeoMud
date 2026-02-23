package com.neomud.server.game.commands

import com.neomud.server.game.MeditationUtils
import com.neomud.server.game.StealthUtils

import com.neomud.server.game.npc.NpcManager
import com.neomud.server.game.progression.XpCalculator
import com.neomud.server.persistence.repository.PlayerRepository
import com.neomud.server.session.PlayerSession
import com.neomud.server.session.SessionManager
import com.neomud.server.world.ClassCatalog
import com.neomud.server.world.SpellCatalog
import com.neomud.shared.model.*
import com.neomud.shared.protocol.ServerMessage

class SpellCommand(
    private val spellCatalog: SpellCatalog,
    private val classCatalog: ClassCatalog,
    private val npcManager: NpcManager,
    private val sessionManager: SessionManager,
    private val playerRepository: PlayerRepository
) {
    suspend fun execute(session: PlayerSession, spellId: String, targetId: String?) {
        val roomId = session.currentRoomId ?: return
        val playerName = session.playerName ?: return
        val player = session.player ?: return

        // Casting a spell breaks meditation and stealth
        MeditationUtils.breakMeditation(session, "You stop meditating.")
        StealthUtils.breakStealth(session, sessionManager, "Casting a spell reveals your presence!")

        val spell = spellCatalog.getSpell(spellId)
        if (spell == null) {
            session.send(ServerMessage.SpellCastResult(false, spellId, "Unknown spell.", player.currentMp))
            return
        }

        // Validate class has school access
        val classDef = classCatalog.getClass(player.characterClass)
        if (classDef == null || !classDef.magicSchools.containsKey(spell.school)) {
            session.send(ServerMessage.SpellCastResult(false, spell.name, "Your class cannot cast ${spell.school} spells.", player.currentMp))
            return
        }

        // Validate level
        if (player.level < spell.levelRequired) {
            session.send(ServerMessage.SpellCastResult(false, spell.name, "You need level ${spell.levelRequired} to cast ${spell.name}.", player.currentMp))
            return
        }

        // Validate MP
        if (player.currentMp < spell.manaCost) {
            session.send(ServerMessage.SpellCastResult(false, spell.name, "Not enough mana! (need ${spell.manaCost}, have ${player.currentMp})", player.currentMp))
            return
        }

        // Validate cooldown
        val cooldown = session.skillCooldowns[spellId]
        if (cooldown != null && cooldown > 0) {
            session.send(ServerMessage.SpellCastResult(false, spell.name, "${spell.name} is on cooldown ($cooldown ticks remaining).", player.currentMp))
            return
        }

        // Deduct MP
        val newMp = player.currentMp - spell.manaCost
        session.player = player.copy(currentMp = newMp)

        // Set cooldown
        if (spell.cooldownTicks > 0) {
            session.skillCooldowns[spellId] = spell.cooldownTicks
        }

        // Calculate spell power using buffed stats
        val effStats = session.effectiveStats()
        val statValue = when (spell.primaryStat) {
            "intellect" -> effStats.intellect
            "willpower" -> effStats.willpower
            "charm" -> effStats.charm
            "strength" -> effStats.strength
            "agility" -> effStats.agility
            else -> effStats.intellect
        }
        val power = spell.basePower + statValue / 3 + player.level / 2 + (1..6).random()

        when (spell.spellType) {
            SpellType.DAMAGE -> handleDamage(session, spell, power, targetId, roomId, playerName)
            SpellType.HEAL -> handleHeal(session, spell, power, playerName)
            SpellType.BUFF -> handleBuff(session, spell, power, playerName)
            SpellType.DOT -> handleDot(session, spell, power, targetId, roomId, playerName)
            SpellType.HOT -> handleHot(session, spell, power, playerName)
        }

        // Persist player state
        session.player?.let { playerRepository.savePlayerState(it) }
    }

    private suspend fun handleDamage(
        session: PlayerSession, spell: SpellDef, power: Int,
        targetId: String?, roomId: String, playerName: String
    ) {
        val target = resolveTarget(session, targetId, roomId)
        if (target == null) {
            session.send(ServerMessage.SpellCastResult(false, spell.name, "No valid target.", session.player!!.currentMp))
            return
        }

        target.engagedPlayerIds.add(playerName)
        target.currentHp -= power
        val castMsg = "$playerName ${spell.castMessage} ${target.name} for $power damage!"

        session.send(ServerMessage.SpellCastResult(true, spell.name, castMsg, session.player!!.currentMp))

        sessionManager.broadcastToRoom(
            roomId,
            ServerMessage.SpellEffect(
                casterName = playerName,
                targetName = target.name,
                spellName = spell.name,
                effectAmount = power,
                targetNewHp = target.currentHp.coerceAtLeast(0),
                targetMaxHp = target.maxHp,
                targetId = target.id
            )
        )

        if (target.currentHp <= 0) {
            handleNpcKill(target, playerName, roomId, session)
        }
    }

    private suspend fun handleDot(
        session: PlayerSession, spell: SpellDef, power: Int,
        targetId: String?, roomId: String, playerName: String
    ) {
        val target = resolveTarget(session, targetId, roomId)
        if (target == null) {
            session.send(ServerMessage.SpellCastResult(false, spell.name, "No valid target.", session.player!!.currentMp))
            return
        }

        target.engagedPlayerIds.add(playerName)
        // Apply initial damage
        val initialDmg = power / 2
        target.currentHp -= initialDmg
        val castMsg = "$playerName ${spell.castMessage} ${target.name}! ($initialDmg initial damage)"

        session.send(ServerMessage.SpellCastResult(true, spell.name, castMsg, session.player!!.currentMp))

        sessionManager.broadcastToRoom(
            roomId,
            ServerMessage.SpellEffect(
                casterName = playerName,
                targetName = target.name,
                spellName = spell.name,
                effectAmount = initialDmg,
                targetNewHp = target.currentHp.coerceAtLeast(0),
                targetMaxHp = target.maxHp,
                targetId = target.id
            )
        )

        if (target.currentHp <= 0) {
            handleNpcKill(target, playerName, roomId, session)
        }
    }

    private suspend fun handleHeal(session: PlayerSession, spell: SpellDef, power: Int, playerName: String) {
        val player = session.player!!
        val newHp = (player.currentHp + power).coerceAtMost(player.maxHp)
        val healed = newHp - player.currentHp
        session.player = player.copy(currentHp = newHp)

        val castMsg = "$playerName ${spell.castMessage} and recovers $healed HP!"
        session.send(ServerMessage.SpellCastResult(true, spell.name, castMsg, session.player!!.currentMp, newHp))

        sessionManager.broadcastToRoom(
            session.currentRoomId!!,
            ServerMessage.SpellEffect(
                casterName = playerName,
                targetName = playerName,
                spellName = spell.name,
                effectAmount = healed,
                targetNewHp = newHp,
                targetMaxHp = player.maxHp,
                isPlayerTarget = true
            )
        )
    }

    private suspend fun handleBuff(session: PlayerSession, spell: SpellDef, power: Int, playerName: String) {
        val effectType = when (spell.effectType) {
            "BUFF_STRENGTH" -> EffectType.BUFF_STRENGTH
            "BUFF_AGILITY" -> EffectType.BUFF_AGILITY
            "BUFF_INTELLECT" -> EffectType.BUFF_INTELLECT
            "BUFF_WILLPOWER" -> EffectType.BUFF_WILLPOWER
            "HASTE" -> EffectType.HASTE
            else -> EffectType.BUFF_STRENGTH
        }

        val effect = ActiveEffect(
            name = spell.name,
            type = effectType,
            remainingTicks = spell.effectDuration,
            magnitude = power
        )

        // Remove existing effect of same name, then add
        session.activeEffects.removeAll { it.name == spell.name }
        session.activeEffects.add(effect)

        val castMsg = "$playerName ${spell.castMessage}! (+$power ${effectType.name.removePrefix("BUFF_").lowercase()} for ${spell.effectDuration} ticks)"
        session.send(ServerMessage.SpellCastResult(true, spell.name, castMsg, session.player!!.currentMp))
        session.send(ServerMessage.ActiveEffectsUpdate(session.activeEffects.toList()))
    }

    private suspend fun handleHot(session: PlayerSession, spell: SpellDef, power: Int, playerName: String) {
        val effect = ActiveEffect(
            name = spell.name,
            type = EffectType.HEAL_OVER_TIME,
            remainingTicks = spell.effectDuration,
            magnitude = power
        )

        session.activeEffects.removeAll { it.name == spell.name }
        session.activeEffects.add(effect)

        val castMsg = "$playerName ${spell.castMessage}! (heals $power/tick for ${spell.effectDuration} ticks)"
        session.send(ServerMessage.SpellCastResult(true, spell.name, castMsg, session.player!!.currentMp))
        session.send(ServerMessage.ActiveEffectsUpdate(session.activeEffects.toList()))
    }

    private fun resolveTarget(session: PlayerSession, targetId: String?, roomId: String): com.neomud.server.game.npc.NpcState? {
        val resolvedId = targetId ?: session.selectedTargetId
        return if (resolvedId != null) {
            val npc = npcManager.getNpcState(resolvedId)
            if (npc != null && npc.currentRoomId == roomId && npc.isAlive) npc else null
        } else {
            npcManager.getLivingHostileNpcsInRoom(roomId).firstOrNull()
        }
    }

    private suspend fun handleNpcKill(target: com.neomud.server.game.npc.NpcState, killerName: String, roomId: String, session: PlayerSession) {
        if (!npcManager.markDead(target.id)) return
        sessionManager.broadcastToRoom(
            roomId,
            ServerMessage.NpcDied(target.id, target.name, killerName, roomId)
        )

        // Award XP
        if (target.xpReward > 0) {
            val player = session.player ?: return
            val xpGained = XpCalculator.xpForKill(target.level, player.level, target.xpReward)
            val newXp = player.currentXp + xpGained
            session.player = player.copy(currentXp = newXp)
            try {
                session.send(ServerMessage.XpGained(xpGained, newXp, player.xpToNextLevel))
                if (XpCalculator.isReadyToLevel(newXp, player.xpToNextLevel, player.level)) {
                    session.send(ServerMessage.SystemMessage("You have enough experience to level up! Visit a trainer."))
                }
            } catch (_: Exception) { }
        }
    }
}

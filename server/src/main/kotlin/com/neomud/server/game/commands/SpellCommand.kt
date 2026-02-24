package com.neomud.server.game.commands

import com.neomud.server.game.GameConfig
import com.neomud.server.game.MeditationUtils
import com.neomud.server.game.StealthUtils
import com.neomud.server.game.npc.NpcManager
import com.neomud.server.game.npc.NpcState
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
    /**
     * Auto-cast a readied spell during the combat tick.
     * Returns true if the spell was cast, false if skipped (cooldown/no target).
     * Clears readiedSpellId if out of mana.
     */
    suspend fun autoCast(session: PlayerSession, spellId: String, targetId: String?, roomId: String): Boolean {
        val player = session.player ?: return false
        val playerName = session.playerName ?: return false

        val spell = spellCatalog.getSpell(spellId) ?: run {
            session.readiedSpellId = null
            return false
        }

        // Cooldown — silently skip, wait for next tick
        val cooldown = session.skillCooldowns[spellId]
        if (cooldown != null && cooldown > 0) return false

        // MP check — clear readied spell if out of mana
        if (player.currentMp < spell.manaCost) {
            session.send(ServerMessage.SpellCastResult(false, spell.name, "Not enough mana! (need ${spell.manaCost}, have ${player.currentMp})", player.currentMp))
            session.readiedSpellId = null
            return false
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
        val power = spell.basePower + statValue / GameConfig.Skills.SPELL_POWER_STAT_DIVISOR + player.level / GameConfig.Skills.SPELL_POWER_LEVEL_DIVISOR + (1..GameConfig.Skills.SPELL_POWER_DICE_SIZE).random()

        when (spell.spellType) {
            SpellType.DAMAGE -> handleDamage(session, spell, power, targetId, roomId, playerName)
            SpellType.DOT -> handleDot(session, spell, power, targetId, roomId, playerName)
            SpellType.HEAL -> handleHeal(session, spell, power, playerName)
            SpellType.BUFF -> handleBuff(session, spell, power, playerName)
            SpellType.HOT -> handleHot(session, spell, power, playerName)
        }

        session.player?.let { playerRepository.savePlayerState(it) }
        return true
    }

    /**
     * Execute a manually-cast spell. Returns the NPC target state if an offensive spell
     * was cast against one (caller should check target.currentHp <= 0 for kill handling).
     */
    suspend fun execute(session: PlayerSession, spellId: String, targetId: String?): NpcState? {
        val roomId = session.currentRoomId ?: return null
        val playerName = session.playerName ?: return null
        val player = session.player ?: return null

        // Casting a spell breaks meditation and stealth
        MeditationUtils.breakMeditation(session, "You stop meditating.")
        StealthUtils.breakStealth(session, sessionManager, "Casting a spell reveals your presence!")

        val spell = spellCatalog.getSpell(spellId)
        if (spell == null) {
            session.send(ServerMessage.SpellCastResult(false, spellId, "Unknown spell.", player.currentMp))
            return null
        }

        // Validate class has school access at required tier
        val classDef = classCatalog.getClass(player.characterClass)
        val schoolLevel = classDef?.magicSchools?.get(spell.school)
        if (schoolLevel == null) {
            session.send(ServerMessage.SpellCastResult(false, spell.name, "Your class cannot cast ${spell.school} spells.", player.currentMp))
            return null
        }
        if (schoolLevel < spell.schoolLevel) {
            session.send(ServerMessage.SpellCastResult(false, spell.name, "Your training in ${spell.school} magic is not advanced enough.", player.currentMp))
            return null
        }

        // Validate level
        if (player.level < spell.levelRequired) {
            session.send(ServerMessage.SpellCastResult(false, spell.name, "You need level ${spell.levelRequired} to cast ${spell.name}.", player.currentMp))
            return null
        }

        // Validate MP
        if (player.currentMp < spell.manaCost) {
            session.send(ServerMessage.SpellCastResult(false, spell.name, "Not enough mana! (need ${spell.manaCost}, have ${player.currentMp})", player.currentMp))
            return null
        }

        // Validate cooldown
        val cooldown = session.skillCooldowns[spellId]
        if (cooldown != null && cooldown > 0) {
            session.send(ServerMessage.SpellCastResult(false, spell.name, "${spell.name} is on cooldown ($cooldown ticks remaining).", player.currentMp))
            return null
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
        val power = spell.basePower + statValue / GameConfig.Skills.SPELL_POWER_STAT_DIVISOR + player.level / GameConfig.Skills.SPELL_POWER_LEVEL_DIVISOR + (1..GameConfig.Skills.SPELL_POWER_DICE_SIZE).random()

        val target = when (spell.spellType) {
            SpellType.DAMAGE -> handleDamage(session, spell, power, targetId, roomId, playerName)
            SpellType.DOT -> handleDot(session, spell, power, targetId, roomId, playerName)
            SpellType.HEAL -> { handleHeal(session, spell, power, playerName); null }
            SpellType.BUFF -> { handleBuff(session, spell, power, playerName); null }
            SpellType.HOT -> { handleHot(session, spell, power, playerName); null }
        }

        // Persist player state
        session.player?.let { playerRepository.savePlayerState(it) }
        return target
    }

    private suspend fun handleDamage(
        session: PlayerSession, spell: SpellDef, power: Int,
        targetId: String?, roomId: String, playerName: String
    ): NpcState? {
        // Offensive spell breaks grace period
        session.combatGraceTicks = 0
        val target = resolveTarget(session, targetId, roomId)
        if (target == null) {
            session.send(ServerMessage.SpellCastResult(false, spell.name, "No valid target.", session.player!!.currentMp))
            return null
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

        return target
    }

    private suspend fun handleDot(
        session: PlayerSession, spell: SpellDef, power: Int,
        targetId: String?, roomId: String, playerName: String
    ): NpcState? {
        // Offensive spell breaks grace period
        session.combatGraceTicks = 0
        val target = resolveTarget(session, targetId, roomId)
        if (target == null) {
            session.send(ServerMessage.SpellCastResult(false, spell.name, "No valid target.", session.player!!.currentMp))
            return null
        }

        target.engagedPlayerIds.add(playerName)
        // Apply initial damage
        val initialDmg = power / GameConfig.Skills.DOT_INITIAL_DAMAGE_DIVISOR
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

        return target
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

    private fun resolveTarget(session: PlayerSession, targetId: String?, roomId: String): NpcState? {
        val resolvedId = targetId ?: session.selectedTargetId
        return if (resolvedId != null) {
            val npc = npcManager.getNpcState(resolvedId)
            if (npc != null && npc.currentRoomId == roomId && npc.isAlive) npc else null
        } else {
            npcManager.getLivingHostileNpcsInRoom(roomId).firstOrNull()
        }
    }
}

package com.neomud.server.game.combat

import com.neomud.server.game.GameConfig
import com.neomud.server.game.inventory.CombatBonuses
import com.neomud.server.game.npc.NpcState
import com.neomud.server.game.progression.ThresholdBonuses
import com.neomud.shared.model.ActiveEffect
import com.neomud.shared.model.EffectType
import com.neomud.shared.model.Stats

object CombatUtils {

    fun effectiveStats(stats: Stats, effects: List<ActiveEffect>): Stats {
        var str = stats.strength
        var agi = stats.agility
        var int = stats.intellect
        var wil = stats.willpower
        for (e in effects) {
            when (e.type) {
                EffectType.BUFF_STRENGTH -> str += e.magnitude
                EffectType.BUFF_AGILITY -> agi += e.magnitude
                EffectType.BUFF_INTELLECT -> int += e.magnitude
                EffectType.BUFF_WILLPOWER -> wil += e.magnitude
                else -> {}
            }
        }
        return stats.copy(strength = str, agility = agi, intellect = int, willpower = wil)
    }

    fun computePlayerAccuracy(
        stats: Stats,
        thresholds: ThresholdBonuses,
        level: Int,
        bonuses: CombatBonuses
    ): Int {
        val base = (stats.strength + stats.agility) / GameConfig.Combat.ACCURACY_STAT_DIVISOR + thresholds.hitBonus + level * GameConfig.Combat.ACCURACY_LEVEL_MULTIPLIER
        return if (bonuses.weaponDamageRange > 0) {
            base + bonuses.totalDamageBonus
        } else {
            base
        }
    }

    fun computeNpcAccuracy(npc: NpcState): Int =
        npc.accuracy + npc.level * GameConfig.Combat.NPC_ACCURACY_LEVEL_MULTIPLIER

    fun computePlayerDefense(
        stats: Stats,
        bonuses: CombatBonuses,
        level: Int
    ): Int =
        stats.agility / GameConfig.Combat.DEFENSE_AGI_DIVISOR + bonuses.totalArmorValue / GameConfig.Combat.DEFENSE_ARMOR_DIVISOR + level

    fun computeNpcDefense(npc: NpcState): Int =
        npc.defense + npc.level * GameConfig.Combat.NPC_DEFENSE_LEVEL_MULTIPLIER

    fun rollToHit(accuracy: Int, defense: Int): Boolean {
        val hitChance = (GameConfig.Combat.BASE_HIT_CHANCE + (accuracy - defense)).coerceIn(GameConfig.Combat.MIN_HIT_CHANCE, GameConfig.Combat.MAX_HIT_CHANCE)
        val roll = (1..100).random()
        return roll <= hitChance
    }

    fun rollEvasion(evasionPercent: Double): Boolean =
        Math.random() < evasionPercent

    /** AGI-scaled dodge chance: 0% at AGI 0, capped at DODGE_MAX_CHANCE. */
    fun playerEvasion(stats: Stats): Double =
        (stats.agility / GameConfig.Combat.DODGE_STAT_DIVISOR * GameConfig.Combat.DODGE_MAX_CHANCE)
            .coerceAtMost(GameConfig.Combat.DODGE_MAX_CHANCE)

    fun npcEvasion(npc: NpcState): Double =
        npc.evasion / GameConfig.Combat.NPC_EVASION_DIVISOR

    /** STR-scaled parry chance: 0% at STR 0, capped at PARRY_MAX_CHANCE. */
    fun playerParry(stats: Stats): Double =
        (stats.strength / GameConfig.Combat.PARRY_STAT_DIVISOR * GameConfig.Combat.PARRY_MAX_CHANCE)
            .coerceAtMost(GameConfig.Combat.PARRY_MAX_CHANCE)

    fun rollParry(parryPercent: Double): Boolean =
        Math.random() < parryPercent

    fun parryReduction(stats: Stats): Int =
        GameConfig.Combat.PARRY_REDUCTION_BASE + stats.strength / GameConfig.Combat.PARRY_REDUCTION_STR_DIVISOR
}

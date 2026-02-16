package com.neomud.server.game.combat

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
        val base = (stats.strength + stats.agility) / 2 + thresholds.hitBonus + level * 2
        return if (bonuses.weaponDamageRange > 0) {
            base + bonuses.totalDamageBonus
        } else {
            base
        }
    }

    fun computeNpcAccuracy(npc: NpcState): Int =
        npc.accuracy + npc.level * 2

    fun computePlayerDefense(
        stats: Stats,
        bonuses: CombatBonuses,
        level: Int
    ): Int =
        stats.agility / 2 + bonuses.totalArmorValue / 2 + level + bonuses.shieldBonus

    fun computeNpcDefense(npc: NpcState): Int =
        npc.defense + npc.level

    fun rollToHit(accuracy: Int, defense: Int): Boolean {
        val hitChance = (50 + (accuracy - defense)).coerceIn(5, 95)
        val roll = (1..100).random()
        return roll <= hitChance
    }

    fun rollEvasion(evasionPercent: Double): Boolean =
        Math.random() < evasionPercent

    fun playerEvasion(thresholds: ThresholdBonuses, effects: List<ActiveEffect>): Double {
        var evasion = thresholds.dodgeChance
        for (e in effects) {
            if (e.type == EffectType.BUFF_AGILITY) {
                evasion += 0.01 * e.magnitude
            }
        }
        return evasion
    }

    fun npcEvasion(npc: NpcState): Double =
        npc.evasion / 100.0
}

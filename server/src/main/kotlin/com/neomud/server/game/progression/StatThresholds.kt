package com.neomud.server.game.progression

import com.neomud.server.game.GameConfig
import com.neomud.shared.model.Stats

data class ThresholdBonuses(
    val meleeDamageBonus: Int = 0,
    val hitBonus: Int = 0,
    val critChance: Double = 0.0,
    val hpBonus: Int = 0,
    val mpBonus: Int = 0
) {
    companion object {
        fun compute(stats: Stats): ThresholdBonuses {
            var meleeDmg = 0
            var hit = 0
            var crit = 0.0
            var hp = 0
            var mp = 0

            // STR thresholds: melee damage + hit
            if (stats.strength >= 90) { meleeDmg += GameConfig.Thresholds.STR_90_MELEE_DAMAGE; hit += GameConfig.Thresholds.STR_90_HIT_BONUS }
            else if (stats.strength >= 75) { meleeDmg += GameConfig.Thresholds.STR_75_MELEE_DAMAGE; hit += GameConfig.Thresholds.STR_75_HIT_BONUS }
            else if (stats.strength >= 60) { meleeDmg += GameConfig.Thresholds.STR_60_MELEE_DAMAGE; hit += GameConfig.Thresholds.STR_60_HIT_BONUS }
            else if (stats.strength >= 40) { meleeDmg += GameConfig.Thresholds.STR_40_MELEE_DAMAGE }

            // AGI thresholds: crit
            if (stats.agility >= 90) { crit += GameConfig.Thresholds.AGI_90_CRIT_CHANCE }

            // INT thresholds: crit
            if (stats.intellect >= 75) { crit += GameConfig.Thresholds.INT_75_CRIT_CHANCE }

            // HEA thresholds: HP
            if (stats.health >= 90) { hp += GameConfig.Thresholds.HEA_90_HP_BONUS }
            else if (stats.health >= 75) { hp += GameConfig.Thresholds.HEA_75_HP_BONUS }
            else if (stats.health >= 60) { hp += GameConfig.Thresholds.HEA_60_HP_BONUS }

            // WIL thresholds: MP
            if (stats.willpower >= 90) { mp += GameConfig.Thresholds.WIL_90_MP_BONUS }
            else if (stats.willpower >= 75) { mp += GameConfig.Thresholds.WIL_75_MP_BONUS }
            else if (stats.willpower >= 60) { mp += GameConfig.Thresholds.WIL_60_MP_BONUS }

            return ThresholdBonuses(
                meleeDamageBonus = meleeDmg,
                hitBonus = hit,
                critChance = crit,
                hpBonus = hp,
                mpBonus = mp
            )
        }
    }
}

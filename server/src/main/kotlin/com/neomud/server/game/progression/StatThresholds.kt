package com.neomud.server.game.progression

import com.neomud.shared.model.Stats

data class ThresholdBonuses(
    val meleeDamageBonus: Int = 0,
    val hitBonus: Int = 0,
    val critChance: Double = 0.0,
    val hpBonus: Int = 0,
    val mpBonus: Int = 0,
    val dodgeChance: Double = 0.0
) {
    companion object {
        fun compute(stats: Stats): ThresholdBonuses {
            var meleeDmg = 0
            var hit = 0
            var crit = 0.0
            var hp = 0
            var mp = 0
            var dodge = 0.0

            // STR thresholds: melee damage
            if (stats.strength >= 90) { meleeDmg += 5; hit += 3 }
            else if (stats.strength >= 75) { meleeDmg += 3; hit += 2 }
            else if (stats.strength >= 60) { meleeDmg += 2; hit += 1 }

            // AGI thresholds: dodge + crit
            if (stats.agility >= 90) { dodge += 0.10; crit += 0.05 }
            else if (stats.agility >= 75) { dodge += 0.05 }
            else if (stats.agility >= 60) { dodge += 0.05 }
            // Note: 60+75 dodge is cumulative only when >= 75 (5+5=10%), and 90 gets 10%+5%crit

            // INT thresholds: crit
            if (stats.intellect >= 75) { crit += 0.05 }

            // HEA thresholds: HP
            if (stats.health >= 90) { hp += 50 }
            else if (stats.health >= 75) { hp += 25 }
            else if (stats.health >= 60) { hp += 15 }

            // WIL thresholds: MP
            if (stats.willpower >= 90) { mp += 25 }
            else if (stats.willpower >= 75) { mp += 15 }
            else if (stats.willpower >= 60) { mp += 10 }

            // CHM thresholds: dodge
            if (stats.charm >= 90) { dodge += 0.05 }
            else if (stats.charm >= 75) { dodge += 0.03 }

            return ThresholdBonuses(
                meleeDamageBonus = meleeDmg,
                hitBonus = hit,
                critChance = crit,
                hpBonus = hp,
                mpBonus = mp,
                dodgeChance = dodge
            )
        }
    }
}

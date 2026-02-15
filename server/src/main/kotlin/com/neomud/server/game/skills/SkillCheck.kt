package com.neomud.server.game.skills

import com.neomud.shared.model.SkillDef
import com.neomud.shared.model.Stats

data class SkillCheckResult(
    val success: Boolean,
    val roll: Int,
    val total: Int,
    val difficulty: Int
)

object SkillCheck {
    fun check(
        skill: SkillDef,
        stats: Stats,
        level: Int,
        difficultyModifier: Int = 0,
        roll: Int = (1..20).random()
    ): SkillCheckResult {
        val primary = getStatValue(skill.primaryStat, stats)
        val secondary = getStatValue(skill.secondaryStat, stats)
        val total = primary + secondary / 2 + level / 2 + roll
        val difficulty = skill.difficulty + difficultyModifier
        return SkillCheckResult(
            success = total >= difficulty,
            roll = roll,
            total = total,
            difficulty = difficulty
        )
    }

    private fun getStatValue(statName: String, stats: Stats): Int = when (statName.lowercase()) {
        "strength" -> stats.strength
        "agility" -> stats.agility
        "intellect" -> stats.intellect
        "willpower" -> stats.willpower
        "health" -> stats.health
        "charm" -> stats.charm
        else -> 0
    }
}

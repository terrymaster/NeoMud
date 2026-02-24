package com.neomud.server.game

object GameConfig {
    object Server {
        const val PORT = 8080
    }
    object Tick {
        const val INTERVAL_MS = 1500L
    }
    object RateLimit {
        const val MAX_MESSAGES_PER_SECOND = 10
        const val BURST_CAPACITY = 20
    }
    object Combat {
        const val GRACE_TICKS = 2
        const val MIN_HIT_CHANCE = 5
        const val MAX_HIT_CHANCE = 95
        const val BASE_HIT_CHANCE = 50
        const val ACCURACY_STAT_DIVISOR = 2
        const val ACCURACY_LEVEL_MULTIPLIER = 2
        const val NPC_ACCURACY_LEVEL_MULTIPLIER = 2
        const val DEFENSE_AGI_DIVISOR = 2
        const val DEFENSE_ARMOR_DIVISOR = 2
        const val NPC_DEFENSE_LEVEL_MULTIPLIER = 1
        const val DODGE_MAX_CHANCE = 0.25
        const val DODGE_STAT_DIVISOR = 100.0
        const val NPC_EVASION_DIVISOR = 100.0
        const val PARRY_MAX_CHANCE = 0.25
        const val PARRY_STAT_DIVISOR = 100.0
        const val PARRY_REDUCTION_BASE = 2
        const val PARRY_REDUCTION_STR_DIVISOR = 20
        const val UNARMED_DAMAGE_RANGE = 3
        const val CRIT_DAMAGE_MULTIPLIER = 1.5
        const val BACKSTAB_DAMAGE_MULTIPLIER = 3
        const val NPC_VARIANCE_DIVISOR = 3
    }
    object Thresholds {
        // STR thresholds
        const val STR_90_MELEE_DAMAGE = 5
        const val STR_90_HIT_BONUS = 3
        const val STR_75_MELEE_DAMAGE = 3
        const val STR_75_HIT_BONUS = 2
        const val STR_60_MELEE_DAMAGE = 2
        const val STR_60_HIT_BONUS = 1
        const val STR_40_MELEE_DAMAGE = 1
        // AGI thresholds
        const val AGI_90_CRIT_CHANCE = 0.05
        // INT thresholds
        const val INT_75_CRIT_CHANCE = 0.05
        // Health thresholds
        const val HEA_90_HP_BONUS = 50
        const val HEA_75_HP_BONUS = 25
        const val HEA_60_HP_BONUS = 15
        // Willpower thresholds
        const val WIL_90_MP_BONUS = 25
        const val WIL_75_MP_BONUS = 15
        const val WIL_60_MP_BONUS = 10
    }
    object Progression {
        const val MAX_LEVEL = 30
        const val XP_BASE_MULTIPLIER = 100
        const val XP_CURVE_EXPONENT = 2.2
        const val XP_MINIMUM = 100L
        const val XP_MOD_5_ABOVE = 1.5
        const val XP_MOD_3_ABOVE = 1.25
        const val XP_MOD_1_ABOVE = 1.1
        const val XP_MOD_SAME = 1.0
        const val XP_MOD_2_BELOW = 0.75
        const val XP_MOD_4_BELOW = 0.5
        const val XP_MOD_5_PLUS_BELOW = 0.25
        const val CP_PER_LEVEL_LOW = 10
        const val CP_PER_LEVEL_MID = 15
        const val CP_PER_LEVEL_HIGH = 20
        const val CP_TIER_2_LEVEL = 10
        const val CP_TIER_3_LEVEL = 20
        const val DEATH_XP_LOSS_PERCENT = 0.05
    }
    object PlayerCreation {
        const val HP_HEALTH_DIVISOR = 10
        const val HP_HEALTH_MULTIPLIER = 4
        const val MP_WILLPOWER_DIVISOR = 10
        const val MP_WILLPOWER_MULTIPLIER = 2
    }
    object Skills {
        const val BASH_DAMAGE_RANGE = 4
        const val BASH_STUN_CHANCE = 50
        const val BASH_STUN_TICKS = 2
        const val BASH_COOLDOWN_TICKS = 3
        const val KICK_DAMAGE_RANGE = 4
        const val KICK_COOLDOWN_TICKS = 2
        const val KICK_KNOCKBACK_STUN_TICKS = 2
        const val SPELL_POWER_STAT_DIVISOR = 3
        const val SPELL_POWER_LEVEL_DIVISOR = 2
        const val SPELL_POWER_DICE_SIZE = 6
        const val DOT_INITIAL_DAMAGE_DIVISOR = 2
    }
    object Stealth {
        const val DC_BASE = 10
        const val DC_WIL_DIVISOR = 2
        const val DC_LEVEL_DIVISOR = 2
        const val PERCEPTION_DICE_SIZE = 20
        const val PERCEPTION_INT_DIVISOR = 2
        const val PERCEPTION_LEVEL_DIVISOR = 2
        const val PERCEPTION_SKILL_BONUS = 3
        const val SNEAK_DIFFICULTY = 15
    }
    object Meditation {
        const val WIL_DIVISOR = 10
        const val RESTORE_BASE = 2
    }
    object Npc {
        const val WANDER_MOVE_TICKS = 15
        const val PATROL_MOVE_TICKS = 20
        const val PURSUIT_MAX_TICKS = 40
        const val PURSUIT_MOVE_TICKS = 5
        const val PURSUIT_LOST_TRAIL_TICKS = 3
    }
    object Trails {
        const val MAX_ENTRIES_PER_ROOM = 20
        const val LIFETIME_MS = 90_000L
        const val STALENESS_PENALTY_MAX = 5
    }
}

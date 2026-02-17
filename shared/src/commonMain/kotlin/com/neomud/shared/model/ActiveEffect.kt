package com.neomud.shared.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class EffectType {
    @SerialName("POISON") POISON,
    @SerialName("HEAL_OVER_TIME") HEAL_OVER_TIME,
    @SerialName("BUFF_STRENGTH") BUFF_STRENGTH,
    @SerialName("BUFF_AGILITY") BUFF_AGILITY,
    @SerialName("HASTE") HASTE,
    @SerialName("BUFF_INTELLECT") BUFF_INTELLECT,
    @SerialName("BUFF_WILLPOWER") BUFF_WILLPOWER,
    @SerialName("DAMAGE") DAMAGE,
    @SerialName("MANA_REGEN") MANA_REGEN,
    @SerialName("MANA_DRAIN") MANA_DRAIN
}

@Serializable
data class ActiveEffect(
    val name: String,
    val type: EffectType,
    val remainingTicks: Int,
    val magnitude: Int
)

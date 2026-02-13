package com.neomud.shared.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class CharacterClass(val baseStats: Stats) {
    @SerialName("WARRIOR")
    WARRIOR(Stats(strength = 16, dexterity = 12, constitution = 14, intelligence = 8, wisdom = 10)),

    @SerialName("MAGE")
    MAGE(Stats(strength = 8, dexterity = 10, constitution = 10, intelligence = 16, wisdom = 14)),

    @SerialName("ROGUE")
    ROGUE(Stats(strength = 10, dexterity = 16, constitution = 12, intelligence = 12, wisdom = 10)),

    @SerialName("CLERIC")
    CLERIC(Stats(strength = 12, dexterity = 10, constitution = 14, intelligence = 10, wisdom = 16));
}

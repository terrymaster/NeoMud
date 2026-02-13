package com.neomud.shared.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class CharacterClass(val baseStats: Stats, val description: String) {
    @SerialName("BARBARIAN")
    BARBARIAN(Stats(strength = 16, dexterity = 12, constitution = 14, intelligence = 8, wisdom = 10), "A fierce warrior driven by primal rage"),

    @SerialName("BARD")
    BARD(Stats(strength = 8, dexterity = 14, constitution = 12, intelligence = 14, wisdom = 12), "A magical entertainer weaving spells through performance"),

    @SerialName("CLERIC")
    CLERIC(Stats(strength = 12, dexterity = 10, constitution = 14, intelligence = 10, wisdom = 16), "A divine champion wielding the power of the gods"),

    @SerialName("DRUID")
    DRUID(Stats(strength = 8, dexterity = 12, constitution = 14, intelligence = 12, wisdom = 16), "A keeper of the old faith, commanding nature's wrath"),

    @SerialName("FIGHTER")
    FIGHTER(Stats(strength = 16, dexterity = 12, constitution = 14, intelligence = 10, wisdom = 10), "A master of martial combat and battlefield tactics"),

    @SerialName("MONK")
    MONK(Stats(strength = 10, dexterity = 16, constitution = 12, intelligence = 10, wisdom = 14), "A martial artist channeling the power of ki"),

    @SerialName("PALADIN")
    PALADIN(Stats(strength = 16, dexterity = 10, constitution = 14, intelligence = 8, wisdom = 12), "A holy warrior bound by a sacred oath"),

    @SerialName("RANGER")
    RANGER(Stats(strength = 12, dexterity = 16, constitution = 12, intelligence = 10, wisdom = 14), "A skilled hunter and tracker of the wilds"),

    @SerialName("ROGUE")
    ROGUE(Stats(strength = 10, dexterity = 16, constitution = 12, intelligence = 14, wisdom = 10), "A scoundrel who uses stealth and cunning"),

    @SerialName("SORCERER")
    SORCERER(Stats(strength = 8, dexterity = 12, constitution = 14, intelligence = 16, wisdom = 12), "A spellcaster drawing on innate magical power"),

    @SerialName("WARLOCK")
    WARLOCK(Stats(strength = 8, dexterity = 12, constitution = 14, intelligence = 14, wisdom = 12), "A wielder of magic granted by an otherworldly pact"),

    @SerialName("WIZARD")
    WIZARD(Stats(strength = 8, dexterity = 10, constitution = 12, intelligence = 16, wisdom = 14), "A scholarly mage whose power comes from study");
}

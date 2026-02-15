package com.neomud.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class CharacterClassDef(
    val id: String,
    val name: String,
    val description: String,
    val baseStats: Stats,
    val skills: List<String> = emptyList(),
    val properties: Map<String, String> = emptyMap(),
    val hpPerLevelMin: Int = 4,
    val hpPerLevelMax: Int = 8,
    val mpPerLevelMin: Int = 0,
    val mpPerLevelMax: Int = 0,
    val xpModifier: Double = 1.0,
    val magicSchools: Map<String, Int> = emptyMap()
)

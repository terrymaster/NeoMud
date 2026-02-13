package com.neomud.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class CharacterClassDef(
    val id: String,
    val name: String,
    val description: String,
    val baseStats: Stats,
    val properties: Map<String, String> = emptyMap()
)

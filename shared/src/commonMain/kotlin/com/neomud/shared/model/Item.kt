package com.neomud.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class Item(
    val id: String,
    val name: String,
    val description: String,
    val type: String,
    val slot: String = "",
    val damageBonus: Int = 0,
    val damageRange: Int = 0,
    val armorValue: Int = 0,
    val value: Int = 0,
    val weight: Int = 0,
    val stackable: Boolean = false,
    val maxStack: Int = 1,
    val useEffect: String = "",
    val levelRequirement: Int = 0,
    val attackSound: String = "",
    val missSound: String = "",
    val useSound: String = ""
)

package com.neomud.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class SkillDef(
    val id: String,
    val name: String,
    val description: String,
    val category: String,
    val primaryStat: String,
    val secondaryStat: String,
    val cooldownTicks: Int = 0,
    val manaCost: Int = 0,
    val difficulty: Int = 15,
    val isPassive: Boolean = false,
    val classRestrictions: List<String> = emptyList(),
    val properties: Map<String, String> = emptyMap()
)

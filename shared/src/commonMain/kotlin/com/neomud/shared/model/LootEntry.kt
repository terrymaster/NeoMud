package com.neomud.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class LootEntry(
    val itemId: String,
    val chance: Double,
    val minQuantity: Int = 1,
    val maxQuantity: Int = 1
)

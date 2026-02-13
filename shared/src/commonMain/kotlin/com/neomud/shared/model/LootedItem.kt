package com.neomud.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class LootedItem(
    val itemId: String,
    val itemName: String,
    val quantity: Int
)

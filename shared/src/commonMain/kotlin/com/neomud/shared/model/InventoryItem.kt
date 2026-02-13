package com.neomud.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class InventoryItem(
    val itemId: String,
    val quantity: Int = 1,
    val equipped: Boolean = false,
    val slot: String = ""
)

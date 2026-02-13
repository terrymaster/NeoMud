package com.neomud.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class GroundItem(
    val itemId: String,
    val quantity: Int
)

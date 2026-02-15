package com.neomud.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class VendorItem(
    val item: Item,
    val price: Coins
)

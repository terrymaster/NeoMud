package com.neomud.client.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neomud.shared.model.Coins
import com.neomud.shared.model.EquipmentSlots
import com.neomud.shared.model.InventoryItem
import com.neomud.shared.model.Item

private val CopperColor = Color(0xFFCD7F32)
private val SilverColor = Color(0xFFC0C0C0)
private val GoldColor = Color(0xFFFFD700)
private val PlatinumColor = Color(0xFFE5E4E2)

@Composable
fun InventoryPanel(
    inventory: List<InventoryItem>,
    equipment: Map<String, String>,
    itemCatalog: Map<String, Item>,
    playerCoins: Coins,
    onEquipItem: (String, String) -> Unit,
    onUnequipItem: (String) -> Unit,
    onUseItem: (String) -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { /* consume all backdrop touches */ }
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .border(1.dp, Color(0xFF55FFFF), RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Inventory",
                    color = Color(0xFF55FFFF),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = onClose,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text("X", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Coins section
            if (!playerCoins.isEmpty()) {
                Text(
                    "Coins",
                    color = Color(0xFFFFFF55),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (playerCoins.platinum > 0) {
                        CoinBadge("${playerCoins.platinum} PP", PlatinumColor)
                    }
                    if (playerCoins.gold > 0) {
                        CoinBadge("${playerCoins.gold} GP", GoldColor)
                    }
                    if (playerCoins.silver > 0) {
                        CoinBadge("${playerCoins.silver} SP", SilverColor)
                    }
                    if (playerCoins.copper > 0) {
                        CoinBadge("${playerCoins.copper} CP", CopperColor)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = Color(0xFF555555))
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Equipment section
            Text(
                "Equipment",
                color = Color(0xFFFFFF55),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))

            for (slot in EquipmentSlots.DEFAULT_SLOTS) {
                val equippedItemId = equipment[slot]
                val item = equippedItemId?.let { itemCatalog[it] }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = slot.replaceFirstChar { it.uppercase() }.padEnd(8),
                        color = Color(0xFFAAAAAA),
                        fontSize = 12.sp,
                        modifier = Modifier.width(64.dp)
                    )
                    if (item != null) {
                        Text(
                            text = item.name,
                            color = Color(0xFF55FF55),
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = { onUnequipItem(slot) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Text("Unequip", fontSize = 10.sp, color = Color(0xFFFF9800))
                        }
                    } else {
                        Text(
                            text = "-- empty --",
                            color = Color(0xFF555555),
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color(0xFF555555))
            Spacer(modifier = Modifier.height(8.dp))

            // Bag section
            Text(
                "Bag",
                color = Color(0xFFFFFF55),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))

            val bagItems = inventory.filter { !it.equipped }
            if (bagItems.isEmpty()) {
                Text("Your bag is empty.", color = Color(0xFF555555), fontSize = 12.sp)
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(bagItems) { invItem ->
                        val item = itemCatalog[invItem.itemId]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = (item?.name ?: invItem.itemId) +
                                    if (invItem.quantity > 1) " x${invItem.quantity}" else "",
                                color = Color(0xFFCCCCCC),
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                            if (item != null && item.slot.isNotEmpty()) {
                                TextButton(
                                    onClick = { onEquipItem(invItem.itemId, item.slot) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                ) {
                                    Text("Equip", fontSize = 10.sp, color = Color(0xFF55FF55))
                                }
                            }
                            if (item != null && item.type == "consumable") {
                                TextButton(
                                    onClick = { onUseItem(invItem.itemId) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                ) {
                                    Text("Use", fontSize = 10.sp, color = Color(0xFF55FFFF))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CoinBadge(text: String, color: Color) {
    Text(
        text = text,
        fontSize = 12.sp,
        color = color,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .background(Color(0xFF2A2A2A), RoundedCornerShape(4.dp))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

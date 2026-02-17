package com.neomud.client.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.neomud.shared.model.Coins
import com.neomud.shared.model.InventoryItem
import com.neomud.shared.model.Item
import com.neomud.shared.model.VendorItem
import com.neomud.shared.protocol.ServerMessage

@Composable
fun VendorPanel(
    vendorInfo: ServerMessage.VendorInfo,
    playerLevel: Int,
    itemCatalog: Map<String, Item>,
    onBuy: (String) -> Unit,
    onSell: (String) -> Unit,
    onClose: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { /* consume clicks */ },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = vendorInfo.vendorName,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD700)
                    )
                    TextButton(onClick = onClose) {
                        Text("Close", color = Color(0xFFAAAAAA))
                    }
                }

                // Player coins
                Text(
                    text = "Your coins: ${vendorInfo.playerCoins.displayString()}",
                    fontSize = 13.sp,
                    color = Color(0xFFFFD700),
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                // Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFF1A1A2E),
                    contentColor = Color(0xFFFFD700),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Buy", fontSize = 14.sp) },
                        selectedContentColor = Color(0xFFFFD700),
                        unselectedContentColor = Color(0xFF888888)
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Sell", fontSize = 14.sp) },
                        selectedContentColor = Color(0xFFFFD700),
                        unselectedContentColor = Color(0xFF888888)
                    )
                }

                HorizontalDivider(color = Color(0xFF333355), modifier = Modifier.padding(vertical = 4.dp))

                // Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (selectedTab == 0) {
                        // Buy tab
                        for (vendorItem in vendorInfo.items) {
                            val ownedQty = vendorInfo.playerInventory
                                .filter { it.itemId == vendorItem.item.id }
                                .sumOf { it.quantity }
                            BuyItemRow(
                                vendorItem = vendorItem,
                                playerCoins = vendorInfo.playerCoins,
                                playerLevel = playerLevel,
                                ownedCount = ownedQty,

                                onBuy = { onBuy(vendorItem.item.id) }
                            )
                        }
                    } else {
                        // Sell tab - show unequipped inventory items
                        val sellableItems = vendorInfo.playerInventory.filter { !it.equipped }
                        if (sellableItems.isEmpty()) {
                            Text(
                                text = "You have nothing to sell.",
                                fontSize = 13.sp,
                                color = Color(0xFF888888),
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        for (invItem in sellableItems) {
                            val item = itemCatalog[invItem.itemId]
                            SellItemRow(
                                inventoryItem = invItem,
                                item = item,
                                playerCharm = vendorInfo.playerCharm,

                                onSell = { onSell(invItem.itemId) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BuyItemRow(
    vendorItem: VendorItem,
    playerCoins: Coins,
    playerLevel: Int,
    ownedCount: Int,
    onBuy: () -> Unit
) {
    val item = vendorItem.item
    val canAfford = playerCoins.totalCopper() >= vendorItem.price.totalCopper()
    val meetsLevel = playerLevel >= item.levelRequirement
    val canBuy = canAfford && meetsLevel
    val context = LocalContext.current
    val serverBaseUrl = LocalServerBaseUrl.current

    val nameColor = when {
        !meetsLevel -> Color(0xFF666666)
        !canAfford -> Color(0xFF888888)
        else -> Color(0xFFCCCCCC)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color(0xFF16213E), RoundedCornerShape(6.dp))
                .border(1.dp, Color(0xFF333355), RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(spriteUrl(serverBaseUrl, item.id))
                    .crossfade(200)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .build(),
                contentDescription = item.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(32.dp).padding(2.dp)
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = nameColor
                )
                if (item.levelRequirement > 1) {
                    Text(
                        text = " Lv${item.levelRequirement}",
                        fontSize = 11.sp,
                        color = if (meetsLevel) Color(0xFF888888) else Color(0xFFCC4444)
                    )
                }
                if (ownedCount > 0) {
                    Text(
                        text = " Owned: $ownedCount",
                        fontSize = 11.sp,
                        color = Color(0xFF66BB6A)
                    )
                }
            }
            val statsText = buildString {
                if (item.slot.isNotEmpty()) append(item.slot)
                if (item.armorValue > 0) append(" | Armor: ${item.armorValue}")
                if (item.damageBonus > 0) append(" | Dmg+${item.damageBonus}")
                if (item.damageRange > 0) append(" Range: ${item.damageRange}")
            }
            if (statsText.isNotEmpty()) {
                Text(
                    text = statsText,
                    fontSize = 11.sp,
                    color = Color(0xFF888888)
                )
            }
        }

        Text(
            text = vendorItem.price.displayString(),
            fontSize = 12.sp,
            color = if (canAfford) Color(0xFFFFD700) else Color(0xFF884444),
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Button(
            onClick = onBuy,
            enabled = canBuy,
            modifier = Modifier.height(32.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1565C0),
                disabledContainerColor = Color(0xFF333333)
            )
        ) {
            Text("Buy", fontSize = 12.sp)
        }
    }
}

@Composable
private fun SellItemRow(
    inventoryItem: InventoryItem,
    item: Item?,
    playerCharm: Int,
    onSell: () -> Unit
) {
    val itemName = item?.name ?: inventoryItem.itemId
    val itemValue = item?.value ?: 0
    val sellPriceCopper = if (itemValue > 0) Coins.sellPriceCopper(itemValue, inventoryItem.quantity, playerCharm) else 0L
    val sellPrice = Coins.fromCopper(sellPriceCopper)
    val canSell = itemValue > 0
    val context = LocalContext.current
    val serverBaseUrl = LocalServerBaseUrl.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color(0xFF16213E), RoundedCornerShape(6.dp))
                .border(1.dp, Color(0xFF333355), RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(spriteUrl(serverBaseUrl, inventoryItem.itemId))
                    .crossfade(200)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .build(),
                contentDescription = itemName,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(32.dp).padding(2.dp)
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = itemName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (canSell) Color(0xFFCCCCCC) else Color(0xFF666666)
                )
                if (inventoryItem.quantity > 1) {
                    Text(
                        text = " x${inventoryItem.quantity}",
                        fontSize = 11.sp,
                        color = Color(0xFF888888)
                    )
                }
            }
        }

        Text(
            text = if (canSell) sellPrice.displayString() else "No value",
            fontSize = 12.sp,
            color = if (canSell) Color(0xFFFFD700) else Color(0xFF666666),
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Button(
            onClick = onSell,
            enabled = canSell,
            modifier = Modifier.height(32.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF2E7D32),
                disabledContainerColor = Color(0xFF333333)
            )
        ) {
            Text("Sell", fontSize = 12.sp)
        }
    }
}

package com.neomud.client.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.neomud.shared.model.Coins
import com.neomud.shared.model.InventoryItem
import com.neomud.shared.model.Item

private val CopperColor = Color(0xFFCD7F32)
private val SilverColor = Color(0xFFC0C0C0)
private val GoldColor = Color(0xFFFFD700)
private val PlatinumColor = Color(0xFFE5E4E2)
private val ConsumableBorder = Color(0xFF55FFFF)
private val DefaultBorder = Color(0xFF555555)
private val CellBgColor = Color(0xFF1A1A2E)

/** Format raw item ID as display name: "item:leather_cap" -> "Leather Cap" */
internal fun displayName(item: Item?, itemId: String): String {
    if (item != null) return item.name
    val raw = itemId.substringAfter(":")
    return raw.split('_').joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
}

@Composable
fun InventoryPanel(
    inventory: List<InventoryItem>,
    itemCatalog: Map<String, Item>,
    playerCoins: Coins,
    serverBaseUrl: String,
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

            CoinsDisplay(playerCoins)

            BagGrid(
                inventory = inventory,
                itemCatalog = itemCatalog,
                serverBaseUrl = serverBaseUrl,
                onUseItem = onUseItem,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun CoinsDisplay(playerCoins: Coins) {
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
            if (playerCoins.platinum > 0) CoinBadge("${playerCoins.platinum} PP", PlatinumColor)
            if (playerCoins.gold > 0) CoinBadge("${playerCoins.gold} GP", GoldColor)
            if (playerCoins.silver > 0) CoinBadge("${playerCoins.silver} SP", SilverColor)
            if (playerCoins.copper > 0) CoinBadge("${playerCoins.copper} CP", CopperColor)
        }
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = Color(0xFF555555))
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun BagGrid(
    inventory: List<InventoryItem>,
    itemCatalog: Map<String, Item>,
    serverBaseUrl: String,
    onUseItem: (String) -> Unit,
    modifier: Modifier = Modifier
) {
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
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(bagItems) { invItem ->
                val item = itemCatalog[invItem.itemId]
                BagItemCell(
                    invItem = invItem,
                    item = item,
                    serverBaseUrl = serverBaseUrl,
                    onUseItem = onUseItem
                )
            }
        }
    }
}

@Composable
private fun BagItemCell(
    invItem: InventoryItem,
    item: Item?,
    serverBaseUrl: String,
    onUseItem: (String) -> Unit
) {
    val isConsumable = item?.type == "consumable"
    val borderColor = if (isConsumable) ConsumableBorder else DefaultBorder
    val context = LocalContext.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.then(
            if (isConsumable) Modifier.clickable { onUseItem(invItem.itemId) }
            else Modifier
        )
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(CellBgColor, RoundedCornerShape(6.dp))
                .border(1.dp, borderColor, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(spriteUrl(serverBaseUrl, invItem.itemId))
                    .crossfade(200)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .build(),
                contentDescription = displayName(item, invItem.itemId),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(48.dp)
                    .padding(2.dp)
            )
            if (invItem.quantity > 1) {
                Text(
                    text = "${invItem.quantity}",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .background(Color(0xCC000000), RoundedCornerShape(3.dp))
                        .padding(horizontal = 3.dp, vertical = 1.dp)
                )
            }
        }
        Text(
            text = displayName(item, invItem.itemId),
            fontSize = 10.sp,
            color = Color(0xFFCCCCCC),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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

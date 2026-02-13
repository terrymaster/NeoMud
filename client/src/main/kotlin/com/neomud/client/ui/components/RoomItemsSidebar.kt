package com.neomud.client.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neomud.shared.model.Coins
import com.neomud.shared.model.GroundItem
import com.neomud.shared.model.Item

private val CopperColor = Color(0xFFCD7F32)
private val SilverColor = Color(0xFFC0C0C0)
private val GoldColor = Color(0xFFFFD700)
private val PlatinumColor = Color(0xFFE5E4E2)
private val ItemColor = Color(0xFF55FFFF)

private data class SidebarEntry(
    val key: String,
    val icon: String,
    val name: String,
    val quantity: Int,
    val color: Color,
    val isCoin: Boolean,
    val coinType: String = "",
    val itemId: String = ""
)

@Composable
fun RoomItemsSidebar(
    groundItems: List<GroundItem>,
    groundCoins: Coins,
    itemCatalog: Map<String, Item>,
    onPickupItem: (String, Int) -> Unit,
    onPickupCoins: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val entries = mutableListOf<SidebarEntry>()

    // Coins: platinum -> gold -> silver -> copper
    if (groundCoins.platinum > 0) entries.add(
        SidebarEntry("coin:platinum", "\u25C9", "Platinum", groundCoins.platinum, PlatinumColor, true, "platinum")
    )
    if (groundCoins.gold > 0) entries.add(
        SidebarEntry("coin:gold", "\u25C9", "Gold", groundCoins.gold, GoldColor, true, "gold")
    )
    if (groundCoins.silver > 0) entries.add(
        SidebarEntry("coin:silver", "\u25C9", "Silver", groundCoins.silver, SilverColor, true, "silver")
    )
    if (groundCoins.copper > 0) entries.add(
        SidebarEntry("coin:copper", "\u25C9", "Copper", groundCoins.copper, CopperColor, true, "copper")
    )

    // Items
    for (gi in groundItems) {
        val item = itemCatalog[gi.itemId]
        val name = item?.name ?: gi.itemId
        entries.add(
            SidebarEntry("item:${gi.itemId}", "\u25A0", name, gi.quantity, ItemColor, false, itemId = gi.itemId)
        )
    }

    LazyColumn(
        modifier = modifier
            .width(44.dp)
            .fillMaxHeight()
            .background(Color(0xFF1A2E1A)),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(entries, key = { it.key }) { entry ->
            Column(
                modifier = Modifier
                    .padding(vertical = 3.dp)
                    .clickable {
                        if (entry.isCoin) {
                            onPickupCoins(entry.coinType)
                        } else {
                            onPickupItem(entry.itemId, entry.quantity)
                        }
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Icon with quantity badge
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, entry.color, CircleShape)
                        .background(Color(0xFF2A4A2A), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = entry.icon,
                        fontSize = 14.sp,
                        color = entry.color
                    )
                    // Quantity badge
                    if (entry.quantity > 1) {
                        Text(
                            text = "${entry.quantity}",
                            fontSize = 7.sp,
                            color = Color.White,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset(x = 2.dp, y = 2.dp)
                                .background(Color(0xCC000000), CircleShape)
                                .padding(horizontal = 2.dp)
                        )
                    }
                }

                // Name (truncated)
                Text(
                    text = entry.name,
                    fontSize = 7.sp,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(42.dp)
                )
            }
        }
    }
}

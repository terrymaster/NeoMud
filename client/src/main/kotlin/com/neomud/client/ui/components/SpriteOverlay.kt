package com.neomud.client.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import com.neomud.shared.model.Coins
import com.neomud.shared.model.GroundItem
import com.neomud.shared.model.Item
import com.neomud.shared.model.Npc

private val SelectedGlow = Color(0xFFFF3333)

fun spriteUrl(serverBaseUrl: String, entityId: String): String {
    val baseId = entityId.substringBefore('#').replace(':', '_')
    val filename = "$baseId.webp"
    return "$serverBaseUrl/assets/images/rooms/$filename"
}

private fun coinSpriteUrl(serverBaseUrl: String, coinType: String): String {
    return "$serverBaseUrl/assets/images/rooms/coin_${coinType}.webp"
}

@Composable
fun SpriteOverlay(
    npcs: List<Npc>,
    groundItems: List<GroundItem>,
    groundCoins: Coins,
    itemCatalog: Map<String, Item>,
    selectedTargetId: String?,
    onSelectTarget: (String?) -> Unit,
    onPickupItem: (String, Int) -> Unit,
    onPickupCoins: (String) -> Unit,
    serverBaseUrl: String,
    readiedSpellId: String? = null,
    onCastSpell: ((String, String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        // NPC sprites — spread along bottom third, horizontally centered
        if (npcs.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(0.35f)
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                npcs.forEach { npc ->
                    val isSelected = npc.id == selectedTargetId
                    val context = androidx.compose.ui.platform.LocalContext.current
                    Box(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .fillMaxHeight()
                            .widthIn(max = 120.dp)
                            .then(
                                if (isSelected) Modifier.border(
                                    2.dp,
                                    SelectedGlow,
                                    RoundedCornerShape(4.dp)
                                ) else Modifier
                            )
                            .clickable(enabled = npc.hostile) {
                                if (npc.hostile) {
                                    if (readiedSpellId != null && onCastSpell != null) {
                                        onCastSpell(readiedSpellId, npc.id)
                                    } else {
                                        onSelectTarget(if (isSelected) null else npc.id)
                                    }
                                }
                            },
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        AsyncImage(
                            model = coil.request.ImageRequest.Builder(context)
                                .data(spriteUrl(serverBaseUrl, npc.id))
                                .crossfade(200)
                                .memoryCachePolicy(CachePolicy.ENABLED)
                                .diskCachePolicy(CachePolicy.ENABLED)
                                .build(),
                            contentDescription = npc.name,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxHeight(0.85f)
                                .widthIn(max = 100.dp)
                        )
                    }
                }
            }
        }

        // Ground loot — items and coins along the bottom
        val hasLoot = groundItems.isNotEmpty() || !groundCoins.isEmpty()
        if (hasLoot) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Bottom
            ) {
                val context = androidx.compose.ui.platform.LocalContext.current

                // Coin piles
                val coinTypes = listOf(
                    Triple("platinum", groundCoins.platinum, "Platinum coins"),
                    Triple("gold", groundCoins.gold, "Gold coins"),
                    Triple("silver", groundCoins.silver, "Silver coins"),
                    Triple("copper", groundCoins.copper, "Copper coins")
                )
                coinTypes.filter { it.second > 0 }.forEach { (type, qty, desc) ->
                    LootSprite(
                        imageUrl = coinSpriteUrl(serverBaseUrl, type),
                        contentDescription = desc,
                        quantity = qty,
                        onClick = { onPickupCoins(type) },
                        context = context
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }

                // Item sprites
                groundItems.take(6).forEach { groundItem ->
                    val item = itemCatalog[groundItem.itemId]
                    LootSprite(
                        imageUrl = spriteUrl(serverBaseUrl, groundItem.itemId),
                        contentDescription = item?.name ?: groundItem.itemId,
                        quantity = groundItem.quantity,
                        onClick = { onPickupItem(groundItem.itemId, groundItem.quantity) },
                        context = context
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
            }
        }
    }
}

@Composable
private fun LootSprite(
    imageUrl: String,
    contentDescription: String,
    quantity: Int,
    onClick: () -> Unit,
    context: android.content.Context
) {
    Box(
        modifier = Modifier.clickable { onClick() }
    ) {
        AsyncImage(
            model = coil.request.ImageRequest.Builder(context)
                .data(imageUrl)
                .crossfade(200)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .build(),
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(40.dp)
        )
        if (quantity > 1) {
            Text(
                text = "$quantity",
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
}

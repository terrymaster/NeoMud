package com.neomud.client.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.crossfade
import com.neomud.shared.model.Coins
import com.neomud.shared.model.GroundItem
import com.neomud.shared.model.Item
import com.neomud.shared.model.Npc
import com.neomud.shared.model.PlayerInfo

private val SelectedGlow = Color(0xFFFF3333)

fun spriteUrl(serverBaseUrl: String, entityId: String): String {
    val baseId = entityId.substringBefore('#').replace(':', '_')
    val filename = "$baseId.webp"
    return "$serverBaseUrl/assets/images/rooms/$filename"
}

fun pcSpriteUrl(serverBaseUrl: String, info: PlayerInfo): String {
    return "$serverBaseUrl/assets/${info.spriteUrl}"
}

private fun coinSpriteUrl(serverBaseUrl: String, coinType: String): String {
    return "$serverBaseUrl/assets/images/rooms/coin_${coinType}.webp"
}

private sealed class RoomEntity {
    data class NpcEntity(val npc: Npc) : RoomEntity()
    data class PcEntity(val info: PlayerInfo) : RoomEntity()
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SpriteOverlay(
    npcs: List<Npc>,
    players: List<PlayerInfo> = emptyList(),
    groundItems: List<GroundItem>,
    groundCoins: Coins,
    itemCatalog: Map<String, Item>,
    selectedTargetId: String?,
    onSelectTarget: (String?) -> Unit,
    onPickupItem: (String, Int) -> Unit,
    onPickupCoins: (String) -> Unit,
    onPlayerTap: ((PlayerInfo) -> Unit)? = null,
    onPlayerLongPress: ((PlayerInfo) -> Unit)? = null,
    readiedSpellId: String? = null,
    onCastSpell: ((String, String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val serverBaseUrl = LocalServerBaseUrl.current
    Box(modifier = modifier.fillMaxSize()) {
        val allEntities = (npcs.map { RoomEntity.NpcEntity(it) } +
                players.map { RoomEntity.PcEntity(it) }).take(8)

        if (allEntities.isNotEmpty()) {
            val totalEntities = npcs.size + players.size
            val backRow = allEntities.filterIndexed { i, _ -> i % 2 == 0 }
            val frontRow = allEntities.filterIndexed { i, _ -> i % 2 == 1 }

            val context = androidx.compose.ui.platform.LocalContext.current

            // Back row — slightly higher, smaller scale
            if (backRow.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .fillMaxHeight(0.45f)
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    backRow.forEach { entity ->
                        EntitySprite(
                            entity = entity,
                            serverBaseUrl = serverBaseUrl,
                            context = context,
                            selectedTargetId = selectedTargetId,
                            onSelectTarget = onSelectTarget,
                            onPlayerTap = onPlayerTap,
                            onPlayerLongPress = onPlayerLongPress,
                            readiedSpellId = readiedSpellId,
                            onCastSpell = onCastSpell,
                            scale = 0.9f,
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .fillMaxHeight()
                                .widthIn(max = 100.dp)
                        )
                    }
                }
            }

            // Front row — at bottom, full scale
            if (frontRow.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .fillMaxHeight(0.40f)
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    frontRow.forEach { entity ->
                        EntitySprite(
                            entity = entity,
                            serverBaseUrl = serverBaseUrl,
                            context = context,
                            selectedTargetId = selectedTargetId,
                            onSelectTarget = onSelectTarget,
                            onPlayerTap = onPlayerTap,
                            onPlayerLongPress = onPlayerLongPress,
                            readiedSpellId = readiedSpellId,
                            onCastSpell = onCastSpell,
                            scale = 1.0f,
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .fillMaxHeight()
                                .widthIn(max = 120.dp)
                        )
                    }
                }
            }

            // Overflow indicator
            if (totalEntities > 8) {
                Text(
                    text = "+${totalEntities - 8} more",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 8.dp, top = 4.dp)
                        .background(Color(0xAA000000), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EntitySprite(
    entity: RoomEntity,
    serverBaseUrl: String,
    context: android.content.Context,
    selectedTargetId: String?,
    onSelectTarget: (String?) -> Unit,
    onPlayerTap: ((PlayerInfo) -> Unit)?,
    onPlayerLongPress: ((PlayerInfo) -> Unit)?,
    readiedSpellId: String?,
    onCastSpell: ((String, String) -> Unit)?,
    scale: Float,
    modifier: Modifier = Modifier
) {
    when (entity) {
        is RoomEntity.NpcEntity -> {
            val npc = entity.npc
            val isSelected = npc.id == selectedTargetId
            Box(
                modifier = modifier
                    .then(
                        if (isSelected) Modifier.border(
                            2.dp, SelectedGlow, RoundedCornerShape(4.dp)
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
                    model = coil3.request.ImageRequest.Builder(context)
                        .data(spriteUrl(serverBaseUrl, npc.id))
                        .crossfade(200)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .build(),
                    contentDescription = npc.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxHeight(0.85f * scale)
                        .widthIn(max = (100 * scale).dp)
                )
            }
        }
        is RoomEntity.PcEntity -> {
            val info = entity.info
            Column(
                modifier = modifier
                    .combinedClickable(
                        onClick = { onPlayerTap?.invoke(info) },
                        onLongClick = { onPlayerLongPress?.invoke(info) }
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                AsyncImage(
                    model = coil3.request.ImageRequest.Builder(context)
                        .data(pcSpriteUrl(serverBaseUrl, info))
                        .crossfade(200)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .build(),
                    contentDescription = info.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .widthIn(max = (100 * scale).dp)
                )
                // Name label below sprite
                Text(
                    text = info.name,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .background(Color(0xAA000000), RoundedCornerShape(3.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                )
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
            model = coil3.request.ImageRequest.Builder(context)
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

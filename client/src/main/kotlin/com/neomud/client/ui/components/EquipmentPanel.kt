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
import com.neomud.shared.model.EquipmentSlots
import com.neomud.shared.model.InventoryItem
import com.neomud.shared.model.Item

private val SlotBgColor = Color(0xFF1A1A2E)
private val SlotEmptyBorder = Color(0xFF555555)
private val SlotFilledBorder = Color(0xFF55FFFF)

@Composable
fun EquipmentPanel(
    inventory: List<InventoryItem>,
    equipment: Map<String, String>,
    itemCatalog: Map<String, Item>,
    onEquipItem: (String, String) -> Unit,
    onUnequipItem: (String) -> Unit,
    onClose: () -> Unit
) {
    val serverBaseUrl = LocalServerBaseUrl.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { /* consume backdrop touches */ }
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
                    "Equipment",
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

            // Everything in a single scrollable column
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // --- Paperdoll ---

                // Head
                EquipmentSlotBox(
                    slot = EquipmentSlots.HEAD,
                    equippedItemId = equipment[EquipmentSlots.HEAD],
                    itemCatalog = itemCatalog,

                    onUnequipItem = onUnequipItem
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Weapon / Chest / Shield
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EquipmentSlotBox(
                        slot = EquipmentSlots.WEAPON,
                        equippedItemId = equipment[EquipmentSlots.WEAPON],
                        itemCatalog = itemCatalog,
    
                        onUnequipItem = onUnequipItem
                    )
                    EquipmentSlotBox(
                        slot = EquipmentSlots.CHEST,
                        equippedItemId = equipment[EquipmentSlots.CHEST],
                        itemCatalog = itemCatalog,
    
                        onUnequipItem = onUnequipItem
                    )
                    EquipmentSlotBox(
                        slot = EquipmentSlots.SHIELD,
                        equippedItemId = equipment[EquipmentSlots.SHIELD],
                        itemCatalog = itemCatalog,
    
                        onUnequipItem = onUnequipItem
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Hands
                EquipmentSlotBox(
                    slot = EquipmentSlots.HANDS,
                    equippedItemId = equipment[EquipmentSlots.HANDS],
                    itemCatalog = itemCatalog,

                    onUnequipItem = onUnequipItem
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Legs
                EquipmentSlotBox(
                    slot = EquipmentSlots.LEGS,
                    equippedItemId = equipment[EquipmentSlots.LEGS],
                    itemCatalog = itemCatalog,

                    onUnequipItem = onUnequipItem
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Feet
                EquipmentSlotBox(
                    slot = EquipmentSlots.FEET,
                    equippedItemId = equipment[EquipmentSlots.FEET],
                    itemCatalog = itemCatalog,

                    onUnequipItem = onUnequipItem
                )

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFF555555))
                Spacer(modifier = Modifier.height(8.dp))

                // --- Equippable bag items ---
                Text(
                    "Equippable Items",
                    color = Color(0xFFFFFF55),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(4.dp))

                // Show unequipped items that have a slot (from catalog or InventoryItem)
                // Also include weapon/armor types even if catalog lookup misses
                val equippableItems = inventory.filter { invItem ->
                    if (invItem.equipped) return@filter false
                    val catalogItem = itemCatalog[invItem.itemId]
                    if (catalogItem != null) {
                        catalogItem.slot.isNotEmpty()
                    } else {
                        // Fallback: check InventoryItem's own slot field
                        invItem.slot.isNotEmpty()
                    }
                }

                if (equippableItems.isEmpty()) {
                    Text(
                        "No equippable items in bag.",
                        color = Color(0xFF555555),
                        fontSize = 12.sp
                    )
                } else {
                    // Render as manual 3-column rows (no LazyVerticalGrid)
                    equippableItems.chunked(3).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            rowItems.forEach { invItem ->
                                val item = itemCatalog[invItem.itemId]
                                val slot = item?.slot?.ifEmpty { null } ?: invItem.slot
                                Box(modifier = Modifier.weight(1f)) {
                                    EquippableBagCell(
                                        invItem = invItem,
                                        item = item,
                    
                                        onEquip = {
                                            if (slot.isNotEmpty()) {
                                                onEquipItem(invItem.itemId, slot)
                                            }
                                        }
                                    )
                                }
                            }
                            // Fill remaining columns with empty space
                            repeat(3 - rowItems.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun EquipmentSlotBox(
    slot: String,
    equippedItemId: String?,
    itemCatalog: Map<String, Item>,
    onUnequipItem: (String) -> Unit
) {
    val serverBaseUrl = LocalServerBaseUrl.current
    val item = equippedItemId?.let { itemCatalog[it] }
    val borderColor = if (equippedItemId != null) SlotFilledBorder else SlotEmptyBorder
    val context = LocalContext.current

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(SlotBgColor, RoundedCornerShape(6.dp))
                .border(1.dp, borderColor, RoundedCornerShape(6.dp))
                .then(
                    if (equippedItemId != null) Modifier.clickable { onUnequipItem(slot) }
                    else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            if (equippedItemId != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(spriteUrl(serverBaseUrl, equippedItemId))
                        .crossfade(200)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .build(),
                    contentDescription = displayName(item, equippedItemId),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(48.dp)
                        .padding(2.dp)
                )
            }
        }
        Text(
            text = slot.replaceFirstChar { it.uppercase() },
            fontSize = 10.sp,
            color = Color(0xFF888888),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun EquippableBagCell(
    invItem: InventoryItem,
    item: Item?,
    onEquip: () -> Unit
) {
    val serverBaseUrl = LocalServerBaseUrl.current
    val context = LocalContext.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEquip() }
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(SlotBgColor, RoundedCornerShape(6.dp))
                .border(1.dp, Color(0xFF55FF55), RoundedCornerShape(6.dp)),
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

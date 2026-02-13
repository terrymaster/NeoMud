package com.neomud.client.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neomud.client.ui.components.CharacterSheet
import com.neomud.client.ui.components.DirectionPad
import com.neomud.client.ui.components.EntitySidebar
import com.neomud.client.ui.components.GameLog
import com.neomud.client.ui.components.InventoryPanel
import com.neomud.client.ui.components.MiniMap
import com.neomud.client.ui.components.PlayerStatusPanel
import com.neomud.client.ui.components.RoomItemsSidebar
import com.neomud.client.viewmodel.GameViewModel

@Composable
fun GameScreen(
    gameViewModel: GameViewModel
) {
    val roomInfo by gameViewModel.roomInfo.collectAsState()
    val mapData by gameViewModel.mapData.collectAsState()
    val gameLog by gameViewModel.gameLog.collectAsState()
    val player by gameViewModel.player.collectAsState()
    val roomEntities by gameViewModel.roomEntities.collectAsState()
    val attackMode by gameViewModel.attackMode.collectAsState()
    val selectedTargetId by gameViewModel.selectedTargetId.collectAsState()
    val showInventory by gameViewModel.showInventory.collectAsState()
    val inventory by gameViewModel.inventory.collectAsState()
    val equipment by gameViewModel.equipment.collectAsState()
    val itemCatalog by gameViewModel.itemCatalog.collectAsState()
    val playerCoins by gameViewModel.playerCoins.collectAsState()
    val roomGroundItems by gameViewModel.roomGroundItems.collectAsState()
    val roomGroundCoins by gameViewModel.roomGroundCoins.collectAsState()
    val activeEffects by gameViewModel.activeEffects.collectAsState()
    val showCharacterSheet by gameViewModel.showCharacterSheet.collectAsState()
    val classCatalog by gameViewModel.classCatalog.collectAsState()

    var sayText by remember { mutableStateOf("") }

    val availableExits = roomInfo?.room?.exits?.keys ?: emptySet()
    val hasHostiles = roomEntities.any { it.hostile }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top: Room Items Sidebar + Mini Map + Entity Sidebar (~35%)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.35f)
            ) {
                // Room items sidebar (left of map)
                RoomItemsSidebar(
                    groundItems = roomGroundItems,
                    groundCoins = roomGroundCoins,
                    itemCatalog = itemCatalog,
                    onPickupItem = { itemId, qty -> gameViewModel.pickupItem(itemId, qty) },
                    onPickupCoins = { coinType -> gameViewModel.pickupCoins(coinType) }
                )

                // Map
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    val data = mapData
                    if (data != null) {
                        MiniMap(
                            rooms = data.rooms,
                            playerRoomId = data.playerRoomId
                        )
                    }
                }

                // Entity sidebar — always visible, scoped to map row
                EntitySidebar(
                    entities = roomEntities,
                    selectedTargetId = selectedTargetId,
                    onSelectTarget = { gameViewModel.selectTarget(it) }
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.primary, thickness = 1.dp)

            // Middle: Game Log (~40%)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.40f)
            ) {
                GameLog(entries = gameLog)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.primary, thickness = 1.dp)

            // Bottom: Controls (~25%)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.25f)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.Top
            ) {
                // D-pad on the left
                DirectionPad(
                    availableExits = availableExits,
                    onMove = { direction -> gameViewModel.move(direction) },
                    onLook = { gameViewModel.look() }
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Right side: action buttons + say bar
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    // Action row: attack button + bag button + player HP
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Attack button
                        val attackBorderColor = when {
                            attackMode -> Color(0xFFFF3333)
                            hasHostiles -> MaterialTheme.colorScheme.primary
                            else -> Color.Gray
                        }
                        val attackBgColor = when {
                            attackMode -> Color(0x44FF3333)
                            else -> Color.Transparent
                        }
                        OutlinedButton(
                            onClick = { gameViewModel.toggleAttackMode(!attackMode) },
                            enabled = hasHostiles || attackMode,
                            modifier = Modifier.size(48.dp),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (attackMode) 2.dp else 1.dp,
                                color = attackBorderColor
                            ),
                            contentPadding = PaddingValues(0.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = attackBgColor,
                                disabledContainerColor = Color.Transparent
                            )
                        ) {
                            Text(
                                text = "\u2694",
                                fontSize = 22.sp,
                                color = attackBorderColor,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Bag button
                        val bagBorderColor = if (showInventory) Color(0xFFFFD700) else MaterialTheme.colorScheme.primary
                        OutlinedButton(
                            onClick = { gameViewModel.toggleInventory() },
                            modifier = Modifier.size(48.dp),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (showInventory) 2.dp else 1.dp,
                                color = bagBorderColor
                            ),
                            contentPadding = PaddingValues(0.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (showInventory) Color(0x44FFD700) else Color.Transparent
                            )
                        ) {
                            Text(
                                text = "\uD83C\uDF92",
                                fontSize = 20.sp,
                                color = bagBorderColor
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Player status panel (HP, MP, effects)
                        val p = player
                        if (p != null && p.maxHp > 0) {
                            PlayerStatusPanel(
                                player = p,
                                activeEffects = activeEffects,
                                onClick = { gameViewModel.toggleCharacterSheet() },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Say bar at the bottom
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = sayText,
                            onValueChange = { sayText = it },
                            placeholder = { Text("Say something...") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Button(
                            onClick = {
                                if (sayText.isNotBlank()) {
                                    gameViewModel.say(sayText)
                                    sayText = ""
                                }
                            },
                            enabled = sayText.isNotBlank()
                        ) {
                            Text("Say")
                        }
                    }
                }
            }
        }

        // Character sheet overlay
        if (showCharacterSheet) {
            val p = player
            if (p != null) {
                CharacterSheet(
                    player = p,
                    classCatalog = classCatalog,
                    equipment = equipment,
                    itemCatalog = itemCatalog,
                    activeEffects = activeEffects,
                    playerCoins = playerCoins,
                    onClose = { gameViewModel.toggleCharacterSheet() }
                )
            }
        }

        // Inventory overlay
        if (showInventory) {
            InventoryPanel(
                inventory = inventory,
                equipment = equipment,
                itemCatalog = itemCatalog,
                playerCoins = playerCoins,
                onEquipItem = { itemId, slot -> gameViewModel.equipItem(itemId, slot) },
                onUnequipItem = { slot -> gameViewModel.unequipItem(slot) },
                onUseItem = { itemId -> gameViewModel.useItem(itemId) },
                onClose = { gameViewModel.toggleInventory() }
            )
        }
    }
}

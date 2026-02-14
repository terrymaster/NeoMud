package com.neomud.client.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neomud.client.ui.components.CharacterSheet
import com.neomud.client.ui.components.DirectionPad
import com.neomud.client.ui.components.EntitySidebar
import com.neomud.client.ui.components.FloatingMiniMap
import com.neomud.client.ui.components.GameLog
import com.neomud.client.ui.components.InventoryPanel
import com.neomud.client.ui.components.MiniMap
import com.neomud.client.ui.components.PlayerStatusPanel
import com.neomud.client.ui.components.RoomBackground
import com.neomud.client.ui.components.RoomItemsSidebar
import com.neomud.client.ui.components.SettingsPanel
import com.neomud.client.ui.components.SpriteOverlay
import com.neomud.client.viewmodel.GameViewModel
import com.neomud.shared.model.Direction

@Composable
fun GameScreen(
    gameViewModel: GameViewModel,
    onLogout: () -> Unit = {}
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
    val showSettings by gameViewModel.showSettings.collectAsState()
    val classCatalog by gameViewModel.classCatalog.collectAsState()
    val deathMessage by gameViewModel.deathMessage.collectAsState()

    var sayText by remember { mutableStateOf("") }

    val availableExits = roomInfo?.room?.exits?.keys ?: emptySet()
    val hasHostiles = roomEntities.any { it.hostile }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val context = LocalContext.current
    val activity = context as? Activity

    val onSetLayoutPreference: (Boolean) -> Unit = { landscape ->
        val prefs = context.getSharedPreferences("neomud_settings", Activity.MODE_PRIVATE)
        prefs.edit().putBoolean("landscape_layout", landscape).apply()
        activity?.requestedOrientation = if (landscape) {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLandscape) {
            GameScreenLandscape(
                gameViewModel = gameViewModel,
                availableExits = availableExits,
                hasHostiles = hasHostiles,
                sayText = sayText,
                onSayTextChange = { sayText = it }
            )
        } else {
            GameScreenPortrait(
                gameViewModel = gameViewModel,
                availableExits = availableExits,
                hasHostiles = hasHostiles,
                sayText = sayText,
                onSayTextChange = { sayText = it }
            )
        }

        // Overlays (shared between layouts)

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

        // Settings overlay
        if (showSettings) {
            SettingsPanel(
                isLandscape = isLandscape,
                onSetLayoutPreference = onSetLayoutPreference,
                onLogout = {
                    gameViewModel.toggleSettings()
                    onLogout()
                },
                onClose = { gameViewModel.toggleSettings() }
            )
        }

        // Death overlay
        if (deathMessage != null) {
            DeathOverlay(
                message = deathMessage!!,
                onDismiss = { gameViewModel.dismissDeath() }
            )
        }
    }
}

@Composable
private fun DeathOverlay(
    message: String,
    onDismiss: () -> Unit
) {
    // Auto-dismiss after 4 seconds
    LaunchedEffect(message) {
        kotlinx.coroutines.delay(4000)
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "YOU DIED",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFCC0000)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                fontSize = 16.sp,
                color = Color(0xFFAAAAAA)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Tap to continue",
                fontSize = 12.sp,
                color = Color(0xFF666666)
            )
        }
    }
}

@Composable
private fun GameScreenPortrait(
    gameViewModel: GameViewModel,
    availableExits: Set<Direction>,
    hasHostiles: Boolean,
    sayText: String,
    onSayTextChange: (String) -> Unit
) {
    val roomInfo by gameViewModel.roomInfo.collectAsState()
    val mapData by gameViewModel.mapData.collectAsState()
    val gameLog by gameViewModel.gameLog.collectAsState()
    val player by gameViewModel.player.collectAsState()
    val roomEntities by gameViewModel.roomEntities.collectAsState()
    val attackMode by gameViewModel.attackMode.collectAsState()
    val selectedTargetId by gameViewModel.selectedTargetId.collectAsState()
    val showInventory by gameViewModel.showInventory.collectAsState()
    val itemCatalog by gameViewModel.itemCatalog.collectAsState()
    val roomGroundItems by gameViewModel.roomGroundItems.collectAsState()
    val roomGroundCoins by gameViewModel.roomGroundCoins.collectAsState()
    val activeEffects by gameViewModel.activeEffects.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // Top: Room background + sidebars + floating minimap (~35%)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.35f)
        ) {
            // Layer 1: Full-bleed background image
            val currentRoom = roomInfo?.room
            RoomBackground(
                imageUrl = currentRoom?.backgroundImage ?: "",
                roomName = currentRoom?.name ?: "",
                serverBaseUrl = gameViewModel.serverBaseUrl,
                modifier = Modifier.fillMaxSize()
            )

            // Layer 1.5: NPC & item sprites overlaid on background
            SpriteOverlay(
                npcs = roomEntities,
                groundItems = roomGroundItems,
                groundCoins = roomGroundCoins,
                itemCatalog = itemCatalog,
                selectedTargetId = selectedTargetId,
                onSelectTarget = { gameViewModel.selectTarget(it) },
                onPickupItem = { itemId, qty -> gameViewModel.pickupItem(itemId, qty) },
                onPickupCoins = { coinType -> gameViewModel.pickupCoins(coinType) },
                serverBaseUrl = gameViewModel.serverBaseUrl,
                modifier = Modifier.fillMaxSize()
            )

            // Layer 2: Floating minimap
            val data = mapData
            if (data != null) {
                FloatingMiniMap(
                    rooms = data.rooms,
                    playerRoomId = data.playerRoomId,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 4.dp, top = 4.dp)
                )
            }
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
            DirectionPad(
                availableExits = availableExits,
                onMove = { direction -> gameViewModel.move(direction) },
                onLook = { gameViewModel.look() }
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                ActionButtonRow(
                    gameViewModel = gameViewModel,
                    attackMode = attackMode,
                    hasHostiles = hasHostiles,
                    showInventory = showInventory,
                    player = player,
                    activeEffects = activeEffects
                )

                Spacer(modifier = Modifier.weight(1f))

                // Say bar + gear in bottom row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SayBar(
                        sayText = sayText,
                        onSayTextChange = onSayTextChange,
                        onSay = { gameViewModel.say(it) },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    SettingsGearButton(onClick = { gameViewModel.toggleSettings() })
                }
            }
        }
    }
}

@Composable
private fun GameScreenLandscape(
    gameViewModel: GameViewModel,
    availableExits: Set<Direction>,
    hasHostiles: Boolean,
    sayText: String,
    onSayTextChange: (String) -> Unit
) {
    val roomInfo by gameViewModel.roomInfo.collectAsState()
    val mapData by gameViewModel.mapData.collectAsState()
    val gameLog by gameViewModel.gameLog.collectAsState()
    val player by gameViewModel.player.collectAsState()
    val roomEntities by gameViewModel.roomEntities.collectAsState()
    val attackMode by gameViewModel.attackMode.collectAsState()
    val selectedTargetId by gameViewModel.selectedTargetId.collectAsState()
    val showInventory by gameViewModel.showInventory.collectAsState()
    val itemCatalog by gameViewModel.itemCatalog.collectAsState()
    val roomGroundItems by gameViewModel.roomGroundItems.collectAsState()
    val roomGroundCoins by gameViewModel.roomGroundCoins.collectAsState()
    val activeEffects by gameViewModel.activeEffects.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // Top row (~55%): Map area + Controls side-by-side
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.55f)
        ) {
            // Left side: Room background + sidebars + floating minimap
            Box(
                modifier = Modifier
                    .weight(0.55f)
                    .fillMaxHeight()
            ) {
                // Layer 1: Full-bleed background image
                val currentRoom = roomInfo?.room
                RoomBackground(
                    imageUrl = currentRoom?.backgroundImage ?: "",
                    roomName = currentRoom?.name ?: "",
                    serverBaseUrl = gameViewModel.serverBaseUrl,
                    modifier = Modifier.fillMaxSize()
                )

                // Layer 1.5: NPC & item sprites overlaid on background
                SpriteOverlay(
                    npcs = roomEntities,
                    groundItems = roomGroundItems,
                    groundCoins = roomGroundCoins,
                    itemCatalog = itemCatalog,
                    selectedTargetId = selectedTargetId,
                    onSelectTarget = { gameViewModel.selectTarget(it) },
                    onPickupItem = { itemId, qty -> gameViewModel.pickupItem(itemId, qty) },
                    onPickupCoins = { coinType -> gameViewModel.pickupCoins(coinType) },
                    serverBaseUrl = gameViewModel.serverBaseUrl,
                    modifier = Modifier.fillMaxSize()
                )

                // Layer 2: Floating minimap
                val data = mapData
                if (data != null) {
                    FloatingMiniMap(
                        rooms = data.rooms,
                        playerRoomId = data.playerRoomId,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(end = 4.dp, top = 4.dp)
                    )
                }
            }

            VerticalDivider(color = MaterialTheme.colorScheme.primary, thickness = 1.dp)

            // Right side: Status, D-pad, Action buttons
            Column(
                modifier = Modifier
                    .weight(0.45f)
                    .fillMaxHeight()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                // Player status panel
                val p = player
                if (p != null && p.maxHp > 0) {
                    PlayerStatusPanel(
                        player = p,
                        activeEffects = activeEffects,
                        onClick = { gameViewModel.toggleCharacterSheet() },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // D-pad and action buttons side by side
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    DirectionPad(
                        availableExits = availableExits,
                        onMove = { direction -> gameViewModel.move(direction) },
                        onLook = { gameViewModel.look() }
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Action buttons + gear in a column
                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        ActionButtonRow(
                            gameViewModel = gameViewModel,
                            attackMode = attackMode,
                            hasHostiles = hasHostiles,
                            showInventory = showInventory,
                            player = null, // Status is above in landscape
                            activeEffects = activeEffects
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        SettingsGearButton(onClick = { gameViewModel.toggleSettings() })
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.primary, thickness = 1.dp)

        // Bottom (~45%): Game log + say bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.45f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                GameLog(entries = gameLog)
            }

            SayBar(
                sayText = sayText,
                onSayTextChange = onSayTextChange,
                onSay = { gameViewModel.say(it) },
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun ActionButtonRow(
    gameViewModel: GameViewModel,
    attackMode: Boolean,
    hasHostiles: Boolean,
    showInventory: Boolean,
    player: com.neomud.shared.model.Player?,
    activeEffects: List<com.neomud.shared.model.ActiveEffect>
) {
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
            border = BorderStroke(
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
            border = BorderStroke(
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

        // Player status panel (only in portrait — landscape puts it above)
        if (player != null && player.maxHp > 0) {
            PlayerStatusPanel(
                player = player,
                activeEffects = activeEffects,
                onClick = { gameViewModel.toggleCharacterSheet() },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SettingsGearButton(onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(28.dp)
    ) {
        Text(
            text = "\u2699\uFE0F",
            fontSize = 16.sp,
            color = Color(0xFFA8A8A8)
        )
    }
}

@Composable
private fun SayBar(
    sayText: String,
    onSayTextChange: (String) -> Unit,
    onSay: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicTextField(
            value = sayText,
            onValueChange = onSayTextChange,
            singleLine = true,
            textStyle = TextStyle(
                fontSize = 13.sp,
                color = Color(0xFFCCCCCC)
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .weight(1f)
                .height(32.dp)
                .border(1.dp, Color(0xFF555555), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp),
            decorationBox = { innerTextField ->
                Box(
                    contentAlignment = Alignment.CenterStart,
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (sayText.isEmpty()) {
                        Text("Say...", fontSize = 13.sp, color = Color(0xFF777777))
                    }
                    innerTextField()
                }
            }
        )
        Spacer(modifier = Modifier.width(4.dp))
        Button(
            onClick = {
                if (sayText.isNotBlank()) {
                    onSay(sayText)
                    onSayTextChange("")
                }
            },
            enabled = sayText.isNotBlank(),
            modifier = Modifier.height(32.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
        ) {
            Text("Say", fontSize = 12.sp)
        }
    }
}

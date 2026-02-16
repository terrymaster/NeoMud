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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neomud.client.ui.theme.MudColors
import com.neomud.client.ui.components.CharacterSheet
import com.neomud.client.ui.components.DirectionPad
import com.neomud.client.ui.components.EntitySidebar
import com.neomud.client.ui.components.FloatingMiniMap
import com.neomud.client.ui.components.GameLog
import com.neomud.client.ui.components.EquipmentPanel
import com.neomud.client.ui.components.InventoryPanel
import com.neomud.client.ui.components.MiniMap
import com.neomud.client.ui.components.PlayerStatusPanel
import com.neomud.client.ui.components.RoomBackground
import com.neomud.client.ui.components.RoomItemsSidebar
import com.neomud.client.audio.AudioManager
import com.neomud.client.ui.components.SettingsPanel
import com.neomud.client.ui.components.SpellBar
import com.neomud.client.ui.components.SpellPicker
import com.neomud.client.ui.components.SpriteOverlay
import com.neomud.client.ui.components.TrainerPanel
import com.neomud.client.ui.components.VendorPanel
import com.neomud.client.viewmodel.GameViewModel
import com.neomud.shared.model.Direction

@Composable
fun GameScreen(
    gameViewModel: GameViewModel,
    onLogout: () -> Unit = {},
    audioManager: AudioManager? = null
) {
    val roomInfo by gameViewModel.roomInfo.collectAsState()
    val mapData by gameViewModel.mapData.collectAsState()
    val gameLog by gameViewModel.gameLog.collectAsState()
    val player by gameViewModel.player.collectAsState()
    val roomEntities by gameViewModel.roomEntities.collectAsState()
    val attackMode by gameViewModel.attackMode.collectAsState()
    val selectedTargetId by gameViewModel.selectedTargetId.collectAsState()
    val showInventory by gameViewModel.showInventory.collectAsState()
    val showEquipment by gameViewModel.showEquipment.collectAsState()
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
    val showTrainer by gameViewModel.showTrainer.collectAsState()
    val trainerInfo by gameViewModel.trainerInfo.collectAsState()
    val showVendor by gameViewModel.showVendor.collectAsState()
    val vendorInfo by gameViewModel.vendorInfo.collectAsState()
    val spellCatalogState by gameViewModel.spellCatalog.collectAsState()
    val spellSlots by gameViewModel.spellSlots.collectAsState()
    val readiedSpellId by gameViewModel.readiedSpellId.collectAsState()
    val showSpellPicker by gameViewModel.showSpellPicker.collectAsState()
    val editingSlotIndex by gameViewModel.editingSlotIndex.collectAsState()

    var sayText by remember { mutableStateOf("") }

    val isHidden by gameViewModel.isHidden.collectAsState()
    val skillCatalog by gameViewModel.skillCatalog.collectAsState()

    val availableExits = roomInfo?.room?.exits?.keys ?: emptySet()
    val hasHostiles = roomEntities.any { it.hostile }
    val hasTrainer = roomEntities.any { it.behaviorType == "trainer" }
    val canLevelUp = player?.let { it.currentXp >= it.xpToNextLevel && it.level < 30 } == true
    val showTrainerButton = hasTrainer && (canLevelUp || (player?.unspentCp ?: 0) > 0)
    val hasVendor = roomEntities.any { it.behaviorType == "vendor" }

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
                showTrainerButton = showTrainerButton,
                showVendorButton = hasVendor,
                sayText = sayText,
                onSayTextChange = { sayText = it }
            )
        } else {
            GameScreenPortrait(
                gameViewModel = gameViewModel,
                availableExits = availableExits,
                hasHostiles = hasHostiles,
                showTrainerButton = showTrainerButton,
                showVendorButton = hasVendor,
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
                    skillCatalog = skillCatalog,
                    spellCatalog = spellCatalogState,
                    isHidden = isHidden,
                    onClose = { gameViewModel.toggleCharacterSheet() }
                )
            }
        }

        // Inventory overlay
        if (showInventory) {
            InventoryPanel(
                inventory = inventory,
                itemCatalog = itemCatalog,
                playerCoins = playerCoins,
                serverBaseUrl = gameViewModel.serverBaseUrl,
                onUseItem = { itemId -> gameViewModel.useItem(itemId) },
                onClose = { gameViewModel.toggleInventory() }
            )
        }

        // Equipment overlay
        if (showEquipment) {
            EquipmentPanel(
                inventory = inventory,
                equipment = equipment,
                itemCatalog = itemCatalog,
                serverBaseUrl = gameViewModel.serverBaseUrl,
                onEquipItem = { itemId, slot -> gameViewModel.equipItem(itemId, slot) },
                onUnequipItem = { slot -> gameViewModel.unequipItem(slot) },
                onClose = { gameViewModel.toggleEquipment() }
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
                onClose = { gameViewModel.toggleSettings() },
                audioManager = audioManager
            )
        }

        // Trainer overlay
        if (showTrainer) {
            val info = trainerInfo
            if (info != null) {
                TrainerPanel(
                    trainerInfo = info,
                    onLevelUp = { gameViewModel.trainLevelUp() },
                    onAllocateStats = { stats -> gameViewModel.allocateTrainedStats(stats) },
                    onClose = { gameViewModel.dismissTrainer() }
                )
            }
        }

        // Vendor overlay
        if (showVendor) {
            val info = vendorInfo
            if (info != null) {
                VendorPanel(
                    vendorInfo = info,
                    playerLevel = player?.level ?: 1,
                    itemCatalog = itemCatalog,
                    onBuy = { itemId -> gameViewModel.buyItem(itemId) },
                    onSell = { itemId -> gameViewModel.sellItem(itemId) },
                    onClose = { gameViewModel.dismissVendor() }
                )
            }
        }

        // Spell picker overlay
        if (showSpellPicker) {
            val slotIdx = editingSlotIndex
            if (slotIdx != null) {
                val p = player
                val cd = p?.let { classCatalog[it.characterClass] }
                SpellPicker(
                    slotIndex = slotIdx,
                    spells = spellCatalogState.values.toList(),
                    classDef = cd,
                    playerLevel = p?.level ?: 1,
                    onAssignSpell = { spellId -> gameViewModel.assignSpellToSlot(slotIdx, spellId) },
                    onClose = { gameViewModel.dismissSpellPicker() }
                )
            }
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
    showTrainerButton: Boolean,
    showVendorButton: Boolean,
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
    val showEquipmentState by gameViewModel.showEquipment.collectAsState()
    val itemCatalog by gameViewModel.itemCatalog.collectAsState()
    val roomGroundItems by gameViewModel.roomGroundItems.collectAsState()
    val roomGroundCoins by gameViewModel.roomGroundCoins.collectAsState()
    val activeEffects by gameViewModel.activeEffects.collectAsState()
    val isHidden by gameViewModel.isHidden.collectAsState()
    val classCatalog by gameViewModel.classCatalog.collectAsState()
    val readiedSpellId by gameViewModel.readiedSpellId.collectAsState()
    val spellSlots by gameViewModel.spellSlots.collectAsState()
    val spellCatalogState by gameViewModel.spellCatalog.collectAsState()

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
                readiedSpellId = readiedSpellId,
                onCastSpell = { spellId, targetId -> gameViewModel.castSpell(spellId, targetId) },
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
                    showEquipment = showEquipmentState,
                    player = player,
                    activeEffects = activeEffects,
                    isHidden = isHidden,
                    spellSlots = spellSlots,
                    spellCatalog = spellCatalogState,
                    readiedSpellId = readiedSpellId,
                    classCatalog = classCatalog
                )

                Spacer(modifier = Modifier.weight(1f))

                // Say bar + gear/trainer in bottom row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SayBar(
                        sayText = sayText,
                        onSayTextChange = onSayTextChange,
                        onSay = { gameViewModel.say(it) },
                        isAdmin = player?.isAdmin == true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    if (showVendorButton) {
                        VendorShopButton(onClick = { gameViewModel.interactVendor() })
                    }
                    if (showTrainerButton) {
                        TrainerStarButton(onClick = { gameViewModel.interactTrainer() })
                    }
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
    showTrainerButton: Boolean,
    showVendorButton: Boolean,
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
    val showEquipmentState by gameViewModel.showEquipment.collectAsState()
    val itemCatalog by gameViewModel.itemCatalog.collectAsState()
    val roomGroundItems by gameViewModel.roomGroundItems.collectAsState()
    val roomGroundCoins by gameViewModel.roomGroundCoins.collectAsState()
    val activeEffects by gameViewModel.activeEffects.collectAsState()
    val isHidden by gameViewModel.isHidden.collectAsState()
    val classCatalog by gameViewModel.classCatalog.collectAsState()
    val readiedSpellId by gameViewModel.readiedSpellId.collectAsState()
    val spellSlots by gameViewModel.spellSlots.collectAsState()
    val spellCatalogState by gameViewModel.spellCatalog.collectAsState()

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
                    readiedSpellId = readiedSpellId,
                    onCastSpell = { spellId, targetId -> gameViewModel.castSpell(spellId, targetId) },
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
                        isHidden = isHidden,
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
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        ActionButtonRow(
                            gameViewModel = gameViewModel,
                            attackMode = attackMode,
                            hasHostiles = hasHostiles,
                            showInventory = showInventory,
                            showEquipment = showEquipmentState,
                            player = null, // Status is above in landscape
                            activeEffects = activeEffects,
                            isHidden = isHidden,
                            spellSlots = spellSlots,
                            spellCatalog = spellCatalogState,
                            readiedSpellId = readiedSpellId,
                            classCatalog = classCatalog,
                            playerCharacterClass = player?.characterClass,
                            currentMp = player?.currentMp ?: 0
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (showVendorButton) {
                                VendorShopButton(onClick = { gameViewModel.interactVendor() })
                            }
                            if (showTrainerButton) {
                                TrainerStarButton(onClick = { gameViewModel.interactTrainer() })
                            }
                            SettingsGearButton(onClick = { gameViewModel.toggleSettings() })
                        }
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
                isAdmin = player?.isAdmin == true,
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
    showEquipment: Boolean = false,
    player: com.neomud.shared.model.Player?,
    activeEffects: List<com.neomud.shared.model.ActiveEffect>,
    isHidden: Boolean = false,
    spellSlots: List<String?> = listOf(null, null, null, null),
    spellCatalog: Map<String, com.neomud.shared.model.SpellDef> = emptyMap(),
    readiedSpellId: String? = null,
    classCatalog: Map<String, com.neomud.shared.model.CharacterClassDef> = emptyMap(),
    playerCharacterClass: String? = player?.characterClass,
    currentMp: Int = player?.currentMp ?: 0
) {
    Column {
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

        // Hide/Stealth button (available to all classes)
        val hideEnabled = !attackMode || isHidden
        val hideBorderColor = when {
            isHidden -> MudColors.stealth
            else -> MaterialTheme.colorScheme.primary
        }
        val hideBgColor = if (isHidden) Color(0x44888888) else Color.Transparent
        OutlinedButton(
            onClick = { gameViewModel.toggleHideMode(!isHidden) },
            enabled = hideEnabled,
            modifier = Modifier.size(48.dp),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(
                width = if (isHidden) 2.dp else 1.dp,
                color = hideBorderColor
            ),
            contentPadding = PaddingValues(0.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = hideBgColor,
                disabledContainerColor = Color.Transparent
            )
        ) {
            // Dark cloak icon
            Text(
                text = "\uD83E\uDDE5",
                fontSize = 20.sp,
                color = hideBorderColor
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Equipment button
        val equipBorderColor = if (showEquipment) Color(0xFFFFD700) else MaterialTheme.colorScheme.primary
        val equipBgColor = if (showEquipment) Color(0x44FFD700) else Color.Transparent
        OutlinedButton(
            onClick = { gameViewModel.toggleEquipment() },
            modifier = Modifier.size(48.dp),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(
                width = if (showEquipment) 2.dp else 1.dp,
                color = equipBorderColor
            ),
            contentPadding = PaddingValues(0.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = equipBgColor
            )
        ) {
            Text(
                text = "\uD83D\uDEE1\uFE0F",
                fontSize = 20.sp,
                color = equipBorderColor
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
                isHidden = isHidden,
                onClick = { gameViewModel.toggleCharacterSheet() },
                modifier = Modifier.weight(1f)
            )
        }
    }

    // Spell bar on its own row (only if class has magic schools)
    val hasMagic = playerCharacterClass != null && classCatalog[playerCharacterClass]?.magicSchools?.isNotEmpty() == true
    if (hasMagic) {
        Spacer(modifier = Modifier.height(4.dp))
        SpellBar(
            spellSlots = spellSlots,
            spellCatalog = spellCatalog,
            readiedSpellId = readiedSpellId,
            currentMp = currentMp,
            onReadySpell = { gameViewModel.readySpell(it) },
            onOpenSpellPicker = { gameViewModel.openSpellPicker(it) }
        )
    }
    } // end Column
}

@Composable
private fun VendorShopButton(onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(28.dp)
    ) {
        Text(
            text = "\uD83D\uDEE0\uFE0F",
            fontSize = 16.sp,
            color = Color(0xFFCC8833)
        )
    }
}

@Composable
private fun TrainerStarButton(onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(28.dp)
    ) {
        Text(
            text = "\u2B50",
            fontSize = 16.sp,
            color = Color(0xFFFFD700)
        )
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

private val ADMIN_COMMANDS = listOf(
    "/broadcast", "/godmode", "/grantcp", "/grantitem", "/grantxp",
    "/heal", "/help", "/kill", "/setlevel", "/setstat", "/spawn", "/teleport"
)

@Composable
private fun SayBar(
    sayText: String,
    onSayTextChange: (String) -> Unit,
    onSay: (String) -> Unit,
    isAdmin: Boolean = false,
    modifier: Modifier = Modifier
) {
    // Internal TextFieldValue for selection control
    var tfv by remember { mutableStateOf(TextFieldValue(sayText)) }

    // Sync external sayText changes (e.g. cleared after send) into our TextFieldValue
    LaunchedEffect(sayText) {
        if (sayText != tfv.text) {
            tfv = TextFieldValue(sayText, TextRange(sayText.length))
        }
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicTextField(
            value = tfv,
            onValueChange = { newTfv ->
                val newText = newTfv.text
                val cursorPos = newTfv.selection.start

                // Determine if the user is typing forward (not deleting/navigating)
                val isTypingForward = newText.length > tfv.text.length ||
                    (newText.length == tfv.text.length && newTfv.selection.collapsed && !tfv.selection.collapsed)

                if (isAdmin && newText.startsWith("/") && !newText.contains(" ") && isTypingForward && newTfv.selection.collapsed) {
                    val prefix = newText.lowercase()
                    val match = ADMIN_COMMANDS.firstOrNull { it.startsWith(prefix) && it != prefix }
                    if (match != null) {
                        // Fill in the completion with the suffix selected
                        tfv = TextFieldValue(
                            text = match,
                            selection = TextRange(cursorPos, match.length)
                        )
                        onSayTextChange(match)
                        return@BasicTextField
                    }
                }

                tfv = newTfv
                onSayTextChange(newText)
            },
            singleLine = true,
            textStyle = TextStyle(
                fontSize = 13.sp,
                color = if (isAdmin && tfv.text.startsWith("/")) Color(0xFFFFD700) else Color(0xFFCCCCCC)
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
                    if (tfv.text.isEmpty()) {
                        Text("Say...", fontSize = 13.sp, color = Color(0xFF777777))
                    }
                    innerTextField()
                }
            }
        )
        Spacer(modifier = Modifier.width(4.dp))
        Button(
            onClick = {
                val text = tfv.text
                if (text.isNotBlank()) {
                    onSay(text)
                    onSayTextChange("")
                }
            },
            enabled = tfv.text.isNotBlank(),
            modifier = Modifier.height(32.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
        ) {
            Text("Say", fontSize = 12.sp)
        }
    }
}

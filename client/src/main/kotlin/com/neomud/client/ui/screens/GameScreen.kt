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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
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
import com.neomud.client.ui.theme.StoneTheme
import com.neomud.client.ui.components.StoneButton
import com.neomud.client.ui.components.StoneDivider
import com.neomud.client.ui.components.ThemedFrame
import com.neomud.client.ui.components.CharacterSheet
import com.neomud.client.ui.components.DirectionPad
import com.neomud.client.ui.components.EntitySidebar
import com.neomud.client.ui.components.FloatingMiniMap
import com.neomud.client.ui.components.GameLog
import com.neomud.client.ui.components.EquipmentPanel
import com.neomud.client.ui.components.InventoryPanel
import com.neomud.client.ui.components.LocalServerBaseUrl
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
import com.neomud.client.ui.components.LockTargetPicker
import com.neomud.client.ui.components.PlayerTooltip
import com.neomud.client.viewmodel.GameViewModel
import com.neomud.shared.model.Direction
import com.neomud.shared.model.PlayerInfo

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

    val showLockTargetPicker by gameViewModel.showLockTargetPicker.collectAsState()

    val roomPlayers by gameViewModel.roomPlayers.collectAsState()
    var tooltipPlayer by remember { mutableStateOf<PlayerInfo?>(null) }

    var sayText by remember { mutableStateOf("") }

    val isHidden by gameViewModel.isHidden.collectAsState()
    val isMeditating by gameViewModel.isMeditating.collectAsState()
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

    CompositionLocalProvider(LocalServerBaseUrl provides gameViewModel.serverBaseUrl) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (isLandscape) {
            GameScreenLandscape(
                gameViewModel = gameViewModel,
                availableExits = availableExits,
                hasHostiles = hasHostiles,
                showTrainerButton = showTrainerButton,
                showVendorButton = hasVendor,
                sayText = sayText,
                onSayTextChange = { sayText = it },
                roomPlayers = roomPlayers,
                onPlayerTap = { gameViewModel.selectTarget(it.name) },
                onPlayerLongPress = { tooltipPlayer = it }
            )
        } else {
            GameScreenPortrait(
                gameViewModel = gameViewModel,
                availableExits = availableExits,
                hasHostiles = hasHostiles,
                showTrainerButton = showTrainerButton,
                showVendorButton = hasVendor,
                sayText = sayText,
                onSayTextChange = { sayText = it },
                roomPlayers = roomPlayers,
                onPlayerTap = { gameViewModel.selectTarget(it.name) },
                onPlayerLongPress = { tooltipPlayer = it }
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

        // Lock target picker overlay
        if (showLockTargetPicker) {
            val lockedExits = roomInfo?.room?.lockedExits ?: emptyMap()
            if (lockedExits.isNotEmpty()) {
                LockTargetPicker(
                    lockedExits = lockedExits,
                    onSelect = { targetId -> gameViewModel.pickLockTarget(targetId) },
                    onDismiss = { gameViewModel.dismissLockTargetPicker() }
                )
            }
        }

        // Player tooltip overlay
        if (tooltipPlayer != null) {
            PlayerTooltip(
                playerInfo = tooltipPlayer!!,
                onDismiss = { tooltipPlayer = null }
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
    } // CompositionLocalProvider
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
    onSayTextChange: (String) -> Unit,
    roomPlayers: List<PlayerInfo> = emptyList(),
    onPlayerTap: ((PlayerInfo) -> Unit)? = null,
    onPlayerLongPress: ((PlayerInfo) -> Unit)? = null,
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
    val trackedDirection by gameViewModel.trackedDirection.collectAsState()
    val skillCatalog by gameViewModel.skillCatalog.collectAsState()

    // Determine if player has TRACK skill
    val hasTrackSkill = player?.characterClass?.let { classCatalog[it] }?.skills?.contains("TRACK") == true

    Column(modifier = Modifier.fillMaxSize().background(StoneTheme.panelBg)) {
        // Top: Room background + sidebars + floating minimap (~35%)
        ThemedFrame(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.35f),
            cornerAccent = true
        ) {
            // Layer 1: Full-bleed background image
            val currentRoom = roomInfo?.room
            RoomBackground(
                imageUrl = currentRoom?.backgroundImage ?: "",
                roomName = currentRoom?.name ?: "",

                modifier = Modifier.fillMaxSize()
            )

            // Layer 1.5: NPC & PC & item sprites overlaid on background
            SpriteOverlay(
                npcs = roomEntities,
                players = roomPlayers,
                groundItems = roomGroundItems,
                groundCoins = roomGroundCoins,
                itemCatalog = itemCatalog,
                selectedTargetId = selectedTargetId,
                onSelectTarget = { gameViewModel.selectTarget(it) },
                onPickupItem = { itemId, qty -> gameViewModel.pickupItem(itemId, qty) },
                onPickupCoins = { coinType -> gameViewModel.pickupCoins(coinType) },
                onPlayerTap = onPlayerTap,
                onPlayerLongPress = onPlayerLongPress,
                readiedSpellId = readiedSpellId,
                onCastSpell = { spellId, targetId -> gameViewModel.castSpell(spellId, targetId) },
                spellSlots = spellSlots,
                spellCatalog = spellCatalogState,
                classCatalog = classCatalog,
                playerCharacterClass = player?.characterClass,
                onAttackTarget = { gameViewModel.attackTarget(it) },
                onTrackTarget = if (hasTrackSkill) { npcId -> gameViewModel.useSkill("TRACK", npcId) } else null,
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

            // Layer 3: Trainer/Vendor overlays (bottom-left of room view)
            if (showTrainerButton || showVendorButton) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 8.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (showTrainerButton) {
                        RoomOverlayButton(
                            icon = "\u2B50",
                            label = "Train",
                            color = Color(0xFFFFD700),
                            onClick = { gameViewModel.interactTrainer() }
                        )
                    }
                    if (showVendorButton) {
                        RoomOverlayButton(
                            icon = "\uD83D\uDEE0\uFE0F",
                            label = "Shop",
                            color = Color(0xFFCC8833),
                            onClick = { gameViewModel.interactVendor() }
                        )
                    }
                }
            }
        }

        StoneDivider()

        // Middle: Game Log (~40%)
        ThemedFrame(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.40f),
            cornerAccent = false
        ) {
            GameLog(entries = gameLog)
        }

        StoneDivider()

        // Bottom: Controls (~25%)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.25f)
                .background(StoneTheme.panelBg)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalAlignment = Alignment.Top
            ) {
                DirectionPad(
                    availableExits = availableExits,
                    onMove = { direction -> gameViewModel.move(direction) },
                    onLook = { gameViewModel.look() },
                    lockedExits = (roomInfo?.room?.lockedExits?.keys ?: emptySet()),
                    trackedDirection = trackedDirection
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
                        isMeditating = gameViewModel.isMeditating.collectAsState().value,
                        spellSlots = spellSlots,
                        spellCatalog = spellCatalogState,
                        readiedSpellId = readiedSpellId,
                        classCatalog = classCatalog,
                        skillCatalog = gameViewModel.skillCatalog.collectAsState().value
                    )
                }
            }

            // Say bar + utility icons (full width, below D-pad)
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
                SpellUtilityButton(classCatalog, player?.characterClass) {
                    gameViewModel.openSpellPicker(0)
                }
                InventoryIconButton(
                    active = showInventory,
                    onClick = { gameViewModel.toggleInventory() }
                )
                EquipmentIconButton(
                    active = showEquipmentState,
                    onClick = { gameViewModel.toggleEquipment() }
                )
                SettingsGearButton(onClick = { gameViewModel.toggleSettings() })
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
    onSayTextChange: (String) -> Unit,
    roomPlayers: List<PlayerInfo> = emptyList(),
    onPlayerTap: ((PlayerInfo) -> Unit)? = null,
    onPlayerLongPress: ((PlayerInfo) -> Unit)? = null,
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
    val trackedDirection by gameViewModel.trackedDirection.collectAsState()

    // Determine if player has TRACK skill
    val hasTrackSkill = player?.characterClass?.let { classCatalog[it] }?.skills?.contains("TRACK") == true

    Column(modifier = Modifier.fillMaxSize().background(StoneTheme.panelBg)) {
        // Top row (~55%): Map area + Controls side-by-side
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.55f)
        ) {
            // Left side: Room background + sidebars + floating minimap
            ThemedFrame(
                modifier = Modifier
                    .weight(0.55f)
                    .fillMaxHeight(),
                cornerAccent = true
            ) {
                // Layer 1: Full-bleed background image
                val currentRoom = roomInfo?.room
                RoomBackground(
                    imageUrl = currentRoom?.backgroundImage ?: "",
                    roomName = currentRoom?.name ?: "",

                    modifier = Modifier.fillMaxSize()
                )

                // Layer 1.5: NPC & PC & item sprites overlaid on background
                SpriteOverlay(
                    npcs = roomEntities,
                    players = roomPlayers,
                    groundItems = roomGroundItems,
                    groundCoins = roomGroundCoins,
                    itemCatalog = itemCatalog,
                    selectedTargetId = selectedTargetId,
                    onSelectTarget = { gameViewModel.selectTarget(it) },
                    onPickupItem = { itemId, qty -> gameViewModel.pickupItem(itemId, qty) },
                    onPickupCoins = { coinType -> gameViewModel.pickupCoins(coinType) },
                    onPlayerTap = onPlayerTap,
                    onPlayerLongPress = onPlayerLongPress,
                    readiedSpellId = readiedSpellId,
                    onCastSpell = { spellId, targetId -> gameViewModel.castSpell(spellId, targetId) },
                    spellSlots = spellSlots,
                    spellCatalog = spellCatalogState,
                    classCatalog = classCatalog,
                    playerCharacterClass = player?.characterClass,
                    onAttackTarget = { gameViewModel.attackTarget(it) },
                    onTrackTarget = if (hasTrackSkill) { npcId -> gameViewModel.useSkill("TRACK", npcId) } else null,
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

                // Layer 3: Trainer/Vendor overlays (bottom-left of room view)
                if (showTrainerButton || showVendorButton) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 8.dp, bottom = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (showTrainerButton) {
                            RoomOverlayButton(
                                icon = "\u2B50",
                                label = "Train",
                                color = Color(0xFFFFD700),
                                onClick = { gameViewModel.interactTrainer() }
                            )
                        }
                        if (showVendorButton) {
                            RoomOverlayButton(
                                icon = "\uD83D\uDEE0\uFE0F",
                                label = "Shop",
                                color = Color(0xFFCC8833),
                                onClick = { gameViewModel.interactVendor() }
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(3.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                StoneTheme.frameLight,
                                StoneTheme.frameMid,
                                StoneTheme.innerShadow
                            )
                        )
                    )
            )

            // Right side: Status, D-pad, Action buttons
            Column(
                modifier = Modifier
                    .weight(0.45f)
                    .fillMaxHeight()
                    .background(StoneTheme.panelBg)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                // Player status panel (compact in landscape to save vertical space)
                val p = player
                if (p != null && p.maxHp > 0) {
                    PlayerStatusPanel(
                        player = p,
                        activeEffects = activeEffects,
                        isHidden = isHidden,
                        isMeditating = gameViewModel.isMeditating.collectAsState().value,
                        compact = true,
                        onClick = { gameViewModel.toggleCharacterSheet() },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }

                // D-pad and action buttons side by side
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    DirectionPad(
                        availableExits = availableExits,
                        onMove = { direction -> gameViewModel.move(direction) },
                        onLook = { gameViewModel.look() },
                        lockedExits = (roomInfo?.room?.lockedExits?.keys ?: emptySet()),
                        trackedDirection = trackedDirection
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
                            isMeditating = gameViewModel.isMeditating.collectAsState().value,
                            spellSlots = spellSlots,
                            spellCatalog = spellCatalogState,
                            readiedSpellId = readiedSpellId,
                            classCatalog = classCatalog,
                            playerCharacterClass = player?.characterClass,
                            currentMp = player?.currentMp ?: 0,
                            skillCatalog = gameViewModel.skillCatalog.collectAsState().value
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SpellUtilityButton(classCatalog, player?.characterClass) {
                                gameViewModel.openSpellPicker(0)
                            }
                            InventoryIconButton(
                                active = showInventory,
                                onClick = { gameViewModel.toggleInventory() }
                            )
                            EquipmentIconButton(
                                active = showEquipmentState,
                                onClick = { gameViewModel.toggleEquipment() }
                            )
                            SettingsGearButton(onClick = { gameViewModel.toggleSettings() })
                        }
                    }
                }
            }
        }

        StoneDivider()

        // Bottom (~45%): Game log + say bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.45f)
        ) {
            ThemedFrame(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                cornerAccent = false
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

/** Skill icon/color mapping for action buttons */
private data class SkillButtonInfo(
    val icon: String,
    val activeColor: Color
)

private val SKILL_BUTTON_MAP = mapOf(
    "BASH" to SkillButtonInfo("\uD83D\uDCA5", Color(0xFFFF8833)),        // 💥 Orange
    "KICK" to SkillButtonInfo("\uD83E\uDDB6", Color(0xFFFF5533)),        // 🦶 Red-orange
    "SNEAK" to SkillButtonInfo("\uD83E\uDDE5", Color(0xFF888888)),       // 🧥 Gray
    "MEDITATE" to SkillButtonInfo("\uD83E\uDDD8", Color(0xFF7755CC)),    // 🧘 Blue-purple
    "TRACK" to SkillButtonInfo("\uD83D\uDC3E", Color(0xFF55AA55)),       // 🐾 Green
    "PICK_LOCK" to SkillButtonInfo("\uD83D\uDD13", Color(0xFFCCAA33))    // 🔓 Gold
)

@Composable
private fun ActionButton(
    icon: String,
    color: Color,
    isActive: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    StoneButton(
        icon = icon,
        color = color,
        isActive = isActive,
        enabled = enabled,
        onClick = onClick
    )
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
    isMeditating: Boolean = false,
    spellSlots: List<String?> = listOf(null, null, null, null),
    spellCatalog: Map<String, com.neomud.shared.model.SpellDef> = emptyMap(),
    readiedSpellId: String? = null,
    classCatalog: Map<String, com.neomud.shared.model.CharacterClassDef> = emptyMap(),
    playerCharacterClass: String? = player?.characterClass,
    currentMp: Int = player?.currentMp ?: 0,
    skillCatalog: Map<String, com.neomud.shared.model.SkillDef> = emptyMap()
) {
    val classDef = playerCharacterClass?.let { classCatalog[it] }
    val hasMagic = classDef?.magicSchools?.isNotEmpty() == true

    Column {
    // Row 1: Action skill buttons (class-driven)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Attack button — shows as Backstab (dagger) when hidden + class has BACKSTAB
        val hasBackstab = classDef?.skills?.contains("BACKSTAB") == true
        val showBackstab = isHidden && hasBackstab
        val attackColor = when {
            showBackstab -> Color(0xFFCC3333)  // Dark red for backstab
            attackMode -> Color(0xFFFF3333)
            hasHostiles -> MaterialTheme.colorScheme.primary
            else -> Color.Gray
        }
        ActionButton(
            icon = if (showBackstab) "\uD83D\uDDE1\uFE0F" else "\u2694\uFE0F",
            color = attackColor,
            isActive = attackMode,
            enabled = hasHostiles || attackMode || showBackstab,
            onClick = { gameViewModel.toggleAttackMode(!attackMode) }
        )

        // Class skill buttons (filtered: non-passive)
        val classSkills = classDef?.skills ?: emptyList()
        for (skillId in classSkills) {
            val skillDef = skillCatalog[skillId]
            if (skillDef?.isPassive == true) continue

            val btnInfo = SKILL_BUTTON_MAP[skillId] ?: continue

            // SNEAK is a special toggle
            if (skillId == "SNEAK") {
                val sneakEnabled = !attackMode || isHidden
                ActionButton(
                    icon = btnInfo.icon,
                    color = if (isHidden) MudColors.stealth else btnInfo.activeColor,
                    isActive = isHidden,
                    enabled = sneakEnabled,
                    onClick = { gameViewModel.toggleSneakMode(!isHidden) }
                )
            } else if (skillId == "MEDITATE") {
                ActionButton(
                    icon = btnInfo.icon,
                    color = if (isMeditating) MudColors.spell else btnInfo.activeColor,
                    isActive = isMeditating,
                    enabled = !attackMode || isMeditating,
                    onClick = { gameViewModel.useSkill("MEDITATE") }
                )
            } else if (skillId == "PICK_LOCK") {
                ActionButton(
                    icon = btnInfo.icon,
                    color = btnInfo.activeColor,
                    onClick = { gameViewModel.showLockTargetPicker() }
                )
            } else {
                ActionButton(
                    icon = btnInfo.icon,
                    color = btnInfo.activeColor,
                    onClick = { gameViewModel.useSkill(skillId) }
                )
            }
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Player status panel (only in portrait — landscape puts it above)
        if (player != null && player.maxHp > 0) {
            PlayerStatusPanel(
                player = player,
                activeEffects = activeEffects,
                isHidden = isHidden,
                isMeditating = isMeditating,
                onClick = { gameViewModel.toggleCharacterSheet() },
                modifier = Modifier.weight(1f)
            )
        }
    }

    // Row 2: Spell bar (magic classes only)
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
private fun RoomOverlayButton(
    icon: String,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xCC1a1a2e.toInt()),
        border = BorderStroke(1.dp, color.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = icon, fontSize = 14.sp, color = color)
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun SpellUtilityButton(
    classCatalog: Map<String, com.neomud.shared.model.CharacterClassDef>,
    playerCharacterClass: String?,
    onClick: () -> Unit
) {
    val classDef = playerCharacterClass?.let { classCatalog[it] }
    val hasMagic = classDef?.magicSchools?.isNotEmpty() == true
    if (!hasMagic) return

    val primarySchool = classDef?.magicSchools?.keys?.firstOrNull() ?: ""
    val spellColor = when (primarySchool) {
        "mage" -> Color(0xFF5599FF)
        "priest" -> Color(0xFFFFDD44)
        "druid" -> Color(0xFF55CC55)
        "kai" -> Color(0xFFFF7744)
        "bard" -> Color(0xFFCC77FF)
        else -> Color(0xFF9B59FF)
    }
    val stoneBg = Brush.verticalGradient(listOf(StoneTheme.frameLight, StoneTheme.frameDark))
    Box(
        modifier = Modifier
            .size(28.dp)
            .background(stoneBg, RoundedCornerShape(4.dp))
            .border(1.dp, StoneTheme.frameMid, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "\u2728",
            fontSize = 16.sp,
            color = spellColor
        )
    }
}

@Composable
private fun InventoryIconButton(active: Boolean, onClick: () -> Unit) {
    val stoneBg = Brush.verticalGradient(listOf(StoneTheme.frameLight, StoneTheme.frameDark))
    Box(
        modifier = Modifier
            .size(28.dp)
            .background(stoneBg, RoundedCornerShape(4.dp))
            .border(1.dp, StoneTheme.frameMid, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "\uD83C\uDF92",
            fontSize = 16.sp,
            color = if (active) Color(0xFFFFD700) else Color(0xFFA8A8A8)
        )
    }
}

@Composable
private fun EquipmentIconButton(active: Boolean, onClick: () -> Unit) {
    val stoneBg = Brush.verticalGradient(listOf(StoneTheme.frameLight, StoneTheme.frameDark))
    Box(
        modifier = Modifier
            .size(28.dp)
            .background(stoneBg, RoundedCornerShape(4.dp))
            .border(1.dp, StoneTheme.frameMid, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "\uD83D\uDEE1\uFE0F",
            fontSize = 16.sp,
            color = if (active) Color(0xFFFFD700) else Color(0xFFA8A8A8)
        )
    }
}

@Composable
private fun SettingsGearButton(onClick: () -> Unit) {
    val stoneBg = Brush.verticalGradient(listOf(StoneTheme.frameLight, StoneTheme.frameDark))
    Box(
        modifier = Modifier
            .size(28.dp)
            .background(stoneBg, RoundedCornerShape(4.dp))
            .border(1.dp, StoneTheme.frameMid, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
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
                color = if (isAdmin && tfv.text.startsWith("/")) Color(0xFF1565C0) else Color(0xFFCCCCCC)
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .weight(1f)
                .height(32.dp)
                .background(StoneTheme.textPanelBg, RoundedCornerShape(6.dp))
                .drawBehind {
                    // Beveled stone border
                    val w = size.width; val h = size.height
                    drawLine(StoneTheme.frameLight, Offset(0f, 0f), Offset(w, 0f), strokeWidth = 1f)
                    drawLine(StoneTheme.frameLight, Offset(0f, 0f), Offset(0f, h), strokeWidth = 1f)
                    drawLine(StoneTheme.innerShadow, Offset(0f, h - 1f), Offset(w, h - 1f), strokeWidth = 1f)
                    drawLine(StoneTheme.innerShadow, Offset(w - 1f, 0f), Offset(w - 1f, h), strokeWidth = 1f)
                }
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
        val sayEnabled = tfv.text.isNotBlank()
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .height(32.dp)
                .drawBehind {
                    val bgBrush = Brush.verticalGradient(listOf(StoneTheme.frameLight, StoneTheme.frameDark))
                    drawRect(bgBrush)
                    val w = size.width; val h = size.height
                    drawLine(StoneTheme.frameLight, Offset(0f, 0f), Offset(w, 0f), strokeWidth = 1f)
                    drawLine(StoneTheme.frameLight, Offset(0f, 0f), Offset(0f, h), strokeWidth = 1f)
                    drawLine(StoneTheme.innerShadow, Offset(0f, h - 1f), Offset(w, h - 1f), strokeWidth = 1f)
                    drawLine(StoneTheme.innerShadow, Offset(w - 1f, 0f), Offset(w - 1f, h), strokeWidth = 1f)
                }
                .clickable(enabled = sayEnabled) {
                    val text = tfv.text
                    if (text.isNotBlank()) {
                        onSay(text)
                        onSayTextChange("")
                    }
                }
                .padding(horizontal = 12.dp)
        ) {
            Text(
                "Say",
                fontSize = 12.sp,
                color = if (sayEnabled) Color(0xFFE0E0E0) else Color(0xFF666666)
            )
        }
    }
}

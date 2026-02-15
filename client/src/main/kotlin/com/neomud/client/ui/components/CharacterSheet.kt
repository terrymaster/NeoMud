package com.neomud.client.ui.components

import android.content.res.Configuration
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neomud.shared.model.*

private val CopperColor = Color(0xFFCD7F32)
private val SilverColor = Color(0xFFC0C0C0)
private val GoldColor = Color(0xFFFFD700)
private val PlatinumColor = Color(0xFFE5E4E2)
private val CyanAccent = Color(0xFF55FFFF)
private val YellowAccent = Color(0xFFFFFF55)
private val DimText = Color(0xFFAAAAAA)
private val BrightText = Color(0xFFCCCCCC)

@Composable
fun CharacterSheet(
    player: Player,
    classCatalog: Map<String, CharacterClassDef>,
    equipment: Map<String, String>,
    itemCatalog: Map<String, Item>,
    activeEffects: List<ActiveEffect>,
    playerCoins: Coins,
    skillCatalog: Map<String, SkillDef> = emptyMap(),
    isHidden: Boolean = false,
    onClose: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

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
                .border(1.dp, CyanAccent, RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            // Header row with close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Character Sheet",
                    color = CyanAccent,
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

            if (isLandscape) {
                CharacterSheetLandscape(
                    player = player,
                    classCatalog = classCatalog,
                    equipment = equipment,
                    itemCatalog = itemCatalog,
                    activeEffects = activeEffects,
                    playerCoins = playerCoins,
                    skillCatalog = skillCatalog,
                    isHidden = isHidden
                )
            } else {
                CharacterSheetPortrait(
                    player = player,
                    classCatalog = classCatalog,
                    equipment = equipment,
                    itemCatalog = itemCatalog,
                    activeEffects = activeEffects,
                    playerCoins = playerCoins,
                    skillCatalog = skillCatalog,
                    isHidden = isHidden
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.CharacterSheetPortrait(
    player: Player,
    classCatalog: Map<String, CharacterClassDef>,
    equipment: Map<String, String>,
    itemCatalog: Map<String, Item>,
    activeEffects: List<ActiveEffect>,
    playerCoins: Coins,
    skillCatalog: Map<String, SkillDef> = emptyMap(),
    isHidden: Boolean = false
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState())
    ) {
        NameAndVitals(player, classCatalog)
        Spacer(modifier = Modifier.height(12.dp))
        StatsSection(player)
        Spacer(modifier = Modifier.height(12.dp))
        EquipmentSection(equipment, itemCatalog)
        Spacer(modifier = Modifier.height(12.dp))
        SkillsSection(player, classCatalog, skillCatalog)
        Spacer(modifier = Modifier.height(12.dp))
        ActiveEffectsSection(activeEffects, isHidden)
        Spacer(modifier = Modifier.height(12.dp))
        CoinsSection(playerCoins)
    }
}

@Composable
private fun ColumnScope.CharacterSheetLandscape(
    player: Player,
    classCatalog: Map<String, CharacterClassDef>,
    equipment: Map<String, String>,
    itemCatalog: Map<String, Item>,
    activeEffects: List<ActiveEffect>,
    playerCoins: Coins,
    skillCatalog: Map<String, SkillDef> = emptyMap(),
    isHidden: Boolean = false
) {
    Row(
        modifier = Modifier
            .weight(1f),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Left column: Name, Vitals, Stats, Skills
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            NameAndVitals(player, classCatalog)
            Spacer(modifier = Modifier.height(12.dp))
            StatsSection(player)
            Spacer(modifier = Modifier.height(12.dp))
            SkillsSection(player, classCatalog, skillCatalog)
        }

        VerticalDivider(color = Color(0xFF555555), thickness = 1.dp)

        // Right column: Equipment, Effects, Coins
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            EquipmentSection(equipment, itemCatalog)
            Spacer(modifier = Modifier.height(12.dp))
            ActiveEffectsSection(activeEffects, isHidden)
            Spacer(modifier = Modifier.height(12.dp))
            CoinsSection(playerCoins)
        }
    }
}

@Composable
private fun NameAndVitals(player: Player, classCatalog: Map<String, CharacterClassDef>) {
    val className = classCatalog[player.characterClass]?.name ?: player.characterClass
    Text(
        text = player.name,
        color = Color.White,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold
    )
    val raceLabel = if (player.race.isNotEmpty()) "${player.race}  \u2022  " else ""
    Text(
        text = "$raceLabel$className  \u2022  Level ${player.level}",
        color = DimText,
        fontSize = 13.sp
    )

    Spacer(modifier = Modifier.height(12.dp))

    SectionHeader("Vitals")
    Spacer(modifier = Modifier.height(4.dp))
    VitalBar(
        label = "HP",
        current = player.currentHp,
        max = player.maxHp,
        color = when {
            player.maxHp > 0 && player.currentHp.toFloat() / player.maxHp > 0.5f -> Color(0xFF4CAF50)
            player.maxHp > 0 && player.currentHp.toFloat() / player.maxHp > 0.25f -> Color(0xFFFF9800)
            else -> Color(0xFFF44336)
        }
    )
    Spacer(modifier = Modifier.height(4.dp))
    VitalBar(
        label = "MP",
        current = player.currentMp,
        max = player.maxMp,
        color = Color(0xFF448AFF)
    )

    if (player.xpToNextLevel > 0) {
        Spacer(modifier = Modifier.height(4.dp))
        VitalBar(
            label = "XP",
            current = player.currentXp.toInt(),
            max = player.xpToNextLevel.toInt(),
            color = Color(0xFF55FFFF)
        )
    }
    if (player.unspentCp > 0) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Unspent CP: ${player.unspentCp}",
            fontSize = 12.sp,
            color = Color(0xFFFFFF55)
        )
    }
}

@Composable
private fun StatsSection(player: Player) {
    SectionHeader("Stats")
    Spacer(modifier = Modifier.height(4.dp))
    StatsGrid(player.stats)
}

@Composable
private fun EquipmentSection(equipment: Map<String, String>, itemCatalog: Map<String, Item>) {
    SectionHeader("Equipment")
    Spacer(modifier = Modifier.height(4.dp))
    for (slot in EquipmentSlots.DEFAULT_SLOTS) {
        val equippedItemId = equipment[slot]
        val item = equippedItemId?.let { itemCatalog[it] }
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = slot.replaceFirstChar { it.uppercase() },
                color = DimText,
                fontSize = 12.sp,
                modifier = Modifier.width(64.dp)
            )
            Text(
                text = item?.name ?: "-- empty --",
                color = if (item != null) Color(0xFF55FF55) else Color(0xFF555555),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun SkillsSection(
    player: Player,
    classCatalog: Map<String, CharacterClassDef>,
    skillCatalog: Map<String, SkillDef>
) {
    SectionHeader("Skills")
    Spacer(modifier = Modifier.height(4.dp))
    val classDef = classCatalog[player.characterClass]
    val classSkillIds = classDef?.skills ?: emptyList()
    if (classSkillIds.isEmpty() || skillCatalog.isEmpty()) {
        Text("No skills", color = Color(0xFF555555), fontSize = 12.sp)
    } else {
        for (skillId in classSkillIds) {
            val skill = skillCatalog[skillId] ?: continue
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = skill.name,
                    color = BrightText,
                    fontSize = 12.sp,
                    modifier = Modifier.width(80.dp)
                )
                Text(
                    text = skill.description,
                    color = DimText,
                    fontSize = 11.sp,
                    modifier = Modifier.weight(1f),
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
private fun ActiveEffectsSection(activeEffects: List<ActiveEffect>, isHidden: Boolean = false) {
    SectionHeader("Active Effects")
    Spacer(modifier = Modifier.height(4.dp))
    if (activeEffects.isEmpty() && !isHidden) {
        Text("No active effects", color = Color(0xFF555555), fontSize = 12.sp)
    } else {
        if (isHidden) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Hidden",
                    color = Color(0xFF888888),
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "stealth",
                    color = Color(0xFF666666),
                    fontSize = 11.sp
                )
            }
        }
        for (effect in activeEffects) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = effect.name,
                    color = BrightText,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${effect.remainingTicks} ticks",
                    color = DimText,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun CoinsSection(playerCoins: Coins) {
    SectionHeader("Coins")
    Spacer(modifier = Modifier.height(4.dp))
    if (playerCoins.isEmpty()) {
        Text("No coins", color = Color(0xFF555555), fontSize = 12.sp)
    } else {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (playerCoins.platinum > 0) CoinBadge("${playerCoins.platinum} PP", PlatinumColor)
            if (playerCoins.gold > 0) CoinBadge("${playerCoins.gold} GP", GoldColor)
            if (playerCoins.silver > 0) CoinBadge("${playerCoins.silver} SP", SilverColor)
            if (playerCoins.copper > 0) CoinBadge("${playerCoins.copper} CP", CopperColor)
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        color = YellowAccent,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun VitalBar(label: String, current: Int, max: Int, color: Color) {
    val fraction = if (max > 0) (current.toFloat() / max).coerceIn(0f, 1f) else 0f
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "$label: $current/$max",
            fontSize = 12.sp,
            color = BrightText,
            modifier = Modifier.width(100.dp)
        )
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier.weight(1f).height(10.dp),
            color = color,
            trackColor = Color(0xFF333333),
        )
    }
}

@Composable
private fun StatsGrid(stats: Stats) {
    val statEntries = listOf(
        "STR" to stats.strength,
        "AGI" to stats.agility,
        "INT" to stats.intellect,
        "WIL" to stats.willpower,
        "HLT" to stats.health,
        "CHM" to stats.charm
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        for ((name, value) in statEntries) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = name, color = DimText, fontSize = 11.sp)
                Text(text = value.toString(), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
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

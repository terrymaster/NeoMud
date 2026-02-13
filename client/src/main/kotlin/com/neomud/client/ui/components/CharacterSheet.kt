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
    onClose: () -> Unit
) {
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

            // Scrollable content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                // Name, class, level
                val className = classCatalog[player.characterClass]?.name ?: player.characterClass
                Text(
                    text = player.name,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$className  \u2022  Level ${player.level}",
                    color = DimText,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Vitals
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

                Spacer(modifier = Modifier.height(12.dp))

                // Stats
                SectionHeader("Stats")
                Spacer(modifier = Modifier.height(4.dp))
                StatsGrid(player.stats)

                Spacer(modifier = Modifier.height(12.dp))

                // Equipment
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

                Spacer(modifier = Modifier.height(12.dp))

                // Active Effects
                SectionHeader("Active Effects")
                Spacer(modifier = Modifier.height(4.dp))
                if (activeEffects.isEmpty()) {
                    Text("No active effects", color = Color(0xFF555555), fontSize = 12.sp)
                } else {
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

                Spacer(modifier = Modifier.height(12.dp))

                // Coins
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
        "DEX" to stats.dexterity,
        "CON" to stats.constitution,
        "INT" to stats.intelligence,
        "WIS" to stats.wisdom
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

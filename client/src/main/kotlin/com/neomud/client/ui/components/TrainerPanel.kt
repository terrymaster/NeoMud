package com.neomud.client.ui.components

import androidx.compose.foundation.background
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
import com.neomud.client.ui.theme.MudColors
import com.neomud.shared.protocol.ServerMessage

@Composable
fun TrainerPanel(
    trainerInfo: ServerMessage.TrainerInfo,
    onLevelUp: () -> Unit,
    onTrainStat: (String, Int) -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { /* consume clicks */ },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Guildmaster",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD700)
                    )
                    TextButton(onClick = onClose) {
                        Text("Close", color = Color(0xFFAAAAAA))
                    }
                }

                HorizontalDivider(color = Color(0xFF333355), modifier = Modifier.padding(vertical = 8.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Level Up section
                    Text(
                        text = "Level Up",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFCCCCCC)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (trainerInfo.canLevelUp) {
                        Button(
                            onClick = onLevelUp,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Level Up!", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Text(
                            text = "You are not ready to level up yet.",
                            fontSize = 13.sp,
                            color = Color(0xFF888888)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color(0xFF333355))
                    Spacer(modifier = Modifier.height(16.dp))

                    // CP display
                    Text(
                        text = "Character Points: ${trainerInfo.unspentCp}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MudColors.xp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Stat training grid
                    Text(
                        text = "Train Stats",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFCCCCCC)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Cost: 1 CP (0-9 above base), 2 CP (10-19), 3 CP (20+)",
                        fontSize = 11.sp,
                        color = Color(0xFF777777)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val stats = listOf(
                        "Strength" to (trainerInfo.currentStats.strength to trainerInfo.baseStats.strength),
                        "Agility" to (trainerInfo.currentStats.agility to trainerInfo.baseStats.agility),
                        "Intellect" to (trainerInfo.currentStats.intellect to trainerInfo.baseStats.intellect),
                        "Willpower" to (trainerInfo.currentStats.willpower to trainerInfo.baseStats.willpower),
                        "Health" to (trainerInfo.currentStats.health to trainerInfo.baseStats.health),
                        "Charm" to (trainerInfo.currentStats.charm to trainerInfo.baseStats.charm)
                    )

                    for ((statName, values) in stats) {
                        val (current, base) = values
                        val above = current - base
                        val cost = when {
                            above < 10 -> 1
                            above < 20 -> 2
                            else -> 3
                        }
                        val canTrain = trainerInfo.unspentCp >= cost

                        StatTrainRow(
                            statName = statName,
                            currentValue = current,
                            baseValue = base,
                            cost = cost,
                            canTrain = canTrain,
                            onTrain = { onTrainStat(statName.lowercase(), 1) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatTrainRow(
    statName: String,
    currentValue: Int,
    baseValue: Int,
    cost: Int,
    canTrain: Boolean,
    onTrain: () -> Unit
) {
    val above = currentValue - baseValue
    val statColor = when {
        above >= 20 -> Color(0xFFFFD700) // Gold for high training
        above >= 10 -> Color(0xFF66BB6A) // Green for moderate
        above > 0 -> Color(0xFF42A5F5)   // Blue for some training
        else -> Color(0xFFCCCCCC)         // Default
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Stat name
        Text(
            text = statName,
            fontSize = 14.sp,
            color = Color(0xFFAAAAAA),
            modifier = Modifier.width(80.dp)
        )

        // Current / base
        Text(
            text = "$currentValue",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = statColor,
            modifier = Modifier.width(35.dp)
        )
        Text(
            text = "(base $baseValue)",
            fontSize = 11.sp,
            color = Color(0xFF666666),
            modifier = Modifier.width(65.dp)
        )

        // Cost indicator
        Text(
            text = "${cost} CP",
            fontSize = 12.sp,
            color = if (canTrain) MudColors.xp else Color(0xFF555555),
            modifier = Modifier.width(35.dp)
        )

        // Train button
        Button(
            onClick = onTrain,
            enabled = canTrain,
            modifier = Modifier.height(32.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1565C0),
                disabledContainerColor = Color(0xFF333333)
            )
        ) {
            Text("+1", fontSize = 12.sp)
        }
    }
}

package com.neomud.client.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neomud.client.ui.components.DirectionPad
import com.neomud.client.ui.components.EntitySidebar
import com.neomud.client.ui.components.GameLog
import com.neomud.client.ui.components.MiniMap
import com.neomud.client.viewmodel.GameViewModel
import com.neomud.shared.model.Direction

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

    var sayText by remember { mutableStateOf("") }

    val availableExits = roomInfo?.room?.exits?.keys ?: emptySet()
    val hasHostiles = roomEntities.any { it.hostile }

    Row(modifier = Modifier.fillMaxSize()) {
        // Main content
        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            // Top: Mini Map (~35%)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.35f)
            ) {
                val data = mapData
                if (data != null) {
                    MiniMap(
                        rooms = data.rooms,
                        playerRoomId = data.playerRoomId
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
                    // Action row: attack button + player HP
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

                        Spacer(modifier = Modifier.width(8.dp))

                        // Player HP bar
                        val p = player
                        if (p != null && p.maxHp > 0) {
                            val hpFraction = (p.currentHp.toFloat() / p.maxHp).coerceIn(0f, 1f)
                            val hpColor = when {
                                hpFraction > 0.5f -> Color(0xFF4CAF50)
                                hpFraction > 0.25f -> Color(0xFFFF9800)
                                else -> Color(0xFFF44336)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "HP: ${p.currentHp}/${p.maxHp}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                LinearProgressIndicator(
                                    progress = { hpFraction },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp),
                                    color = hpColor,
                                    trackColor = Color(0xFF333333),
                                )
                            }
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

        // Entity sidebar on the right
        if (roomEntities.isNotEmpty()) {
            EntitySidebar(
                entities = roomEntities,
                selectedTargetId = selectedTargetId,
                onSelectTarget = { gameViewModel.selectTarget(it) }
            )
        }
    }
}

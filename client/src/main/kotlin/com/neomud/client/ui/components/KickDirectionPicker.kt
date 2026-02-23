package com.neomud.client.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neomud.shared.model.Direction

private val EnabledColor = Color(0xFFFF5533)
private val DisabledColor = Color(0xFF444444)
private val ButtonSize = 44.dp
private val SmallButtonSize = 38.dp
private val Gap = 4.dp

@Composable
fun KickDirectionPicker(
    availableExits: Set<Direction>,
    lockedExits: Set<Direction>,
    npcName: String,
    onSelect: (Direction) -> Unit,
    onDismiss: () -> Unit
) {
    // Filter: never UP, only exits that exist in the room
    val validDirections = availableExits - Direction.UP
    val allDirections = Direction.entries.toSet() - Direction.UP

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x88000000))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {} // consume clicks on the card
                ),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Kick $npcName",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = EnabledColor
                )
                Text(
                    text = "Choose direction",
                    fontSize = 12.sp,
                    color = Color(0xFF888888)
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Row 1: NW, N, NE
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Gap),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    KickDirButton("\u2196", Direction.NORTHWEST, validDirections, lockedExits, onSelect, SmallButtonSize)
                    KickDirButton("\u25B2", Direction.NORTH, validDirections, lockedExits, onSelect, ButtonSize)
                    KickDirButton("\u2197", Direction.NORTHEAST, validDirections, lockedExits, onSelect, SmallButtonSize)
                }

                Spacer(modifier = Modifier.height(Gap))

                // Row 2: W, label, E
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Gap),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    KickDirButton("\u25C0", Direction.WEST, validDirections, lockedExits, onSelect, ButtonSize)
                    Box(
                        modifier = Modifier.size(ButtonSize),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "\uD83E\uDDB6",
                            fontSize = 20.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                    KickDirButton("\u25B6", Direction.EAST, validDirections, lockedExits, onSelect, ButtonSize)
                }

                Spacer(modifier = Modifier.height(Gap))

                // Row 3: SW, S, SE
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Gap),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    KickDirButton("\u2199", Direction.SOUTHWEST, validDirections, lockedExits, onSelect, SmallButtonSize)
                    KickDirButton("\u25BC", Direction.SOUTH, validDirections, lockedExits, onSelect, ButtonSize)
                    KickDirButton("\u2198", Direction.SOUTHEAST, validDirections, lockedExits, onSelect, SmallButtonSize)
                }

                // Row 4: DOWN
                if (Direction.DOWN in availableExits) {
                    Spacer(modifier = Modifier.height(Gap))
                    val isLocked = Direction.DOWN in lockedExits
                    val enabled = !isLocked
                    FilledTonalButton(
                        onClick = { if (enabled) onSelect(Direction.DOWN) },
                        enabled = enabled,
                        modifier = Modifier.height(36.dp),
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (enabled) EnabledColor.copy(alpha = 0.2f) else DisabledColor,
                            contentColor = if (enabled) EnabledColor else Color(0xFF666666)
                        )
                    ) {
                        Text(
                            text = "\u2B07 Down",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KickDirButton(
    icon: String,
    direction: Direction,
    validDirections: Set<Direction>,
    lockedExits: Set<Direction>,
    onSelect: (Direction) -> Unit,
    size: androidx.compose.ui.unit.Dp
) {
    val exists = direction in validDirections
    val isLocked = direction in lockedExits
    val enabled = exists && !isLocked

    FilledTonalButton(
        onClick = { if (enabled) onSelect(direction) },
        enabled = enabled,
        modifier = Modifier.size(size),
        shape = RoundedCornerShape(6.dp),
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = if (enabled) EnabledColor.copy(alpha = 0.2f) else DisabledColor,
            contentColor = if (enabled) EnabledColor else Color(0xFF666666),
            disabledContainerColor = DisabledColor,
            disabledContentColor = Color(0xFF666666)
        )
    ) {
        Text(
            text = if (isLocked) "\uD83D\uDD12" else icon,
            fontSize = if (size == SmallButtonSize) 14.sp else 16.sp,
            textAlign = TextAlign.Center
        )
    }
}

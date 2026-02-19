package com.neomud.client.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neomud.shared.model.Direction

private val LockedContainer = Color(0xFFFFCC80) // amber/orange
private val LockedContent = Color(0xFF6D4C00)   // dark amber

private val BUTTON_SIZE = 40.dp
private val SMALL_BUTTON_SIZE = 34.dp
private val H_GAP = 6.dp
private val V_GAP = 4.dp

@Composable
fun DirectionPad(
    availableExits: Set<Direction>,
    onMove: (Direction) -> Unit,
    onLook: () -> Unit,
    modifier: Modifier = Modifier,
    lockedExits: Set<Direction> = emptySet()
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(V_GAP)
    ) {
        // Row 1: NW, N, NE
        Row(
            horizontalArrangement = Arrangement.spacedBy(H_GAP),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DPadButton(
                text = "\u2196",
                enabled = Direction.NORTHWEST in availableExits,
                locked = Direction.NORTHWEST in lockedExits,
                onClick = { onMove(Direction.NORTHWEST) },
                size = SMALL_BUTTON_SIZE
            )
            DPadButton(
                text = "\u25B2",
                enabled = Direction.NORTH in availableExits,
                locked = Direction.NORTH in lockedExits,
                onClick = { onMove(Direction.NORTH) }
            )
            DPadButton(
                text = "\u2197",
                enabled = Direction.NORTHEAST in availableExits,
                locked = Direction.NORTHEAST in lockedExits,
                onClick = { onMove(Direction.NORTHEAST) },
                size = SMALL_BUTTON_SIZE
            )
        }

        // Row 2: W, LOOK, E
        Row(
            horizontalArrangement = Arrangement.spacedBy(H_GAP),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DPadButton(
                text = "\u25C0",
                enabled = Direction.WEST in availableExits,
                locked = Direction.WEST in lockedExits,
                onClick = { onMove(Direction.WEST) }
            )
            DPadButton(
                text = "\uD83D\uDC41",
                enabled = true,
                onClick = onLook,
                isLook = true
            )
            DPadButton(
                text = "\u25B6",
                enabled = Direction.EAST in availableExits,
                locked = Direction.EAST in lockedExits,
                onClick = { onMove(Direction.EAST) }
            )
        }

        // Row 3: SW, S, SE
        Row(
            horizontalArrangement = Arrangement.spacedBy(H_GAP),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DPadButton(
                text = "\u2199",
                enabled = Direction.SOUTHWEST in availableExits,
                locked = Direction.SOUTHWEST in lockedExits,
                onClick = { onMove(Direction.SOUTHWEST) },
                size = SMALL_BUTTON_SIZE
            )
            DPadButton(
                text = "\u25BC",
                enabled = Direction.SOUTH in availableExits,
                locked = Direction.SOUTH in lockedExits,
                onClick = { onMove(Direction.SOUTH) }
            )
            DPadButton(
                text = "\u2198",
                enabled = Direction.SOUTHEAST in availableExits,
                locked = Direction.SOUTHEAST in lockedExits,
                onClick = { onMove(Direction.SOUTHEAST) },
                size = SMALL_BUTTON_SIZE
            )
        }

        // Row 4: UP (under SW) and DOWN (under SE)
        Row(
            horizontalArrangement = Arrangement.spacedBy(H_GAP),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StairButton(
                label = "\u2B06",
                enabled = Direction.UP in availableExits,
                locked = Direction.UP in lockedExits,
                onClick = { onMove(Direction.UP) }
            )
            Spacer(modifier = Modifier.width(BUTTON_SIZE))
            StairButton(
                label = "\u2B07",
                enabled = Direction.DOWN in availableExits,
                locked = Direction.DOWN in lockedExits,
                onClick = { onMove(Direction.DOWN) }
            )
        }
    }
}

@Composable
private fun DPadButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    isLook: Boolean = false,
    locked: Boolean = false,
    size: Dp = BUTTON_SIZE
) {
    FilledTonalButton(
        onClick = if (enabled) onClick else ({}),
        enabled = true,
        modifier = Modifier.size(size),
        shape = CircleShape,
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = when {
                isLook -> MaterialTheme.colorScheme.secondaryContainer
                locked -> LockedContainer
                enabled -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            },
            contentColor = when {
                isLook -> MaterialTheme.colorScheme.onSecondaryContainer
                locked -> LockedContent
                enabled -> MaterialTheme.colorScheme.onPrimaryContainer
                else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
            }
        )
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun StairButton(
    label: String,
    enabled: Boolean,
    locked: Boolean = false,
    onClick: () -> Unit
) {
    FilledTonalButton(
        onClick = if (enabled) onClick else ({}),
        enabled = true,
        modifier = Modifier.size(SMALL_BUTTON_SIZE),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = when {
                locked -> LockedContainer
                enabled -> MaterialTheme.colorScheme.tertiaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            },
            contentColor = when {
                locked -> LockedContent
                enabled -> MaterialTheme.colorScheme.onTertiaryContainer
                else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
            }
        )
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
    }
}

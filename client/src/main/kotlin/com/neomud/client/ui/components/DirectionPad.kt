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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neomud.shared.model.Direction

private val BUTTON_SIZE = 40.dp
private val SMALL_BUTTON_SIZE = 34.dp
private val H_GAP = 6.dp
private val V_GAP = 4.dp

@Composable
fun DirectionPad(
    availableExits: Set<Direction>,
    onMove: (Direction) -> Unit,
    onLook: () -> Unit,
    modifier: Modifier = Modifier
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
                onClick = { onMove(Direction.NORTHWEST) },
                size = SMALL_BUTTON_SIZE
            )
            DPadButton(
                text = "\u25B2",
                enabled = Direction.NORTH in availableExits,
                onClick = { onMove(Direction.NORTH) }
            )
            DPadButton(
                text = "\u2197",
                enabled = Direction.NORTHEAST in availableExits,
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
                onClick = { onMove(Direction.WEST) }
            )
            DPadButton(
                text = "\u25CE",
                enabled = true,
                onClick = onLook,
                isLook = true
            )
            DPadButton(
                text = "\u25B6",
                enabled = Direction.EAST in availableExits,
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
                onClick = { onMove(Direction.SOUTHWEST) },
                size = SMALL_BUTTON_SIZE
            )
            DPadButton(
                text = "\u25BC",
                enabled = Direction.SOUTH in availableExits,
                onClick = { onMove(Direction.SOUTH) }
            )
            DPadButton(
                text = "\u2198",
                enabled = Direction.SOUTHEAST in availableExits,
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
                onClick = { onMove(Direction.UP) }
            )
            Spacer(modifier = Modifier.width(BUTTON_SIZE))
            StairButton(
                label = "\u2B07",
                enabled = Direction.DOWN in availableExits,
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
                enabled -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            },
            contentColor = when {
                isLook -> MaterialTheme.colorScheme.onSecondaryContainer
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
    onClick: () -> Unit
) {
    FilledTonalButton(
        onClick = if (enabled) onClick else ({}),
        enabled = true,
        modifier = Modifier.size(SMALL_BUTTON_SIZE),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = if (enabled) MaterialTheme.colorScheme.tertiaryContainer
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            contentColor = if (enabled) MaterialTheme.colorScheme.onTertiaryContainer
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
        )
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
    }
}

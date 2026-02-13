package com.neomud.client.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neomud.shared.model.Direction

@Composable
fun DirectionPad(
    availableExits: Set<Direction>,
    onMove: (Direction) -> Unit,
    onLook: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // North
        DPadButton(
            text = "\u25B2",
            enabled = Direction.NORTH in availableExits,
            onClick = { onMove(Direction.NORTH) }
        )

        // West - Look - East
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DPadButton(
                text = "\u25C0",
                enabled = Direction.WEST in availableExits,
                onClick = { onMove(Direction.WEST) }
            )

            Spacer(modifier = Modifier.width(2.dp))

            DPadButton(
                text = "\u25CE",
                enabled = true,
                onClick = onLook,
                isLook = true
            )

            Spacer(modifier = Modifier.width(2.dp))

            DPadButton(
                text = "\u25B6",
                enabled = Direction.EAST in availableExits,
                onClick = { onMove(Direction.EAST) }
            )
        }

        // South
        DPadButton(
            text = "\u25BC",
            enabled = Direction.SOUTH in availableExits,
            onClick = { onMove(Direction.SOUTH) }
        )
    }
}

@Composable
private fun DPadButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    isLook: Boolean = false
) {
    FilledTonalButton(
        onClick = if (enabled) onClick else ({}),
        enabled = true, // Always render as "enabled" so it stays visible
        modifier = Modifier.size(40.dp),
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

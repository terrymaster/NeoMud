package com.neomud.client.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.neomud.client.ui.theme.StoneTheme

@Composable
fun StoneButton(
    icon: String,
    color: Color,
    size: Dp = 36.dp,
    isActive: Boolean = false,
    enabled: Boolean = true,
    overrideIconUrl: String? = null,
    onClick: () -> Unit
) {
    val activeState = isActive
    val btnColor = color
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .alpha(if (enabled) 1f else 0.4f)
            .drawBehind { drawStoneButton(activeState, btnColor) }
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        if (overrideIconUrl != null) {
            AsyncImage(
                model = overrideIconUrl,
                contentDescription = null,
                modifier = Modifier.size(size * 0.6f)
            )
        } else {
            Text(
                text = icon,
                fontSize = (size.value * 0.5f).sp,
                color = if (enabled) color else Color.Gray
            )
        }
    }
}

private fun DrawScope.drawStoneButton(isActive: Boolean, color: Color) {
    val w = size.width
    val h = size.height

    // Background gradient
    val bgBrush = Brush.verticalGradient(
        colors = listOf(StoneTheme.frameLight, StoneTheme.frameDark)
    )
    drawRect(bgBrush)

    // Active glow overlay
    if (isActive) {
        drawRect(color.copy(alpha = 0.3f))
    }

    // Bevel: light top-left
    drawLine(StoneTheme.frameLight, Offset(0f, 0f), Offset(w, 0f), strokeWidth = 1f)
    drawLine(StoneTheme.frameLight, Offset(0f, 0f), Offset(0f, h), strokeWidth = 1f)

    // Bevel: dark bottom-right
    val borderColor = if (isActive) color else StoneTheme.innerShadow
    drawLine(borderColor, Offset(0f, h - 1f), Offset(w, h - 1f), strokeWidth = 1f)
    drawLine(borderColor, Offset(w - 1f, 0f), Offset(w - 1f, h), strokeWidth = 1f)

    // Active: colored top-left border too
    if (isActive) {
        drawLine(color, Offset(0f, 0f), Offset(w, 0f), strokeWidth = 1f)
        drawLine(color, Offset(0f, 0f), Offset(0f, h), strokeWidth = 1f)
    }
}

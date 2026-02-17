package com.neomud.client.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.neomud.client.ui.theme.StoneTheme

@Composable
fun ThemedFrame(
    modifier: Modifier = Modifier,
    borderWidth: Dp = 3.dp,
    cornerAccent: Boolean = true,
    innerGlow: Boolean = false,
    overrideImageUrl: String? = null,
    content: @Composable BoxScope.() -> Unit
) {
    if (overrideImageUrl != null) {
        Box(modifier = modifier) {
            AsyncImage(
                model = overrideImageUrl,
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.matchParentSize()
            )
            Box(
                modifier = Modifier.padding(borderWidth),
                content = content
            )
        }
    } else {
        val showCorners = cornerAccent
        val showGlow = innerGlow
        Box(
            modifier = modifier.drawBehind {
                drawStoneFrame(borderWidth.toPx(), showCorners, showGlow)
            },
            content = content
        )
    }
}

private fun DrawScope.drawStoneFrame(borderPx: Float, cornerAccent: Boolean, innerGlow: Boolean) {
    val w = size.width
    val h = size.height

    // Frame body fill
    // Top
    drawRect(StoneTheme.frameMid, Offset.Zero, Size(w, borderPx))
    // Bottom
    drawRect(StoneTheme.frameMid, Offset(0f, h - borderPx), Size(w, borderPx))
    // Left
    drawRect(StoneTheme.frameMid, Offset(0f, borderPx), Size(borderPx, h - borderPx * 2))
    // Right
    drawRect(StoneTheme.frameMid, Offset(w - borderPx, borderPx), Size(borderPx, h - borderPx * 2))

    // Outer bevel highlight (top + left = light)
    drawLine(StoneTheme.frameLight, Offset(0f, 0f), Offset(w, 0f), strokeWidth = 1f)
    drawLine(StoneTheme.frameLight, Offset(0f, 0f), Offset(0f, h), strokeWidth = 1f)

    // Outer bevel shadow (bottom + right = dark)
    drawLine(StoneTheme.innerShadow, Offset(0f, h - 1f), Offset(w, h - 1f), strokeWidth = 1f)
    drawLine(StoneTheme.innerShadow, Offset(w - 1f, 0f), Offset(w - 1f, h), strokeWidth = 1f)

    // Inner edge shadow (bottom + right inside)
    drawLine(StoneTheme.innerShadow, Offset(borderPx, h - borderPx), Offset(w - borderPx, h - borderPx), strokeWidth = 1f)
    drawLine(StoneTheme.innerShadow, Offset(w - borderPx, borderPx), Offset(w - borderPx, h - borderPx), strokeWidth = 1f)

    // Inner glow
    if (innerGlow) {
        drawLine(StoneTheme.runeGlow, Offset(borderPx, borderPx), Offset(w - borderPx, borderPx), strokeWidth = 1f)
        drawLine(StoneTheme.runeGlow, Offset(borderPx, borderPx), Offset(borderPx, h - borderPx), strokeWidth = 1f)
    }

    // Corner rivets
    if (cornerAccent) {
        val rivetRadius = 2.dp.toPx()
        val rivetOffset = borderPx / 2f
        drawCircle(StoneTheme.metalGold, rivetRadius, Offset(rivetOffset, rivetOffset))
        drawCircle(StoneTheme.metalGold, rivetRadius, Offset(w - rivetOffset, rivetOffset))
        drawCircle(StoneTheme.metalGold, rivetRadius, Offset(rivetOffset, h - rivetOffset))
        drawCircle(StoneTheme.metalGold, rivetRadius, Offset(w - rivetOffset, h - rivetOffset))
    }
}

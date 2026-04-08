package com.neomud.client.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Canvas-drawn close X icon. Works on all platforms including WASM
 * where Unicode symbols may not render in the default font.
 */
@Composable
fun CloseIcon(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 12.dp,
    strokeWidth: Float = 2.5f
) {
    Canvas(modifier = modifier.size(size)) {
        val pad = this.size.width * 0.15f
        val w = this.size.width - pad
        val h = this.size.height - pad
        drawLine(color, Offset(pad, pad), Offset(w, h), strokeWidth, StrokeCap.Round)
        drawLine(color, Offset(w, pad), Offset(pad, h), strokeWidth, StrokeCap.Round)
    }
}

/**
 * Canvas-drawn look/eye icon for the d-pad center button.
 * Draws a simple eye shape with pupil.
 */
@Composable
fun LookIcon(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 16.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val cx = this.size.width / 2f
        val cy = this.size.height / 2f
        val rx = this.size.width * 0.42f
        val ry = this.size.height * 0.25f

        // Eye outline — draw as an oval
        drawOval(
            color = color,
            topLeft = Offset(cx - rx, cy - ry),
            size = androidx.compose.ui.geometry.Size(rx * 2, ry * 2),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.8f)
        )

        // Pupil — filled circle in center
        drawCircle(
            color = color,
            radius = this.size.width * 0.12f,
            center = Offset(cx, cy)
        )
    }
}

/** Canvas-drawn right-pointing triangle for collapsed accordion sections. */
@Composable
fun ExpandArrow(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 10.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val path = Path().apply {
            moveTo(w * 0.2f, h * 0.1f)
            lineTo(w * 0.85f, h * 0.5f)
            lineTo(w * 0.2f, h * 0.9f)
            close()
        }
        drawPath(path, color)
    }
}

/** Canvas-drawn down-pointing triangle for expanded accordion sections. */
@Composable
fun CollapseArrow(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 10.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val path = Path().apply {
            moveTo(w * 0.1f, h * 0.2f)
            lineTo(w * 0.9f, h * 0.2f)
            lineTo(w * 0.5f, h * 0.85f)
            close()
        }
        drawPath(path, color)
    }
}

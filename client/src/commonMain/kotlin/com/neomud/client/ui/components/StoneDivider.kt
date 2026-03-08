package com.neomud.client.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.neomud.client.ui.theme.StoneTheme

@Composable
fun StoneDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(3.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        StoneTheme.frameLight,
                        StoneTheme.frameMid,
                        StoneTheme.innerShadow
                    )
                )
            )
    )
}

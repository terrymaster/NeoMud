package com.neomud.client.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neomud.client.viewmodel.LogEntry

@Composable
fun GameLog(
    entries: List<LogEntry>,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(entries.size) {
        if (entries.isNotEmpty()) {
            listState.animateScrollToItem(entries.size - 1)
        }
    }

    val scrollbarTrackColor = Color(0x33FFFFFF)
    val scrollbarThumbColor = Color(0x99FFFFFF)
    val scrollbarWidth = 4.dp

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
            .padding(8.dp)
            .drawWithContent {
                drawContent()
                val info = listState.layoutInfo
                val totalItems = info.totalItemsCount
                if (totalItems > 0 && info.visibleItemsInfo.isNotEmpty()) {
                    val viewportHeight = info.viewportSize.height.toFloat()
                    val firstVisible = info.visibleItemsInfo.first()
                    val scrollFraction = firstVisible.index.toFloat() / totalItems
                    val visibleFraction = (info.visibleItemsInfo.size.toFloat() / totalItems)
                        .coerceAtMost(1f)
                    val thumbHeight = (visibleFraction * viewportHeight).coerceAtLeast(16.dp.toPx())
                    val trackHeight = viewportHeight
                    val thumbTop = scrollFraction * (trackHeight - thumbHeight)
                    val barW = scrollbarWidth.toPx()
                    val barX = size.width - barW

                    // Track
                    drawRect(
                        color = scrollbarTrackColor,
                        topLeft = Offset(barX, 0f),
                        size = Size(barW, trackHeight)
                    )
                    // Thumb
                    drawRect(
                        color = scrollbarThumbColor,
                        topLeft = Offset(barX, thumbTop),
                        size = Size(barW, thumbHeight)
                    )
                }
            }
    ) {
        items(entries) { entry ->
            Text(
                text = buildAnnotatedString {
                    for (span in entry.spans) {
                        withStyle(SpanStyle(color = span.color)) {
                            append(span.text)
                        }
                    }
                },
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                modifier = Modifier.padding(vertical = 1.dp)
            )
        }
    }
}

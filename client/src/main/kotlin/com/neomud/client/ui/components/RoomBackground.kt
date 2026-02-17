package com.neomud.client.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import coil.request.CachePolicy

private val FallbackColor = Color(0xFF0D1117)

@Composable
fun RoomBackground(
    imageUrl: String,
    roomName: String,
    modifier: Modifier = Modifier
) {
    val serverBaseUrl = LocalServerBaseUrl.current
    Box(modifier = modifier.background(FallbackColor)) {
        if (imageUrl.isNotEmpty()) {
            AsyncImage(
                model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                    .data("$serverBaseUrl$imageUrl")
                    .crossfade(300)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .build(),
                contentDescription = roomName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

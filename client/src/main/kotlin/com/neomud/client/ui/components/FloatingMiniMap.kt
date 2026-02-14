package com.neomud.client.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.neomud.shared.model.MapRoom
import com.neomud.shared.model.RoomId

@Composable
fun FloatingMiniMap(
    rooms: List<MapRoom>,
    playerRoomId: RoomId,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(100.dp)
            .alpha(0.85f)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xCC0D1117))
            .border(1.dp, Color(0xFF444444), RoundedCornerShape(8.dp))
    ) {
        MiniMap(
            rooms = rooms,
            playerRoomId = playerRoomId
        )
    }
}

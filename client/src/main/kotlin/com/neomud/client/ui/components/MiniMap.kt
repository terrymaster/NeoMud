package com.neomud.client.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.neomud.shared.model.Direction
import com.neomud.shared.model.MapRoom
import com.neomud.shared.model.RoomId

@Composable
fun MiniMap(
    rooms: List<MapRoom>,
    playerRoomId: RoomId,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
    ) {
        if (rooms.isEmpty()) return@Canvas

        val playerRoom = rooms.find { it.id == playerRoomId } ?: return@Canvas
        val centerX = size.width / 2
        val centerY = size.height / 2
        val cellSize = 48f
        val roomSize = 36f

        // Build a set of room positions for quick lookup
        val roomPositions = mutableMapOf<RoomId, Offset>()
        for (room in rooms) {
            val dx = (room.x - playerRoom.x).toFloat()
            val dy = -(room.y - playerRoom.y).toFloat() // Flip Y: higher Y = north = up
            val px = centerX + dx * cellSize
            val py = centerY + dy * cellSize
            roomPositions[room.id] = Offset(px, py)
        }

        // Draw exit lines
        for (room in rooms) {
            val from = roomPositions[room.id] ?: continue
            for ((_, targetId) in room.exits) {
                val to = roomPositions[targetId] ?: continue
                drawLine(
                    color = Color(0xFF444444),
                    start = from,
                    end = to,
                    strokeWidth = 2f
                )
            }
        }

        // Draw rooms
        for (room in rooms) {
            val pos = roomPositions[room.id] ?: continue
            val color = when {
                room.id == playerRoomId -> Color(0xFF4CAF50) // Green for player
                room.hasPlayers -> Color(0xFF42A5F5) // Blue for other players
                room.hasNpcs -> Color(0xFFFF9800) // Orange for NPCs
                else -> Color(0xFF555555) // Gray for empty
            }

            drawRect(
                color = color,
                topLeft = Offset(pos.x - roomSize / 2, pos.y - roomSize / 2),
                size = Size(roomSize, roomSize)
            )
        }
    }
}

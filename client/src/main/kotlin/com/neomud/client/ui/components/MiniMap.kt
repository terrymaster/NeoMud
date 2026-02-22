package com.neomud.client.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.neomud.shared.model.MapRoom
import com.neomud.shared.model.RoomId
import kotlin.math.abs

private fun zoneColor(zoneId: String): Color {
    if (zoneId.isEmpty()) return Color(0xFF555555)
    val hash = zoneId.hashCode()
    val hue = (abs(hash) % 360).toFloat()
    // Convert HSL (hue, 40% sat, 45% lightness) to RGB for muted pastel zone colors
    val s = 0.4f
    val l = 0.45f
    val c = (1f - abs(2f * l - 1f)) * s
    val x = c * (1f - abs((hue / 60f) % 2f - 1f))
    val m = l - c / 2f
    val (r1, g1, b1) = when {
        hue < 60f  -> Triple(c, x, 0f)
        hue < 120f -> Triple(x, c, 0f)
        hue < 180f -> Triple(0f, c, x)
        hue < 240f -> Triple(0f, x, c)
        hue < 300f -> Triple(x, 0f, c)
        else       -> Triple(c, 0f, x)
    }
    return Color(r1 + m, g1 + m, b1 + m)
}

@Composable
fun MiniMap(
    rooms: List<MapRoom>,
    playerRoomId: RoomId,
    visitedRoomIds: Set<RoomId> = emptySet(),
    fogOfWar: Boolean = false,
    cellSize: Float = 48f,
    roomSize: Float = 36f,
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

        val roomById = rooms.associateBy { it.id }

        // Build positions for all rooms (needed for exit line targets)
        val roomPositions = mutableMapOf<RoomId, Offset>()
        for (room in rooms) {
            val dx = (room.x - playerRoom.x).toFloat()
            val dy = -(room.y - playerRoom.y).toFloat()
            val px = centerX + dx * cellSize
            val py = centerY + dy * cellSize
            roomPositions[room.id] = Offset(px, py)
        }

        // Determine which rooms to draw
        val visibleRooms = if (fogOfWar) {
            rooms.filter { it.id in visitedRoomIds }
        } else {
            rooms
        }

        // Draw exit lines from visible rooms
        for (room in visibleRooms) {
            val from = roomPositions[room.id] ?: continue
            for ((_, targetId) in room.exits) {
                if (fogOfWar && targetId !in visitedRoomIds) {
                    // Gray stub: half-length line + small dot for unvisited exit
                    val targetPos = roomPositions[targetId]
                    if (targetPos != null) {
                        val midX = from.x + (targetPos.x - from.x) * 0.5f
                        val midY = from.y + (targetPos.y - from.y) * 0.5f
                        val mid = Offset(midX, midY)
                        drawLine(
                            color = Color(0xFF333333),
                            start = from,
                            end = mid,
                            strokeWidth = 2f
                        )
                        drawCircle(
                            color = Color(0xFF444444),
                            radius = 4f,
                            center = mid
                        )
                    }
                } else {
                    val to = roomPositions[targetId] ?: continue
                    drawLine(
                        color = Color(0xFF444444),
                        start = from,
                        end = to,
                        strokeWidth = 2f
                    )
                }
            }
        }

        // Draw rooms
        for (room in visibleRooms) {
            val pos = roomPositions[room.id] ?: continue
            val baseColor = when {
                room.id == playerRoomId -> Color(0xFF4CAF50) // Green for player room
                fogOfWar -> zoneColor(room.zoneId)
                room.hasPlayers -> Color(0xFF42A5F5)
                room.hasNpcs -> Color(0xFFFF9800)
                else -> Color(0xFF555555)
            }

            drawRect(
                color = baseColor,
                topLeft = Offset(pos.x - roomSize / 2, pos.y - roomSize / 2),
                size = Size(roomSize, roomSize)
            )

            // Draw entity indicators as small dots on zone-colored rooms
            if (fogOfWar && room.id != playerRoomId) {
                if (room.hasPlayers) {
                    drawCircle(
                        color = Color(0xFF42A5F5),
                        radius = 4f,
                        center = Offset(pos.x + roomSize / 4, pos.y - roomSize / 4)
                    )
                }
                if (room.hasNpcs) {
                    drawCircle(
                        color = Color(0xFFFF9800),
                        radius = 4f,
                        center = Offset(pos.x - roomSize / 4, pos.y - roomSize / 4)
                    )
                }
            }
        }
    }
}

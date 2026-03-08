package com.neomud.client.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.neomud.shared.model.Direction
import com.neomud.shared.model.MapRoom
import com.neomud.shared.model.RoomId
import kotlin.math.abs

private val ExitColorNormal = Color(0xFF444444)
private val ExitColorLocked = Color(0xFFCC8833)
private val ExitColorHidden = Color(0xFF7755AA)
private val FogStubColor = Color(0xFF333333)
private val FogDotColor = Color(0xFF444444)
private val UpDownFill = Color(0xFFE0E0E0)       // bright light gray fill
private val UpDownOutline = Color(0xFF111111)     // dark outline for contrast

private fun zoneColor(zoneId: String): Color {
    if (zoneId.isEmpty()) return Color(0xFF555555)
    val hash = zoneId.hashCode()
    val hue = (abs(hash) % 360).toFloat()
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

private fun exitColor(dir: Direction, room: MapRoom): Color = when {
    dir in room.lockedExits -> ExitColorLocked
    dir in room.hiddenExits -> ExitColorHidden
    else -> ExitColorNormal
}

private fun isHiddenExit(dir: Direction, room: MapRoom): Boolean = dir in room.hiddenExits

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

        // Build positions for all rooms
        val roomPositions = mutableMapOf<RoomId, Offset>()
        for (room in rooms) {
            val dx = (room.x - playerRoom.x).toFloat()
            val dy = -(room.y - playerRoom.y).toFloat()
            val px = centerX + dx * cellSize
            val py = centerY + dy * cellSize
            roomPositions[room.id] = Offset(px, py)
        }

        val visibleRooms = if (fogOfWar) {
            rooms.filter { it.id in visitedRoomIds }
        } else {
            rooms
        }

        val dashedEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f), 0f)

        // Draw exit lines from visible rooms
        for (room in visibleRooms) {
            val from = roomPositions[room.id] ?: continue
            for ((dir, targetId) in room.exits) {
                // Skip UP/DOWN — they get triangle indicators instead of lines
                if (dir == Direction.UP || dir == Direction.DOWN) continue

                val color = exitColor(dir, room)
                val isDashed = isHiddenExit(dir, room)

                if (fogOfWar && targetId !in visitedRoomIds) {
                    // Fog stub: half-length line + small dot for unvisited exit
                    val targetPos = roomPositions[targetId]
                    if (targetPos != null) {
                        val midX = from.x + (targetPos.x - from.x) * 0.5f
                        val midY = from.y + (targetPos.y - from.y) * 0.5f
                        val mid = Offset(midX, midY)
                        val stubColor = if (color != ExitColorNormal) color.copy(alpha = 0.5f) else FogStubColor
                        if (isDashed) {
                            drawLine(color = stubColor, start = from, end = mid, strokeWidth = 2f, pathEffect = dashedEffect)
                        } else {
                            drawLine(color = stubColor, start = from, end = mid, strokeWidth = 2f)
                        }
                        drawCircle(color = stubColor.copy(alpha = 0.7f), radius = 4f, center = mid)
                    }
                } else {
                    val to = roomPositions[targetId] ?: continue
                    if (isDashed) {
                        drawLine(color = color, start = from, end = to, strokeWidth = 2f, pathEffect = dashedEffect)
                    } else {
                        drawLine(color = color, start = from, end = to, strokeWidth = 2f)
                    }
                    // Lock dot at midpoint for locked exits
                    if (dir in room.lockedExits) {
                        val mid = Offset((from.x + to.x) / 2f, (from.y + to.y) / 2f)
                        drawCircle(color = ExitColorLocked, radius = 3f, center = mid)
                    }
                }
            }
        }

        // Draw rooms
        for (room in visibleRooms) {
            val pos = roomPositions[room.id] ?: continue
            val baseColor = when {
                room.id == playerRoomId -> Color(0xFF4CAF50)
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

            // Draw UP/DOWN triangle indicators (larger, outlined for visibility)
            val triSize = 9f
            val hasNorth = Direction.NORTH in room.exits
            val hasSouth = Direction.SOUTH in room.exits

            if (Direction.UP in room.exits) {
                val upColor = if (Direction.UP in room.lockedExits) ExitColorLocked
                    else if (Direction.UP in room.hiddenExits) ExitColorHidden
                    else UpDownFill
                val offsetY = if (hasNorth) roomSize / 2 + triSize + 3f else roomSize / 2 + 3f
                val tipY = pos.y - offsetY
                drawUpTriangle(pos.x, tipY, triSize, upColor, UpDownOutline)
            }

            if (Direction.DOWN in room.exits) {
                val downColor = if (Direction.DOWN in room.lockedExits) ExitColorLocked
                    else if (Direction.DOWN in room.hiddenExits) ExitColorHidden
                    else UpDownFill
                val offsetY = if (hasSouth) roomSize / 2 + triSize + 3f else roomSize / 2 + 3f
                val tipY = pos.y + offsetY
                drawDownTriangle(pos.x, tipY, triSize, downColor, UpDownOutline)
            }
        }
    }
}

private fun DrawScope.drawUpTriangle(cx: Float, tipY: Float, size: Float, color: Color, outline: Color) {
    val path = Path().apply {
        moveTo(cx, tipY)
        lineTo(cx - size, tipY + size)
        lineTo(cx + size, tipY + size)
        close()
    }
    drawPath(path, outline, style = Stroke(width = 3f))
    drawPath(path, color)
}

private fun DrawScope.drawDownTriangle(cx: Float, tipY: Float, size: Float, color: Color, outline: Color) {
    val path = Path().apply {
        moveTo(cx, tipY)
        lineTo(cx - size, tipY - size)
        lineTo(cx + size, tipY - size)
        close()
    }
    drawPath(path, outline, style = Stroke(width = 3f))
    drawPath(path, color)
}

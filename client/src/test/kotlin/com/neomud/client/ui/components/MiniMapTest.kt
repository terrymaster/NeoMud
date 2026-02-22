package com.neomud.client.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.unit.dp
import com.neomud.client.testutil.TestThemeWrapper
import com.neomud.shared.model.Direction
import com.neomud.shared.model.MapRoom
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MiniMapTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val squareRoom = MapRoom("r1", "Center", 0, 0, mapOf(
        Direction.NORTH to "r2",
        Direction.SOUTH to "r3",
        Direction.EAST to "r4",
        Direction.WEST to "r5"
    ))

    @Test
    fun `renders without crash with empty room list`() {
        composeRule.setContent {
            TestThemeWrapper {
                MiniMap(
                    rooms = emptyList(),
                    playerRoomId = "r1",
                    modifier = Modifier.size(200.dp)
                )
            }
        }
        composeRule.onRoot().assertIsDisplayed()
    }

    @Test
    fun `renders basic rooms with cardinal exits`() {
        val rooms = listOf(
            squareRoom,
            MapRoom("r2", "North", 0, 1, mapOf(Direction.SOUTH to "r1")),
            MapRoom("r3", "South", 0, -1, mapOf(Direction.NORTH to "r1")),
            MapRoom("r4", "East", 1, 0, mapOf(Direction.WEST to "r1")),
            MapRoom("r5", "West", -1, 0, mapOf(Direction.EAST to "r1"))
        )
        composeRule.setContent {
            TestThemeWrapper {
                MiniMap(
                    rooms = rooms,
                    playerRoomId = "r1",
                    modifier = Modifier.size(300.dp)
                )
            }
        }
        composeRule.onRoot().assertIsDisplayed()
    }

    @Test
    fun `renders UP and DOWN exit triangles without crash`() {
        val rooms = listOf(
            MapRoom("r1", "Center", 0, 0, mapOf(
                Direction.NORTH to "r2",
                Direction.UP to "r3",
                Direction.DOWN to "r4"
            )),
            MapRoom("r2", "North", 0, 1, mapOf(Direction.SOUTH to "r1")),
            MapRoom("r3", "Above", 0, 0, mapOf(Direction.DOWN to "r1")),
            MapRoom("r4", "Below", 0, 0, mapOf(Direction.UP to "r1"))
        )
        composeRule.setContent {
            TestThemeWrapper {
                MiniMap(
                    rooms = rooms,
                    playerRoomId = "r1",
                    modifier = Modifier.size(300.dp)
                )
            }
        }
        composeRule.onRoot().assertIsDisplayed()
    }

    @Test
    fun `renders locked exits with amber styling without crash`() {
        val rooms = listOf(
            MapRoom("r1", "Center", 0, 0,
                mapOf(Direction.NORTH to "r2", Direction.EAST to "r3"),
                lockedExits = setOf(Direction.NORTH)
            ),
            MapRoom("r2", "North", 0, 1, mapOf(Direction.SOUTH to "r1")),
            MapRoom("r3", "East", 1, 0, mapOf(Direction.WEST to "r1"))
        )
        composeRule.setContent {
            TestThemeWrapper {
                MiniMap(
                    rooms = rooms,
                    playerRoomId = "r1",
                    modifier = Modifier.size(300.dp)
                )
            }
        }
        composeRule.onRoot().assertIsDisplayed()
    }

    @Test
    fun `renders hidden exits with dashed purple styling without crash`() {
        val rooms = listOf(
            MapRoom("r1", "Center", 0, 0,
                mapOf(Direction.NORTH to "r2", Direction.SOUTH to "r3"),
                hiddenExits = setOf(Direction.SOUTH)
            ),
            MapRoom("r2", "North", 0, 1, mapOf(Direction.SOUTH to "r1")),
            MapRoom("r3", "South", 0, -1, mapOf(Direction.NORTH to "r1"))
        )
        composeRule.setContent {
            TestThemeWrapper {
                MiniMap(
                    rooms = rooms,
                    playerRoomId = "r1",
                    modifier = Modifier.size(300.dp)
                )
            }
        }
        composeRule.onRoot().assertIsDisplayed()
    }

    @Test
    fun `renders locked UP exit triangle without crash`() {
        val rooms = listOf(
            MapRoom("r1", "Center", 0, 0,
                mapOf(Direction.UP to "r2"),
                lockedExits = setOf(Direction.UP)
            ),
            MapRoom("r2", "Above", 0, 0, mapOf(Direction.DOWN to "r1"))
        )
        composeRule.setContent {
            TestThemeWrapper {
                MiniMap(
                    rooms = rooms,
                    playerRoomId = "r1",
                    modifier = Modifier.size(200.dp)
                )
            }
        }
        composeRule.onRoot().assertIsDisplayed()
    }

    @Test
    fun `renders hidden DOWN exit triangle without crash`() {
        val rooms = listOf(
            MapRoom("r1", "Center", 0, 0,
                mapOf(Direction.DOWN to "r2"),
                hiddenExits = setOf(Direction.DOWN)
            ),
            MapRoom("r2", "Below", 0, 0, mapOf(Direction.UP to "r1"))
        )
        composeRule.setContent {
            TestThemeWrapper {
                MiniMap(
                    rooms = rooms,
                    playerRoomId = "r1",
                    modifier = Modifier.size(200.dp)
                )
            }
        }
        composeRule.onRoot().assertIsDisplayed()
    }

    @Test
    fun `renders with fog of war and visited rooms`() {
        val rooms = listOf(
            squareRoom,
            MapRoom("r2", "North", 0, 1, mapOf(Direction.SOUTH to "r1")),
            MapRoom("r3", "South", 0, -1, mapOf(Direction.NORTH to "r1"))
        )
        composeRule.setContent {
            TestThemeWrapper {
                MiniMap(
                    rooms = rooms,
                    playerRoomId = "r1",
                    visitedRoomIds = setOf("r1", "r2"),
                    fogOfWar = true,
                    modifier = Modifier.size(300.dp)
                )
            }
        }
        composeRule.onRoot().assertIsDisplayed()
    }

    @Test
    fun `renders fog of war stubs for locked unvisited exits`() {
        val rooms = listOf(
            MapRoom("r1", "Center", 0, 0,
                mapOf(Direction.NORTH to "r2"),
                lockedExits = setOf(Direction.NORTH)
            ),
            MapRoom("r2", "North", 0, 1, mapOf(Direction.SOUTH to "r1"))
        )
        composeRule.setContent {
            TestThemeWrapper {
                MiniMap(
                    rooms = rooms,
                    playerRoomId = "r1",
                    visitedRoomIds = setOf("r1"), // r2 not visited
                    fogOfWar = true,
                    modifier = Modifier.size(300.dp)
                )
            }
        }
        composeRule.onRoot().assertIsDisplayed()
    }

    @Test
    fun `renders combined locked hidden and up-down exits`() {
        val rooms = listOf(
            MapRoom("r1", "Center", 0, 0,
                mapOf(
                    Direction.NORTH to "r2",
                    Direction.EAST to "r3",
                    Direction.UP to "r4",
                    Direction.DOWN to "r5"
                ),
                lockedExits = setOf(Direction.NORTH, Direction.UP),
                hiddenExits = setOf(Direction.EAST)
            ),
            MapRoom("r2", "North", 0, 1, mapOf(Direction.SOUTH to "r1")),
            MapRoom("r3", "East", 1, 0, mapOf(Direction.WEST to "r1")),
            MapRoom("r4", "Above", 0, 0, mapOf(Direction.DOWN to "r1")),
            MapRoom("r5", "Below", 0, 0, mapOf(Direction.UP to "r1"))
        )
        composeRule.setContent {
            TestThemeWrapper {
                MiniMap(
                    rooms = rooms,
                    playerRoomId = "r1",
                    modifier = Modifier.size(300.dp)
                )
            }
        }
        composeRule.onRoot().assertIsDisplayed()
    }
}

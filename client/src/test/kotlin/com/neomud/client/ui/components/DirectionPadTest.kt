package com.neomud.client.ui.components

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.neomud.client.testutil.TestThemeWrapper
import com.neomud.shared.model.Direction
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DirectionPadTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `north button fires onMove NORTH when available`() {
        var moved: Direction? = null
        composeRule.setContent {
            TestThemeWrapper {
                DirectionPad(
                    availableExits = setOf(Direction.NORTH),
                    onMove = { moved = it },
                    onLook = {}
                )
            }
        }

        // North button uses ▲ (U+25B2)
        composeRule.onNodeWithText("\u25B2").performClick()
        assert(moved == Direction.NORTH) { "Expected NORTH, got $moved" }
    }

    @Test
    fun `south button fires onMove SOUTH when available`() {
        var moved: Direction? = null
        composeRule.setContent {
            TestThemeWrapper {
                DirectionPad(
                    availableExits = setOf(Direction.SOUTH),
                    onMove = { moved = it },
                    onLook = {}
                )
            }
        }

        composeRule.onNodeWithText("\u25BC").performClick()
        assert(moved == Direction.SOUTH) { "Expected SOUTH, got $moved" }
    }

    @Test
    fun `east and west buttons fire correct directions`() {
        val moves = mutableListOf<Direction>()
        composeRule.setContent {
            TestThemeWrapper {
                DirectionPad(
                    availableExits = setOf(Direction.EAST, Direction.WEST),
                    onMove = { moves.add(it) },
                    onLook = {}
                )
            }
        }

        composeRule.onNodeWithText("\u25B6").performClick() // East ▶
        composeRule.onNodeWithText("\u25C0").performClick() // West ◀
        assert(moves == listOf(Direction.EAST, Direction.WEST)) { "Expected [EAST, WEST], got $moves" }
    }

    @Test
    fun `diagonal buttons fire correct directions`() {
        val moves = mutableListOf<Direction>()
        composeRule.setContent {
            TestThemeWrapper {
                DirectionPad(
                    availableExits = Direction.entries.toSet(),
                    onMove = { moves.add(it) },
                    onLook = {}
                )
            }
        }

        composeRule.onNodeWithText("\u2196").performClick() // NW ↖
        composeRule.onNodeWithText("\u2197").performClick() // NE ↗
        composeRule.onNodeWithText("\u2199").performClick() // SW ↙
        composeRule.onNodeWithText("\u2198").performClick() // SE ↘
        assert(moves == listOf(Direction.NORTHWEST, Direction.NORTHEAST, Direction.SOUTHWEST, Direction.SOUTHEAST)) {
            "Expected [NW, NE, SW, SE], got $moves"
        }
    }

    @Test
    fun `up and down stair buttons fire correct directions`() {
        val moves = mutableListOf<Direction>()
        composeRule.setContent {
            TestThemeWrapper {
                DirectionPad(
                    availableExits = setOf(Direction.UP, Direction.DOWN),
                    onMove = { moves.add(it) },
                    onLook = {}
                )
            }
        }

        composeRule.onNodeWithText("\u2B06").performClick() // UP ⬆
        composeRule.onNodeWithText("\u2B07").performClick() // DOWN ⬇
        assert(moves == listOf(Direction.UP, Direction.DOWN)) { "Expected [UP, DOWN], got $moves" }
    }

    @Test
    fun `disabled direction does not fire onMove`() {
        var moved: Direction? = null
        composeRule.setContent {
            TestThemeWrapper {
                DirectionPad(
                    availableExits = emptySet(), // No exits available
                    onMove = { moved = it },
                    onLook = {}
                )
            }
        }

        composeRule.onNodeWithText("\u25B2").performClick() // North
        composeRule.onNodeWithText("\u25BC").performClick() // South
        composeRule.onNodeWithText("\u25B6").performClick() // East
        assert(moved == null) { "Expected no move, got $moved" }
    }

    @Test
    fun `look button always fires`() {
        var looked = false
        composeRule.setContent {
            TestThemeWrapper {
                DirectionPad(
                    availableExits = emptySet(), // No exits at all
                    onMove = {},
                    onLook = { looked = true }
                )
            }
        }

        composeRule.onNodeWithText("\uD83D\uDC41").performClick() // Look 👁
        assert(looked) { "Expected look to fire" }
    }
}

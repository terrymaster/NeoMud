package com.neomud.client.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.neomud.client.testutil.CoilTestRule
import com.neomud.client.testutil.TestThemeWrapper
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StoneButtonTest {

    private val coilRule = CoilTestRule()
    private val composeRule = createComposeRule()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(coilRule).around(composeRule)

    @Test
    fun `click callback fires when enabled`() {
        var clicked = false
        composeRule.setContent {
            TestThemeWrapper {
                StoneButton(
                    icon = "\u2694", // ⚔
                    color = Color.Red,
                    enabled = true,
                    onClick = { clicked = true }
                )
            }
        }

        composeRule.onNodeWithText("\u2694").performClick()
        assert(clicked) { "Expected click to fire" }
    }

    @Test
    fun `click does not fire when disabled`() {
        var clicked = false
        composeRule.setContent {
            TestThemeWrapper {
                StoneButton(
                    icon = "\u2694",
                    color = Color.Red,
                    enabled = false,
                    onClick = { clicked = true }
                )
            }
        }

        composeRule.onNodeWithText("\u2694").performClick()
        assert(!clicked) { "Expected click not to fire when disabled" }
    }

    @Test
    fun `displays icon text`() {
        composeRule.setContent {
            TestThemeWrapper {
                StoneButton(
                    icon = "\uD83D\uDEE1", // 🛡
                    color = Color.Blue,
                    onClick = {}
                )
            }
        }

        composeRule.onNodeWithText("\uD83D\uDEE1").assertIsDisplayed()
    }
}

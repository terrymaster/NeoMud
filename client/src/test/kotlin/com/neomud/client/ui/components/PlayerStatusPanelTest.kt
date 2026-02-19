package com.neomud.client.ui.components

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.neomud.client.testutil.TestData
import com.neomud.client.testutil.TestThemeWrapper
import com.neomud.shared.model.EffectType
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PlayerStatusPanelTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `displays HP and MP text`() {
        val player = TestData.player(currentHp = 80, maxHp = 100, currentMp = 30, maxMp = 50)
        composeRule.setContent {
            TestThemeWrapper {
                PlayerStatusPanel(
                    player = player,
                    activeEffects = emptyList(),
                    onClick = {}
                )
            }
        }

        composeRule.onNodeWithText("HP: 80/100").assertIsDisplayed()
        composeRule.onNodeWithText("MP: 30/50").assertIsDisplayed()
    }

    @Test
    fun `displays HP text for full health`() {
        val player = TestData.player(currentHp = 100, maxHp = 100)
        composeRule.setContent {
            TestThemeWrapper {
                PlayerStatusPanel(
                    player = player,
                    activeEffects = emptyList(),
                    onClick = {}
                )
            }
        }

        composeRule.onNodeWithText("HP: 100/100").assertIsDisplayed()
    }

    @Test
    fun `displays HP text for low health`() {
        val player = TestData.player(currentHp = 10, maxHp = 100)
        composeRule.setContent {
            TestThemeWrapper {
                PlayerStatusPanel(
                    player = player,
                    activeEffects = emptyList(),
                    onClick = {}
                )
            }
        }

        composeRule.onNodeWithText("HP: 10/100").assertIsDisplayed()
    }

    @Test
    fun `shows active effects when present`() {
        val effects = listOf(
            TestData.activeEffect(name = "Poison", type = EffectType.POISON),
            TestData.activeEffect(name = "Haste", type = EffectType.HASTE)
        )
        composeRule.setContent {
            TestThemeWrapper {
                PlayerStatusPanel(
                    player = TestData.player(),
                    activeEffects = effects,
                    onClick = {}
                )
            }
        }

        // Poison icon is ☠ (U+2620), Haste is ↻ (U+21BB)
        composeRule.onNodeWithText("\u2620").assertIsDisplayed()
        composeRule.onNodeWithText("\u21BB").assertIsDisplayed()
    }

    @Test
    fun `no effect icons when no effects and not hidden`() {
        composeRule.setContent {
            TestThemeWrapper {
                PlayerStatusPanel(
                    player = TestData.player(),
                    activeEffects = emptyList(),
                    isHidden = false,
                    onClick = {}
                )
            }
        }

        // Skull icon should not be present
        composeRule.onNodeWithText("\u2620").assertDoesNotExist()
    }

    @Test
    fun `shows hidden eye icon when isHidden`() {
        composeRule.setContent {
            TestThemeWrapper {
                PlayerStatusPanel(
                    player = TestData.player(),
                    activeEffects = emptyList(),
                    isHidden = true,
                    onClick = {}
                )
            }
        }

        // Eye icon 👁 (U+1F441)
        composeRule.onNodeWithText("\uD83D\uDC41").assertIsDisplayed()
    }

    @Test
    fun `clicking panel fires onClick`() {
        var clicked = false
        composeRule.setContent {
            TestThemeWrapper {
                PlayerStatusPanel(
                    player = TestData.player(),
                    activeEffects = emptyList(),
                    onClick = { clicked = true }
                )
            }
        }

        composeRule.onNodeWithText("HP: 80/100").performClick()
        assert(clicked) { "Expected onClick to fire" }
    }

    // --- Compact mode tests (landscape layout) ---

    @Test
    fun `compact mode displays HP and MP labels and counts`() {
        val player = TestData.player(currentHp = 8, maxHp = 10, currentMp = 11, maxMp = 15)
        composeRule.setContent {
            TestThemeWrapper {
                PlayerStatusPanel(
                    player = player,
                    activeEffects = emptyList(),
                    compact = true,
                    onClick = {}
                )
            }
        }

        composeRule.onNodeWithText("HP:").assertIsDisplayed()
        composeRule.onNodeWithText("8/10").assertIsDisplayed()
        composeRule.onNodeWithText("MP:").assertIsDisplayed()
        composeRule.onNodeWithText("11/15").assertIsDisplayed()
    }

    @Test
    fun `compact mode shows hidden eye icon when isHidden`() {
        composeRule.setContent {
            TestThemeWrapper {
                PlayerStatusPanel(
                    player = TestData.player(),
                    activeEffects = emptyList(),
                    isHidden = true,
                    compact = true,
                    onClick = {}
                )
            }
        }

        composeRule.onNodeWithText("\uD83D\uDC41").assertIsDisplayed()
    }

    @Test
    fun `compact mode no eye icon when not hidden`() {
        composeRule.setContent {
            TestThemeWrapper {
                PlayerStatusPanel(
                    player = TestData.player(),
                    activeEffects = emptyList(),
                    isHidden = false,
                    compact = true,
                    onClick = {}
                )
            }
        }

        composeRule.onNodeWithText("\uD83D\uDC41").assertDoesNotExist()
    }

    @Test
    fun `shows meditation icon when isMeditating`() {
        composeRule.setContent {
            TestThemeWrapper {
                PlayerStatusPanel(
                    player = TestData.player(),
                    activeEffects = emptyList(),
                    isMeditating = true,
                    onClick = {}
                )
            }
        }

        // Meditation icon 🧘 (U+1F9D8)
        composeRule.onNodeWithText("\uD83E\uDDD8").assertIsDisplayed()
    }

    @Test
    fun `no meditation icon when not meditating`() {
        composeRule.setContent {
            TestThemeWrapper {
                PlayerStatusPanel(
                    player = TestData.player(),
                    activeEffects = emptyList(),
                    isMeditating = false,
                    onClick = {}
                )
            }
        }

        composeRule.onNodeWithText("\uD83E\uDDD8").assertDoesNotExist()
    }

    @Test
    fun `compact mode shows meditation icon when isMeditating`() {
        composeRule.setContent {
            TestThemeWrapper {
                PlayerStatusPanel(
                    player = TestData.player(),
                    activeEffects = emptyList(),
                    isMeditating = true,
                    compact = true,
                    onClick = {}
                )
            }
        }

        composeRule.onNodeWithText("\uD83E\uDDD8").assertIsDisplayed()
    }

    @Test
    fun `compact mode no meditation icon when not meditating`() {
        composeRule.setContent {
            TestThemeWrapper {
                PlayerStatusPanel(
                    player = TestData.player(),
                    activeEffects = emptyList(),
                    isMeditating = false,
                    compact = true,
                    onClick = {}
                )
            }
        }

        composeRule.onNodeWithText("\uD83E\uDDD8").assertDoesNotExist()
    }

    @Test
    fun `compact mode clicking fires onClick`() {
        var clicked = false
        composeRule.setContent {
            TestThemeWrapper {
                PlayerStatusPanel(
                    player = TestData.player(),
                    activeEffects = emptyList(),
                    compact = true,
                    onClick = { clicked = true }
                )
            }
        }

        composeRule.onNodeWithText("HP:").performClick()
        assert(clicked) { "Expected onClick to fire in compact mode" }
    }
}

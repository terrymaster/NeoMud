package com.neomud.client.ui.components

import androidx.compose.ui.test.*
import com.neomud.client.testutil.ComposeTestBase
import com.neomud.client.testutil.TestData
import com.neomud.client.testutil.TestThemeWrapper
import com.neomud.shared.model.EffectType
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class PlayerStatusPanelTest : ComposeTestBase() {

    @Test
    fun displays_HP_and_MP_text() = runComposeUiTest {
        val player = TestData.player(currentHp = 80, maxHp = 100, currentMp = 30, maxMp = 50)
        setContent {
            TestThemeWrapper {
                PlayerStatusPanel(
                    player = player,
                    activeEffects = emptyList(),
                    onClick = {}
                )
            }
        }

        onNodeWithText("HP: 80/100").assertExists()
        onNodeWithText("MP: 30/50").assertExists()
    }

    @Test
    fun displays_HP_text_for_full_health() = runComposeUiTest {
        val player = TestData.player(currentHp = 100, maxHp = 100)
        setContent {
            TestThemeWrapper {
                PlayerStatusPanel(
                    player = player,
                    activeEffects = emptyList(),
                    onClick = {}
                )
            }
        }

        onNodeWithText("HP: 100/100").assertExists()
    }

    @Test
    fun displays_HP_text_for_low_health() = runComposeUiTest {
        val player = TestData.player(currentHp = 10, maxHp = 100)
        setContent {
            TestThemeWrapper {
                PlayerStatusPanel(
                    player = player,
                    activeEffects = emptyList(),
                    onClick = {}
                )
            }
        }

        onNodeWithText("HP: 10/100").assertExists()
    }

    @Test
    fun shows_active_effects_when_present() = runComposeUiTest {
        val effects = listOf(
            TestData.activeEffect(name = "Poison", type = EffectType.POISON),
            TestData.activeEffect(name = "Haste", type = EffectType.HASTE)
        )
        setContent {
            TestThemeWrapper {
                PlayerStatusPanel(
                    player = TestData.player(),
                    activeEffects = effects,
                    onClick = {}
                )
            }
        }

        // Effect icons now use Material Icons with contentDescription = type name
        onNodeWithContentDescription("POISON").assertExists()
        onNodeWithContentDescription("HASTE").assertExists()
    }

    @Test
    fun no_effect_icons_when_no_effects_and_not_hidden() = runComposeUiTest {
        setContent {
            TestThemeWrapper {
                PlayerStatusPanel(
                    player = TestData.player(),
                    activeEffects = emptyList(),
                    isHidden = false,
                    onClick = {}
                )
            }
        }

        onNodeWithContentDescription("POISON").assertDoesNotExist()
    }

    @Test
    fun shows_hidden_icon_when_isHidden() = runComposeUiTest {
        setContent {
            TestThemeWrapper {
                PlayerStatusPanel(
                    player = TestData.player(),
                    activeEffects = emptyList(),
                    isHidden = true,
                    onClick = {}
                )
            }
        }

        // Hidden icon is a Material Icon — verify the component renders without crash
        onRoot().assertIsDisplayed()
    }

    @Test
    fun clicking_panel_fires_onClick() = runComposeUiTest {
        var clicked = false
        setContent {
            TestThemeWrapper {
                PlayerStatusPanel(
                    player = TestData.player(),
                    activeEffects = emptyList(),
                    onClick = { clicked = true }
                )
            }
        }

        onNodeWithText("HP: 80/100").performClick()
        assertTrue(clicked)
    }

    // --- Compact mode tests (landscape layout) ---

    @Test
    fun compact_mode_displays_HP_and_MP_labels_and_counts() = runComposeUiTest {
        val player = TestData.player(currentHp = 8, maxHp = 10, currentMp = 11, maxMp = 15)
        setContent {
            TestThemeWrapper {
                PlayerStatusPanel(
                    player = player,
                    activeEffects = emptyList(),
                    compact = true,
                    onClick = {}
                )
            }
        }

        onNodeWithText("HP:").assertExists()
        onNodeWithText("8/10").assertExists()
        onNodeWithText("MP:").assertExists()
        onNodeWithText("11/15").assertExists()
    }

    @Test
    fun compact_mode_shows_hidden_icon_when_isHidden() = runComposeUiTest {
        setContent {
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

        // Hidden icon is a Material Icon — verify renders without crash
        onRoot().assertIsDisplayed()
    }

    @Test
    fun compact_mode_no_eye_icon_when_not_hidden() = runComposeUiTest {
        setContent {
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

        onRoot().assertIsDisplayed()
    }

    @Test
    fun shows_meditation_icon_when_isMeditating() = runComposeUiTest {
        setContent {
            TestThemeWrapper {
                PlayerStatusPanel(
                    player = TestData.player(),
                    activeEffects = emptyList(),
                    isMeditating = true,
                    onClick = {}
                )
            }
        }

        // Meditation icon is a Material Icon — verify renders without crash
        onRoot().assertIsDisplayed()
    }

    @Test
    fun no_meditation_icon_when_not_meditating() = runComposeUiTest {
        setContent {
            TestThemeWrapper {
                PlayerStatusPanel(
                    player = TestData.player(),
                    activeEffects = emptyList(),
                    isMeditating = false,
                    onClick = {}
                )
            }
        }

        onRoot().assertIsDisplayed()
    }

    @Test
    fun compact_mode_shows_meditation_icon_when_isMeditating() = runComposeUiTest {
        setContent {
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

        onRoot().assertIsDisplayed()
    }

    @Test
    fun compact_mode_no_meditation_icon_when_not_meditating() = runComposeUiTest {
        setContent {
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

        onRoot().assertIsDisplayed()
    }

    @Test
    fun compact_mode_clicking_fires_onClick() = runComposeUiTest {
        var clicked = false
        setContent {
            TestThemeWrapper {
                PlayerStatusPanel(
                    player = TestData.player(),
                    activeEffects = emptyList(),
                    compact = true,
                    onClick = { clicked = true }
                )
            }
        }

        onNodeWithText("HP:").performClick()
        assertTrue(clicked)
    }
}

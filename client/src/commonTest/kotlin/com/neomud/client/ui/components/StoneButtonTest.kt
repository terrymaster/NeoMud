package com.neomud.client.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.*
import com.neomud.client.testutil.ComposeTestBase
import com.neomud.client.testutil.TestThemeWrapper
import com.neomud.client.testutil.installTestCoil
import com.neomud.client.testutil.resetTestCoil
import neomud.client.generated.resources.Res
import neomud.client.generated.resources.icon_attack
import neomud.client.generated.resources.icon_settings
import kotlin.test.BeforeTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class StoneButtonTest : ComposeTestBase() {

    @BeforeTest
    fun setup() { installTestCoil() }

    @AfterTest
    fun teardown() { resetTestCoil() }

    @Test
    fun click_callback_fires_when_enabled() = runComposeUiTest {
        var clicked = false
        setContent {
            TestThemeWrapper {
                StoneButton(
                    icon = Res.drawable.icon_attack,
                    color = Color.Red,
                    enabled = true,
                    onClick = { clicked = true }
                )
            }
        }

        onNode(hasClickAction()).performClick()
        assertTrue(clicked)
    }

    @Test
    fun click_does_not_fire_when_disabled() = runComposeUiTest {
        var clicked = false
        setContent {
            TestThemeWrapper {
                StoneButton(
                    icon = Res.drawable.icon_attack,
                    color = Color.Red,
                    enabled = false,
                    onClick = { clicked = true }
                )
            }
        }

        onNode(hasClickAction()).performClick()
        assertFalse(clicked)
    }

    @Test
    fun displays_icon() = runComposeUiTest {
        setContent {
            TestThemeWrapper {
                StoneButton(
                    icon = Res.drawable.icon_settings,
                    color = Color.Blue,
                    onClick = {}
                )
            }
        }

        onRoot().assertIsDisplayed()
    }
}

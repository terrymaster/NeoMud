package com.neomud.client.smoke

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.neomud.client.MainActivity
import org.junit.Rule
import org.junit.Test

class NavigationSmokeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun connectShowsLoginFields() {
        // The login screen starts in disconnected state with host/port fields
        composeRule.onNodeWithText("Server Host").assertIsDisplayed()
        composeRule.onNodeWithText("Port").assertIsDisplayed()
        composeRule.onNodeWithText("Connect").assertIsDisplayed()
    }
}

package com.neomud.client.smoke

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.neomud.client.MainActivity
import org.junit.Rule
import org.junit.Test

class AppLaunchSmokeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun activityLaunches_loginScreenVisible() {
        composeRule.onNodeWithText("NeoMud").assertIsDisplayed()
        composeRule.onNodeWithText("Server Host").assertIsDisplayed()
        composeRule.onNodeWithText("Connect").assertIsDisplayed()
    }
}

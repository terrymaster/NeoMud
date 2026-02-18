package com.neomud.client.testutil

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.neomud.client.ui.components.LocalServerBaseUrl
import com.neomud.client.ui.theme.NeoMudTheme

/**
 * Wraps test content with NeoMudTheme and a test LocalServerBaseUrl.
 * Use this in all Compose UI tests to match the real app's composition tree.
 */
@Composable
fun TestThemeWrapper(content: @Composable () -> Unit) {
    NeoMudTheme {
        CompositionLocalProvider(LocalServerBaseUrl provides "http://test-server:8080") {
            content()
        }
    }
}

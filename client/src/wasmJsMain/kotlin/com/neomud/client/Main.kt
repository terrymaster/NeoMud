package com.neomud.client

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeViewport
import com.neomud.client.audio.WebAudioManager
import com.neomud.client.platform.LocalIsLandscape
import com.neomud.client.platform.LocalSetLayoutPreference
import com.neomud.client.ui.navigation.NeoMudApp
import com.neomud.client.ui.theme.NeoMudTheme
import com.neomud.client.viewmodel.AuthViewModel
import kotlinx.browser.document

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val rootElement = document.getElementById("ComposeTarget")!!
    ComposeViewport(rootElement) {
        val authViewModel = remember { AuthViewModel() }
        val audioManager = remember { WebAudioManager() }

        NeoMudTheme {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isLandscape = maxWidth > maxHeight

                CompositionLocalProvider(
                    LocalIsLandscape provides isLandscape,
                    LocalSetLayoutPreference provides { /* On web, resize the browser window */ }
                ) {
                    NeoMudApp(
                        authViewModel = authViewModel,
                        audioManager = audioManager
                    )
                }
            }
        }
    }
}

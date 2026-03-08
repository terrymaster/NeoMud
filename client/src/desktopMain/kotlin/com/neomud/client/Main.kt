package com.neomud.client

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.neomud.client.audio.DesktopAudioManager
import com.neomud.client.platform.LocalIsLandscape
import com.neomud.client.platform.LocalSetLayoutPreference
import com.neomud.client.ui.navigation.NeoMudApp
import com.neomud.client.ui.theme.NeoMudTheme
import com.neomud.client.viewmodel.AuthViewModel

fun main() = application {
    val windowState = rememberWindowState(width = 1280.dp, height = 720.dp)
    val authViewModel = remember { AuthViewModel() }
    val audioManager = remember { DesktopAudioManager() }

    // Desktop is always "landscape" (wide window)
    val isLandscape = windowState.size.width > windowState.size.height

    Window(
        onCloseRequest = {
            audioManager.release()
            exitApplication()
        },
        state = windowState,
        title = "NeoMud"
    ) {
        NeoMudTheme {
            CompositionLocalProvider(
                LocalIsLandscape provides isLandscape,
                LocalSetLayoutPreference provides { /* No-op on desktop — window is resizable */ }
            ) {
                NeoMudApp(
                    authViewModel = authViewModel,
                    audioManager = audioManager
                )
            }
        }
    }
}

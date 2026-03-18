package com.neomud.client

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.neomud.client.audio.IosAudioManager
import com.neomud.client.platform.LocalIsLandscape
import com.neomud.client.platform.LocalSetLayoutPreference
import com.neomud.client.ui.navigation.NeoMudApp
import com.neomud.client.ui.theme.NeoMudTheme
import com.neomud.client.viewmodel.AuthViewModel
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    return ComposeUIViewController {
        val authViewModel = remember { AuthViewModel() }
        val audioManager = remember { IosAudioManager() }

        NeoMudTheme {
            CompositionLocalProvider(
                LocalIsLandscape provides true, // MUD plays best in landscape — orientation locked in Xcode
                LocalSetLayoutPreference provides { /* Handled by iOS orientation lock in Info.plist */ }
            ) {
                NeoMudApp(
                    authViewModel = authViewModel,
                    audioManager = audioManager
                )
            }
        }
    }
}

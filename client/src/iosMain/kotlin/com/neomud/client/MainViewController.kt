package com.neomud.client

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.neomud.client.audio.IosAudioManager
import com.neomud.client.platform.LocalIsLandscape
import com.neomud.client.platform.LocalSetLayoutPreference
import com.neomud.client.ui.navigation.NeoMudApp
import com.neomud.client.ui.theme.NeoMudTheme
import com.neomud.client.viewmodel.AuthViewModel
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSUserDefaults
import platform.UIKit.UIViewController

private const val LANDSCAPE_LAYOUT_KEY = "landscape_layout"

@OptIn(ExperimentalForeignApi::class)
fun MainViewController(): UIViewController {
    return ComposeUIViewController {
        val defaults = remember { NSUserDefaults.standardUserDefaults }
        val isLandscape = remember {
            mutableStateOf(defaults.boolForKey(LANDSCAPE_LAYOUT_KEY))
        }
        val authViewModel = remember { AuthViewModel() }
        val audioManager = remember { IosAudioManager() }

        NeoMudTheme {
            CompositionLocalProvider(
                LocalIsLandscape provides isLandscape.value,
                LocalSetLayoutPreference provides { landscape ->
                    defaults.setBool(landscape, forKey = LANDSCAPE_LAYOUT_KEY)
                    isLandscape.value = landscape
                }
            ) {
                NeoMudApp(
                    authViewModel = authViewModel,
                    audioManager = audioManager
                )
            }
        }
    }
}

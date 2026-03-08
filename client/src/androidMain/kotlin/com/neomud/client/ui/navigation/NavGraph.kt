package com.neomud.client.ui.navigation

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.neomud.client.audio.AudioManager
import com.neomud.client.platform.LocalIsLandscape
import com.neomud.client.platform.LocalSetLayoutPreference
import com.neomud.client.viewmodel.AuthViewModel

/**
 * Android entry point — provides platform-specific CompositionLocals
 * (orientation, layout preference) and delegates to the shared NeoMudApp.
 */
@Composable
fun NeoMudNavGraph(authViewModel: AuthViewModel) {
    val context = LocalContext.current
    val audioManager = remember { AudioManager(context) }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val activity = context as? Activity
    val onSetLayoutPreference: (Boolean) -> Unit = remember(context, activity) {
        { landscape ->
            val prefs = context.getSharedPreferences("neomud_settings", Activity.MODE_PRIVATE)
            prefs.edit().putBoolean("landscape_layout", landscape).apply()
            activity?.requestedOrientation = if (landscape) {
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            } else {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        }
    }

    CompositionLocalProvider(
        LocalIsLandscape provides isLandscape,
        LocalSetLayoutPreference provides onSetLayoutPreference
    ) {
        NeoMudApp(
            authViewModel = authViewModel,
            audioManager = audioManager
        )
    }
}

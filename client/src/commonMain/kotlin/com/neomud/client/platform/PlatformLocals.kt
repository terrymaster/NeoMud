package com.neomud.client.platform

import androidx.compose.runtime.compositionLocalOf

/**
 * Whether the current window/device is in landscape orientation.
 * Provided by the platform-specific entry point (e.g., NavGraph on Android).
 */
val LocalIsLandscape = compositionLocalOf { false }

/**
 * Callback to set the user's layout preference (landscape vs portrait).
 * On Android this persists to SharedPreferences and changes the Activity orientation.
 * On Desktop this may resize the window or be a no-op.
 */
val LocalSetLayoutPreference = compositionLocalOf<(Boolean) -> Unit> { {} }

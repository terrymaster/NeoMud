package com.neomud.client.testutil

import androidx.test.core.app.ApplicationProvider
import coil3.PlatformContext

actual fun testPlatformContext(): PlatformContext =
    ApplicationProvider.getApplicationContext()

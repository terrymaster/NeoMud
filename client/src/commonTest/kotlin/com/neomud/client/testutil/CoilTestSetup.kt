package com.neomud.client.testutil

import coil3.ColorImage
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.annotation.DelicateCoilApi
import coil3.test.FakeImageLoaderEngine

/**
 * Installs a FakeImageLoaderEngine returning a default test image for all requests.
 * Call from @BeforeTest in tests that render Coil images.
 */
@OptIn(DelicateCoilApi::class)
fun installTestCoil() {
    val engine = FakeImageLoaderEngine.Builder()
        .default(ColorImage())
        .build()
    SingletonImageLoader.setUnsafe(
        ImageLoader.Builder(testPlatformContext())
            .components { add(engine) }
            .build()
    )
}

/**
 * Resets the singleton image loader. Call from @AfterTest.
 */
@OptIn(DelicateCoilApi::class)
fun resetTestCoil() {
    SingletonImageLoader.reset()
}

expect fun testPlatformContext(): PlatformContext

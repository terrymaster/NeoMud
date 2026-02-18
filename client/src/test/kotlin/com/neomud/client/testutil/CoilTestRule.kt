package com.neomud.client.testutil

import coil3.ColorImage
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.annotation.DelicateCoilApi
import coil3.test.FakeImageLoaderEngine
import coil3.test.default
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * JUnit rule that installs a FakeImageLoaderEngine returning a default
 * test image for all requests. Prevents network calls and produces
 * deterministic renders in Compose tests.
 */
@OptIn(DelicateCoilApi::class)
class CoilTestRule : TestWatcher() {

    override fun starting(description: Description?) {
        val engine = FakeImageLoaderEngine.Builder()
            .default(ColorImage(android.graphics.Color.GRAY))
            .build()
        SingletonImageLoader.setUnsafe(
            ImageLoader.Builder(androidx.test.core.app.ApplicationProvider.getApplicationContext())
                .components { add(engine) }
                .build()
        )
    }

    override fun finished(description: Description?) {
        SingletonImageLoader.reset()
    }
}

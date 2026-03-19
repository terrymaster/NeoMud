package com.neomud.client.testutil

/**
 * Platform-specific base class for Compose UI tests.
 * On Android, this carries @RunWith(RobolectricTestRunner::class) and @Config(sdk = [34])
 * so that runComposeUiTest can function properly.
 * On Desktop and iOS, this is an empty open class.
 */
expect abstract class ComposeTestBase()

package com.neomud.client.platform

import io.ktor.client.*

/**
 * Creates a platform-specific Ktor HttpClient with WebSocket support installed.
 * Android uses OkHttp engine; Desktop will use CIO or OkHttp.
 */
expect fun createPlatformHttpClient(): HttpClient

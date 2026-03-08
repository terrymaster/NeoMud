package com.neomud.client.platform

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.websocket.*

actual fun createPlatformHttpClient(): HttpClient = HttpClient(OkHttp) {
    install(WebSockets)
}

package com.neomud.client.platform

import io.ktor.client.*
import io.ktor.client.engine.js.*
import io.ktor.client.plugins.websocket.*

actual fun createPlatformHttpClient(): HttpClient = HttpClient(Js) {
    install(WebSockets)
}

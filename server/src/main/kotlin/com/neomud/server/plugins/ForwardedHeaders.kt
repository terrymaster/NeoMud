package com.neomud.server.plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.forwardedheaders.*

fun Application.configureForwardedHeaders() {
    install(XForwardedHeaders)
}

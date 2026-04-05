package com.neomud.client.platform

private fun isLocalhost(): Boolean = js("window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1'")

actual val serverConfig: ServerConfig = run {
    val local = isLocalhost()
    ServerConfig(
        defaultHost = if (local) "localhost" else "play.neomud.app",
        defaultPort = if (local) 8080 else 443,
        useTls = !local,
        showServerConfig = true, // Always show config during development
        platformApiUrl = if (local)
            "https://api.neomud.app/api/v1" // Use live API even in local dev for now
        else
            "https://api.neomud.app/api/v1"
    )
}

package com.neomud.client.platform

/** Server connection configuration, driven by build flavor (Android) or build config (Desktop/iOS). */
data class ServerConfig(
    val defaultHost: String,
    val defaultPort: Int,
    val useTls: Boolean,
    val showServerConfig: Boolean,
    val platformApiUrl: String,
    /** When true, skip the world browser and go directly to login (marketplace-launched). */
    val skipMarketplace: Boolean = false
)

expect val serverConfig: ServerConfig

/** Redirect back to the web marketplace. Only meaningful on WASM; no-op on native platforms. */
expect fun returnToMarketplace()

/** Backward-compatible alias — use [serverConfig] directly for new code. */
val defaultServerHost: String get() = serverConfig.defaultHost

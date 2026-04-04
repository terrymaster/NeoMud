package com.neomud.client.platform

/** Server connection configuration, driven by build flavor (Android) or build config (Desktop/iOS). */
data class ServerConfig(
    val defaultHost: String,
    val defaultPort: Int,
    val useTls: Boolean,
    val showServerConfig: Boolean,
    val platformApiUrl: String
)

expect val serverConfig: ServerConfig

/** Backward-compatible alias — use [serverConfig] directly for new code. */
val defaultServerHost: String get() = serverConfig.defaultHost

package com.neomud.client.network

/** Parsed server endpoint with host, port, TLS flag, and WebSocket path. */
data class ParsedEndpoint(
    val host: String,
    val port: Int,
    val useTls: Boolean,
    val path: String = "/game"
)

/**
 * Parse a WebSocket endpoint URL into host/port/TLS/path components.
 * Handles formats like:
 * - "ws://localhost:9000/game"
 * - "wss://play.neomud.app/worlds/abc123/game"
 * - "wss://play.neomud.app:443/worlds/abc123/game"
 *
 * Returns null if the endpoint cannot be parsed.
 */
fun parseServerEndpoint(endpoint: String): ParsedEndpoint? {
    val useTls = endpoint.startsWith("wss://")
    val withoutScheme = endpoint
        .removePrefix("wss://")
        .removePrefix("ws://")

    // Split on first "/" to get authority (host:port) and path
    val slashIndex = withoutScheme.indexOf('/')
    val authority = if (slashIndex >= 0) withoutScheme.substring(0, slashIndex) else withoutScheme
    val path = if (slashIndex >= 0) withoutScheme.substring(slashIndex) else "/game"
    val parts = authority.split(":")

    val host = parts.getOrNull(0)?.takeIf { it.isNotEmpty() } ?: return null
    val port = parts.getOrNull(1)?.toIntOrNull()
        ?: if (useTls) 443 else 80

    return ParsedEndpoint(host, port, useTls, path.ifEmpty { "/game" })
}

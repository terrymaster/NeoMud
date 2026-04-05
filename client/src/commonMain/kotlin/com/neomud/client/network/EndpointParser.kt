package com.neomud.client.network

/** Parsed server endpoint with host, port, and TLS flag. */
data class ParsedEndpoint(val host: String, val port: Int, val useTls: Boolean)

/**
 * Parse a WebSocket endpoint URL into host/port/TLS components.
 * Handles formats like:
 * - "ws://localhost:9000/game"
 * - "wss://play.neomud.app/worlds/abc123"
 * - "wss://play.neomud.app:443/worlds/abc123"
 *
 * Returns null if the endpoint cannot be parsed.
 */
fun parseServerEndpoint(endpoint: String): ParsedEndpoint? {
    val useTls = endpoint.startsWith("wss://")
    val withoutScheme = endpoint
        .removePrefix("wss://")
        .removePrefix("ws://")

    // Split on first "/" to get authority (host:port)
    val authority = withoutScheme.substringBefore("/")
    val parts = authority.split(":")

    val host = parts.getOrNull(0)?.takeIf { it.isNotEmpty() } ?: return null
    val port = parts.getOrNull(1)?.toIntOrNull()
        ?: if (useTls) 443 else 80

    return ParsedEndpoint(host, port, useTls)
}

package com.neomud.client.network

import com.neomud.client.platform.ServerConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ServerConfigTest {

    @Test
    fun skipMarketplaceDefaultsToFalse() {
        val config = ServerConfig(
            defaultHost = "localhost",
            defaultPort = 8080,
            useTls = false,
            showServerConfig = true,
            platformApiUrl = "https://api.neomud.app/api/v1"
        )
        assertFalse(config.skipMarketplace)
    }

    @Test
    fun skipMarketplaceCanBeSetTrue() {
        val config = ServerConfig(
            defaultHost = "play.neomud.app",
            defaultPort = 443,
            useTls = true,
            showServerConfig = false,
            platformApiUrl = "https://api.neomud.app/api/v1",
            skipMarketplace = true
        )
        assertTrue(config.skipMarketplace)
    }

    @Test
    fun marketplaceLaunchedConfigHidesServerConfig() {
        // When launched from marketplace, server config UI should be hidden
        val config = ServerConfig(
            defaultHost = "play.neomud.app",
            defaultPort = 443,
            useTls = true,
            showServerConfig = false,
            platformApiUrl = "https://api.neomud.app/api/v1",
            skipMarketplace = true
        )
        assertFalse(config.showServerConfig)
        assertTrue(config.useTls)
        assertEquals(443, config.defaultPort)
    }

    @Test
    fun standaloneModeShowsServerConfig() {
        val config = ServerConfig(
            defaultHost = "localhost",
            defaultPort = 8080,
            useTls = false,
            showServerConfig = true,
            platformApiUrl = "https://api.neomud.app/api/v1"
        )
        assertTrue(config.showServerConfig)
        assertFalse(config.skipMarketplace)
    }
}

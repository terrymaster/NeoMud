package com.neomud.client.network

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlatformApiClientTest {

    @Test
    fun initialStateIsUnauthenticated() {
        val client = PlatformApiClient("https://api.example.com/api/v1")
        assertFalse(client.isAuthenticated)
        assertNull(client.accessToken)
        assertNull(client.refreshToken)
    }

    @Test
    fun restoreTokensSetsAuthState() {
        val client = PlatformApiClient("https://api.example.com/api/v1")
        client.restoreTokens("access-123", "refresh-456")
        assertTrue(client.isAuthenticated)
        assertEquals("access-123", client.accessToken)
        assertEquals("refresh-456", client.refreshToken)
    }

    @Test
    fun isAuthenticatedReflectsTokenPresence() {
        val client = PlatformApiClient("https://api.example.com/api/v1")
        assertFalse(client.isAuthenticated)
        client.restoreTokens("token", "refresh")
        assertTrue(client.isAuthenticated)
    }
}

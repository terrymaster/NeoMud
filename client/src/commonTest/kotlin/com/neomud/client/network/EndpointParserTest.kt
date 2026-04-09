package com.neomud.client.network

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class EndpointParserTest {

    @Test
    fun parsesWssEndpointWithPort() {
        val result = parseServerEndpoint("wss://play.neomud.app:443/worlds/abc123")
        assertEquals("play.neomud.app", result?.host)
        assertEquals(443, result?.port)
        assertTrue(result!!.useTls)
    }

    @Test
    fun parsesWssEndpointWithoutPort() {
        val result = parseServerEndpoint("wss://play.neomud.app/worlds/abc123")
        assertEquals("play.neomud.app", result?.host)
        assertEquals(443, result?.port) // defaults to 443 for wss
        assertTrue(result!!.useTls)
    }

    @Test
    fun parsesWsEndpointWithPort() {
        val result = parseServerEndpoint("ws://localhost:9000/game")
        assertEquals("localhost", result?.host)
        assertEquals(9000, result?.port)
        assertFalse(result!!.useTls)
    }

    @Test
    fun parsesWsEndpointWithoutPort() {
        val result = parseServerEndpoint("ws://localhost/game")
        assertEquals("localhost", result?.host)
        assertEquals(80, result?.port) // defaults to 80 for ws
        assertFalse(result!!.useTls)
    }

    @Test
    fun parsesEndpointWithNoPath() {
        val result = parseServerEndpoint("wss://play.neomud.app")
        assertEquals("play.neomud.app", result?.host)
        assertEquals(443, result?.port)
        assertTrue(result!!.useTls)
    }

    @Test
    fun parsesIpAddress() {
        val result = parseServerEndpoint("ws://10.0.2.2:8080/game")
        assertEquals("10.0.2.2", result?.host)
        assertEquals(8080, result?.port)
        assertFalse(result!!.useTls)
    }

    @Test
    fun returnsNullForEmptyHost() {
        assertNull(parseServerEndpoint("ws:///game"))
    }

    @Test
    fun returnsNullForEmptyString() {
        assertNull(parseServerEndpoint(""))
    }

    @Test
    fun parsesProductionWorldEndpoint() {
        // Real production format from orchestrator
        val result = parseServerEndpoint("wss://api.neomud.app/worlds/clxyz123abc")
        assertEquals("api.neomud.app", result?.host)
        assertEquals(443, result?.port)
        assertTrue(result!!.useTls)
    }

    @Test
    fun parsesDevWorldEndpoint() {
        // Real dev format from orchestrator
        val result = parseServerEndpoint("ws://localhost:9001/game")
        assertEquals("localhost", result?.host)
        assertEquals(9001, result?.port)
        assertFalse(result!!.useTls)
    }

    // ─── Path parsing tests ─────────────────────────────

    @Test
    fun parsesPathFromWorldEndpoint() {
        val result = parseServerEndpoint("wss://stage.neomud.app/worlds/cmnqrhljj000016n1f15es4rl/game")
        assertEquals("stage.neomud.app", result?.host)
        assertEquals(443, result?.port)
        assertTrue(result!!.useTls)
        assertEquals("/worlds/cmnqrhljj000016n1f15es4rl/game", result.path)
    }

    @Test
    fun parsesSimpleGamePath() {
        val result = parseServerEndpoint("ws://localhost:9000/game")
        assertEquals("/game", result?.path)
    }

    @Test
    fun defaultsPathToGameWhenNoPath() {
        val result = parseServerEndpoint("wss://play.neomud.app")
        assertEquals("/game", result?.path)
    }

    @Test
    fun parsesPathWithPortAndWorldId() {
        val result = parseServerEndpoint("wss://stage.neomud.app:443/worlds/abc123def/game")
        assertEquals("stage.neomud.app", result?.host)
        assertEquals(443, result?.port)
        assertEquals("/worlds/abc123def/game", result?.path)
    }

    @Test
    fun devEndpointKeepsSimplePath() {
        // Dev mode uses direct port, no per-world path prefix
        val result = parseServerEndpoint("ws://localhost:9501/game")
        assertEquals("localhost", result?.host)
        assertEquals(9501, result?.port)
        assertEquals("/game", result?.path)
    }
}

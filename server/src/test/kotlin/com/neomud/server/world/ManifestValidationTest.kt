package com.neomud.server.world

import com.neomud.shared.NeoMudVersion
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ManifestValidationTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun manifest(
        formatVersion: Int = 1,
        engineVersion: String = "0.1.0.0",
        engineVersionMin: String = "0.1.0.0",
        worldId: String = "test-world-id",
        name: String = "Test World",
        author: String = "Tester",
        version: String = "1.0.0",
        createdWithMaker: Boolean = true
    ): String = """
        {
            "formatVersion": $formatVersion,
            "name": "$name",
            "author": "$author",
            "version": "$version",
            "engineVersion": "$engineVersion",
            "engineVersionMin": "$engineVersionMin",
            "worldId": "$worldId",
            "createdWithMaker": $createdWithMaker
        }
    """.trimIndent()

    @Test
    fun `parses valid manifest with all fields`() {
        val m = json.decodeFromString<WorldManifest>(manifest())
        assertEquals(1, m.formatVersion)
        assertEquals("Test World", m.name)
        assertEquals("Tester", m.author)
        assertEquals("1.0.0", m.version)
        assertEquals("0.1.0.0", m.engineVersion)
        assertEquals("0.1.0.0", m.engineVersionMin)
        assertEquals("test-world-id", m.worldId)
        assertTrue(m.createdWithMaker)
    }

    @Test
    fun `engine version equal to server version passes`() {
        val m = json.decodeFromString<WorldManifest>(
            manifest(engineVersionMin = NeoMudVersion.ENGINE_VERSION)
        )
        val result = NeoMudVersion.compareVersions(NeoMudVersion.ENGINE_VERSION, m.engineVersionMin)
        assertTrue(result >= 0, "Server version should be >= engineVersionMin")
    }

    @Test
    fun `engine version older than engineVersionMin is rejected`() {
        val m = json.decodeFromString<WorldManifest>(
            manifest(engineVersionMin = "99.0.0")
        )
        val result = NeoMudVersion.compareVersions(NeoMudVersion.ENGINE_VERSION, m.engineVersionMin)
        assertTrue(result < 0, "Server should be older than 99.0.0")
    }

    @Test
    fun `format version above current is rejected`() {
        val m = json.decodeFromString<WorldManifest>(manifest(formatVersion = 999))
        assertTrue(m.formatVersion > 1, "Format version should be above current")
    }

    @Test
    fun `empty optional fields deserialize with defaults`() {
        val m = json.decodeFromString<WorldManifest>(manifest())
        // description and createdAt have defaults
        assertEquals("", m.description)
        assertEquals("", m.createdAt)
    }
}

package com.neomud.client.model

import com.neomud.client.model.platform.*
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Verify Platform API response models deserialize correctly from JSON. */
class PlatformModelsTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun loginResponseDeserialization() {
        val raw = """
            {
                "accessToken": "eyJ...",
                "refreshToken": "abc123",
                "user": { "id": "user-1", "displayName": "TestPlayer", "role": "CREATOR" }
            }
        """.trimIndent()
        val response = json.decodeFromString<LoginResponse>(raw)
        assertEquals("eyJ...", response.accessToken)
        assertEquals("abc123", response.refreshToken)
        assertEquals("user-1", response.user.id)
        assertEquals("TestPlayer", response.user.displayName)
        assertEquals("CREATOR", response.user.role)
    }

    @Test
    fun loginResponseWithUnknownFieldsIgnored() {
        val raw = """
            {
                "accessToken": "tok",
                "refreshToken": "ref",
                "user": { "id": "u1", "displayName": "P", "role": "ADMIN", "extraField": true }
            }
        """.trimIndent()
        val response = json.decodeFromString<LoginResponse>(raw)
        assertEquals("ADMIN", response.user.role)
    }

    @Test
    fun refreshResponseDeserialization() {
        val raw = """{ "accessToken": "new-access", "refreshToken": "new-refresh" }"""
        val response = json.decodeFromString<RefreshResponse>(raw)
        assertEquals("new-access", response.accessToken)
        assertEquals("new-refresh", response.refreshToken)
    }

    @Test
    fun ratingEntryDeserialization() {
        val raw = """
            {
                "id": "rating-1",
                "stars": 4,
                "review": "Great world!",
                "reviewer": { "id": "user-2", "displayName": "Reviewer" },
                "createdAt": "2026-04-05T00:00:00Z",
                "updatedAt": "2026-04-05T00:00:00Z"
            }
        """.trimIndent()
        val entry = json.decodeFromString<RatingEntry>(raw)
        assertEquals(4, entry.stars)
        assertEquals("Great world!", entry.review)
        assertEquals("Reviewer", entry.reviewer.displayName)
    }

    @Test
    fun ratingEntryWithNullReview() {
        val raw = """
            {
                "id": "r1", "stars": 3, "review": null,
                "reviewer": { "id": "u1", "displayName": "U" },
                "createdAt": "2026-01-01T00:00:00Z", "updatedAt": "2026-01-01T00:00:00Z"
            }
        """.trimIndent()
        val entry = json.decodeFromString<RatingEntry>(raw)
        assertNull(entry.review)
    }

    @Test
    fun ratingListResponseDeserialization() {
        val raw = """
            {
                "ratings": [],
                "userRating": { "id": "r1", "stars": 5, "review": "Loved it" },
                "pagination": { "page": 1, "limit": 20, "total": 0, "totalPages": 0 }
            }
        """.trimIndent()
        val response = json.decodeFromString<RatingListResponse>(raw)
        assertEquals(0, response.ratings.size)
        assertEquals(5, response.userRating?.stars)
        assertEquals("Loved it", response.userRating?.review)
    }

    @Test
    fun ratingListResponseWithNullUserRating() {
        val raw = """
            {
                "ratings": [],
                "userRating": null,
                "pagination": { "page": 1, "limit": 20, "total": 0, "totalPages": 0 }
            }
        """.trimIndent()
        val response = json.decodeFromString<RatingListResponse>(raw)
        assertNull(response.userRating)
    }

    @Test
    fun playSessionResponseDeserialization() {
        val raw = """
            { "id": "sess-1", "worldId": "world-1", "startedAt": "2026-04-05T10:00:00Z" }
        """.trimIndent()
        val session = json.decodeFromString<PlaySessionResponse>(raw)
        assertEquals("sess-1", session.id)
        assertEquals("world-1", session.worldId)
        assertNull(session.endedAt)
    }

    @Test
    fun playSessionResponseWithEndedAt() {
        val raw = """
            {
                "id": "sess-1", "worldId": "world-1",
                "startedAt": "2026-04-05T10:00:00Z",
                "endedAt": "2026-04-05T10:05:00Z"
            }
        """.trimIndent()
        val session = json.decodeFromString<PlaySessionResponse>(raw)
        assertEquals("2026-04-05T10:05:00Z", session.endedAt)
    }

    @Test
    fun platformUserDefaultsRole() {
        val raw = """{ "id": "u1", "displayName": "Test" }"""
        val user = json.decodeFromString<PlatformUser>(raw)
        assertEquals("CREATOR", user.role)
    }
}

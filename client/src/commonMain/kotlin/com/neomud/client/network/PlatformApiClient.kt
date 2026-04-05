package com.neomud.client.network

import com.neomud.client.model.platform.CreateRatingResponse
import com.neomud.client.model.platform.LoginResponse
import com.neomud.client.model.platform.PlaySessionResponse
import com.neomud.client.model.platform.RatingListResponse
import com.neomud.client.model.platform.RefreshResponse
import com.neomud.client.model.platform.WorldDetail
import com.neomud.client.model.platform.WorldListResponse
import com.neomud.client.platform.PlatformLogger
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

/**
 * REST client for the NeoMud Platform API (marketplace, auth, ratings, play sessions).
 * Separate from the WebSocket GameConnection used for in-game communication.
 */
class PlatformApiClient(
    private val baseUrl: String
) {
    private val json = Json { ignoreUnknownKeys = true }

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(json)
        }
    }

    /** Current access token for authenticated requests. */
    var accessToken: String? = null
        private set

    /** Current refresh token for token renewal. */
    var refreshToken: String? = null
        private set

    /** Whether the client has stored auth tokens. */
    val isAuthenticated: Boolean get() = accessToken != null

    // ─── Public API (no auth) ───

    /** Fetch paginated list of published worlds. */
    suspend fun getWorlds(page: Int = 1, limit: Int = 20, search: String? = null): WorldListResponse {
        val response = client.get("$baseUrl/worlds") {
            parameter("page", page)
            parameter("limit", limit)
            if (!search.isNullOrBlank()) {
                parameter("search", search)
            }
        }
        checkResponse(response)
        return response.body()
    }

    /** Fetch world detail by slug. */
    suspend fun getWorldDetail(slug: String): WorldDetail {
        val response = client.get("$baseUrl/worlds/$slug")
        checkResponse(response)
        return response.body()
    }

    // ─── Auth ───

    /** Register a new Platform account. Returns true on success (201). */
    suspend fun register(email: String, password: String, displayName: String): Boolean {
        val response = client.post("$baseUrl/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("email" to email, "password" to password, "displayName" to displayName))
        }
        return response.status.value == 201
    }

    /** Log in and store tokens. Returns the user info on success. */
    suspend fun login(email: String, password: String): LoginResponse {
        val response = client.post("$baseUrl/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("email" to email, "password" to password))
        }
        checkResponse(response)
        val loginResponse: LoginResponse = response.body()
        accessToken = loginResponse.accessToken
        refreshToken = loginResponse.refreshToken
        return loginResponse
    }

    /** Refresh the access token using the stored refresh token. Returns true on success. */
    suspend fun refreshAccessToken(): Boolean {
        val rt = refreshToken ?: return false
        return try {
            val response = client.post("$baseUrl/auth/refresh") {
                contentType(ContentType.Application.Json)
                setBody(mapOf("refreshToken" to rt))
            }
            if (!response.status.isSuccess()) return false
            val refreshResponse: RefreshResponse = response.body()
            accessToken = refreshResponse.accessToken
            refreshToken = refreshResponse.refreshToken
            true
        } catch (e: Exception) {
            PlatformLogger.w("PlatformApiClient", "Token refresh failed: ${e.message}")
            false
        }
    }

    /** Log out and clear stored tokens. */
    suspend fun logout() {
        val rt = refreshToken
        if (rt != null) {
            try {
                client.post("$baseUrl/auth/logout") {
                    contentType(ContentType.Application.Json)
                    setBody(mapOf("refreshToken" to rt))
                }
            } catch (_: Exception) {
                // Best-effort
            }
        }
        accessToken = null
        refreshToken = null
    }

    /** Restore tokens from persistent storage (called on app startup). */
    fun restoreTokens(access: String, refresh: String) {
        accessToken = access
        refreshToken = refresh
    }

    // ─── Ratings (auth required for write, optional for read) ───

    /** Fetch ratings for a world (public). */
    suspend fun getRatings(worldId: String, page: Int = 1): RatingListResponse {
        val response = client.get("$baseUrl/worlds/$worldId/ratings") {
            parameter("page", page)
            applyAuth()
        }
        checkResponse(response)
        return response.body()
    }

    /** Submit or update a rating for a world (auth required). */
    suspend fun submitRating(worldId: String, stars: Int, review: String? = null): CreateRatingResponse {
        val response = client.post("$baseUrl/worlds/$worldId/ratings") {
            contentType(ContentType.Application.Json)
            setBody(buildMap {
                put("stars", stars.toString())
                put("review", review.orEmpty())
            })
            applyAuth()
        }
        checkResponse(response)
        return response.body()
    }

    /** Delete own rating for a world (auth required). */
    suspend fun deleteRating(worldId: String) {
        val response = client.delete("$baseUrl/worlds/$worldId/ratings") {
            applyAuth()
        }
        checkResponse(response)
    }

    // ─── Play Sessions (auth required) ───

    /** Start a play session for a world. Returns the session ID. */
    suspend fun startPlaySession(worldId: String): PlaySessionResponse {
        val response = client.post("$baseUrl/play-sessions") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("worldId" to worldId))
            applyAuth()
        }
        checkResponse(response)
        return response.body()
    }

    /** End a play session. */
    suspend fun endPlaySession(sessionId: String): PlaySessionResponse {
        val response = client.patch("$baseUrl/play-sessions/$sessionId") {
            applyAuth()
        }
        checkResponse(response)
        return response.body()
    }

    // ─── Helpers ───

    private fun HttpRequestBuilder.applyAuth() {
        val token = accessToken
        if (token != null) {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
    }

    private suspend fun checkResponse(response: HttpResponse) {
        if (!response.status.isSuccess()) {
            val body = try { response.bodyAsText() } catch (_: Exception) { "" }
            PlatformLogger.w("PlatformApiClient", "API error ${response.status}: $body")
            throw PlatformApiException(response.status.value, body)
        }
    }
}

class PlatformApiException(val statusCode: Int, val body: String) :
    Exception("Platform API error $statusCode")

package com.neomud.client.network

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
 * REST client for the NeoMud Platform API (marketplace, auth, etc.).
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

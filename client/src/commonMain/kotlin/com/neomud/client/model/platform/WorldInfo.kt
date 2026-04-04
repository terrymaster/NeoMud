package com.neomud.client.model.platform

import kotlinx.serialization.Serializable

/** Summary of a world in the marketplace listing. */
@Serializable
data class WorldSummary(
    val id: String,
    val name: String,
    val slug: String,
    val description: String,
    val creator: CreatorInfo,
    val latestVersion: VersionSummary? = null,
    val createdAt: String,
    val updatedAt: String
)

/** Creator info included with world listings. */
@Serializable
data class CreatorInfo(
    val id: String,
    val displayName: String
)

/** Brief version info for listings. */
@Serializable
data class VersionSummary(
    val version: String,
    val createdAt: String,
    val engineVersion: String = ""
)

/** Full world detail including version history. */
@Serializable
data class WorldDetail(
    val id: String,
    val name: String,
    val slug: String,
    val description: String,
    val status: String,
    val serverEndpoint: String? = null,
    val creator: CreatorInfo,
    val versions: List<VersionDetail>,
    val createdAt: String,
    val updatedAt: String
)

/** Version detail with changelog. */
@Serializable
data class VersionDetail(
    val id: String,
    val version: String,
    val changelog: String = "",
    val formatVersion: Int,
    val engineVersion: String,
    val createdAt: String
)

/** Paginated response wrapper. */
@Serializable
data class WorldListResponse(
    val worlds: List<WorldSummary>,
    val pagination: PaginationInfo
)

@Serializable
data class PaginationInfo(
    val page: Int,
    val limit: Int,
    val total: Int,
    val totalPages: Int
)

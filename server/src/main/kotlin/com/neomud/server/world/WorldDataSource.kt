package com.neomud.server.world

import kotlinx.serialization.Serializable
import java.io.InputStream

interface WorldDataSource {
    fun readText(path: String): String?
    fun readBytes(path: String): ByteArray?
    fun list(prefix: String, suffix: String = ""): List<String>
    fun openStream(path: String): InputStream?
}

@Serializable
data class WorldManifest(
    val formatVersion: Int,
    val name: String,
    val author: String = "Unknown",
    val version: String = "0.0.0",
    val description: String = "",
    val createdAt: String = ""
)

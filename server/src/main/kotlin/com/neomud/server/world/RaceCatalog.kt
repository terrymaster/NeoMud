package com.neomud.server.world

import com.neomud.shared.model.RaceDef
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

@Serializable
data class RaceCatalogData(val races: List<RaceDef>)

class RaceCatalog(races: List<RaceDef>) {
    private val raceMap: Map<String, RaceDef> = races.associateBy { it.id }

    val raceCount: Int get() = raceMap.size

    fun getRace(id: String): RaceDef? = raceMap[id]

    fun getAllRaces(): List<RaceDef> = raceMap.values.toList()

    companion object {
        private val logger = LoggerFactory.getLogger(RaceCatalog::class.java)
        private val json = Json { ignoreUnknownKeys = true }

        fun load(): RaceCatalog {
            val resource = RaceCatalog::class.java.classLoader.getResourceAsStream("world/races.json")
                ?: error("races.json not found in resources")
            val content = resource.bufferedReader().use { it.readText() }
            val data = json.decodeFromString<RaceCatalogData>(content)
            logger.info("Loaded ${data.races.size} races")
            return RaceCatalog(data.races)
        }
    }
}

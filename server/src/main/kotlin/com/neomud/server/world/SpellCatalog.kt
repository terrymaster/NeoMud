package com.neomud.server.world

import com.neomud.shared.model.SpellDef
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

@Serializable
data class SpellCatalogData(val spells: List<SpellDef>)

class SpellCatalog(spells: List<SpellDef>) {
    private val spellMap: Map<String, SpellDef> = spells.associateBy { it.id }

    val spellCount: Int get() = spellMap.size

    fun getSpell(id: String): SpellDef? = spellMap[id]

    fun getAllSpells(): List<SpellDef> = spellMap.values.toList()

    fun getSpellsForSchool(school: String): List<SpellDef> =
        spellMap.values.filter { it.school == school }

    companion object {
        private val logger = LoggerFactory.getLogger(SpellCatalog::class.java)
        private val json = Json { ignoreUnknownKeys = true }

        fun load(): SpellCatalog {
            val resource = SpellCatalog::class.java.classLoader.getResourceAsStream("world/spells.json")
                ?: error("spells.json not found in resources")
            val content = resource.bufferedReader().use { it.readText() }
            val data = json.decodeFromString<SpellCatalogData>(content)
            logger.info("Loaded ${data.spells.size} spells")
            return SpellCatalog(data.spells)
        }
    }
}

package com.neomud.server.world

import com.neomud.shared.model.SkillDef
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

@Serializable
data class SkillCatalogData(val skills: List<SkillDef>)

class SkillCatalog(skills: List<SkillDef>) {
    private val skillMap: Map<String, SkillDef> = skills.associateBy { it.id }

    val skillCount: Int get() = skillMap.size

    fun getSkill(id: String): SkillDef? = skillMap[id]

    fun getAllSkills(): List<SkillDef> = skillMap.values.toList()

    fun getSkillsForClass(classId: String): List<SkillDef> =
        skillMap.values.filter { it.classRestrictions.isEmpty() || classId in it.classRestrictions }

    companion object {
        private val logger = LoggerFactory.getLogger(SkillCatalog::class.java)
        private val json = Json { ignoreUnknownKeys = true }

        fun load(): SkillCatalog {
            val resource = SkillCatalog::class.java.classLoader.getResourceAsStream("world/skills.json")
                ?: error("skills.json not found in resources")
            val content = resource.bufferedReader().use { it.readText() }
            val data = json.decodeFromString<SkillCatalogData>(content)
            logger.info("Loaded ${data.skills.size} skills")
            return SkillCatalog(data.skills)
        }
    }
}

package com.neomud.server.world

import com.neomud.shared.model.CharacterClassDef
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

@Serializable
data class ClassCatalogData(val classes: List<CharacterClassDef>)

class ClassCatalog(classes: List<CharacterClassDef>) {
    private val classMap: Map<String, CharacterClassDef> = classes.associateBy { it.id }

    val classCount: Int get() = classMap.size

    fun getClass(id: String): CharacterClassDef? = classMap[id]

    fun getAllClasses(): List<CharacterClassDef> = classMap.values.toList()

    companion object {
        private val logger = LoggerFactory.getLogger(ClassCatalog::class.java)
        private val json = Json { ignoreUnknownKeys = true }

        fun load(source: WorldDataSource): ClassCatalog {
            val content = source.readText("world/classes.json")
                ?: error("classes.json not found")
            val data = json.decodeFromString<ClassCatalogData>(content)
            logger.info("Loaded ${data.classes.size} character classes")
            return ClassCatalog(data.classes)
        }

        fun load(): ClassCatalog = load(ClasspathDataSource())
    }
}

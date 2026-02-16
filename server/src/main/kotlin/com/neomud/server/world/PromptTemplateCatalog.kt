package com.neomud.server.world

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

@Serializable
data class PromptTemplate(
    val entityType: String,
    val entityId: String,
    val prompt: String,
    val style: String = "",
    val negativePrompt: String = "",
    val width: Int = 1024,
    val height: Int = 576
)

@Serializable
data class PromptTemplateData(val templates: List<PromptTemplate>)

class PromptTemplateCatalog(private val templates: Map<String, PromptTemplate>) {

    val templateCount: Int get() = templates.size

    fun getTemplate(key: String): PromptTemplate? = templates[key]

    fun getTemplatesByType(entityType: String): List<PromptTemplate> =
        templates.values.filter { it.entityType == entityType }

    companion object {
        private val logger = LoggerFactory.getLogger(PromptTemplateCatalog::class.java)
        private val json = Json { ignoreUnknownKeys = true }

        fun load(source: WorldDataSource): PromptTemplateCatalog {
            val content = source.readText("world/prompt_templates.json")
                ?: error("prompt_templates.json not found")
            val data = json.decodeFromString<PromptTemplateData>(content)
            val map = data.templates.associateBy { "${it.entityType}:${it.entityId}" }
            logger.info("Loaded ${map.size} prompt templates")
            return PromptTemplateCatalog(map)
        }

        fun load(): PromptTemplateCatalog = load(ClasspathDataSource())
    }
}

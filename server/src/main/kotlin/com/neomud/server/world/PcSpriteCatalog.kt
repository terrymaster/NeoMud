package com.neomud.server.world

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

@Serializable
data class PcSpriteData(val sprites: List<PcSpriteDef>)

@Serializable
data class PcSpriteDef(
    val id: String,
    val race: String,
    val gender: String,
    val characterClass: String,
    val imagePrompt: String = "",
    val imageStyle: String = "",
    val imageNegativePrompt: String = ""
)

class PcSpriteCatalog(sprites: List<PcSpriteDef>) {
    private val spriteMap: Map<String, PcSpriteDef> = sprites.associateBy { it.id }

    val spriteCount: Int get() = spriteMap.size

    fun getSprite(id: String): PcSpriteDef? = spriteMap[id]

    fun getSpriteFor(race: String, gender: String, characterClass: String): PcSpriteDef? {
        val id = "${race.lowercase()}_${gender.lowercase()}_${characterClass.lowercase()}"
        return spriteMap[id]
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PcSpriteCatalog::class.java)
        private val json = Json { ignoreUnknownKeys = true }

        fun load(source: WorldDataSource): PcSpriteCatalog {
            val content = source.readText("world/pc_sprites.json")
            if (content == null) {
                logger.info("pc_sprites.json not found, using empty sprite catalog")
                return PcSpriteCatalog(emptyList())
            }
            val data = json.decodeFromString<PcSpriteData>(content)
            logger.info("Loaded ${data.sprites.size} PC sprite definitions")
            return PcSpriteCatalog(data.sprites)
        }
    }
}

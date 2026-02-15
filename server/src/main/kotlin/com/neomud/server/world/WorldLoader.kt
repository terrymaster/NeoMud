package com.neomud.server.world

import com.neomud.shared.model.Npc
import com.neomud.shared.model.Room
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("WorldLoader")

object WorldLoader {
    private val json = Json { ignoreUnknownKeys = true }

    data class LoadResult(
        val worldGraph: WorldGraph,
        val npcDataList: List<Pair<NpcData, String>>,
        val classCatalog: ClassCatalog,
        val itemCatalog: ItemCatalog,
        val lootTableCatalog: LootTableCatalog,
        val promptTemplateCatalog: PromptTemplateCatalog,
        val skillCatalog: SkillCatalog,
        val raceCatalog: RaceCatalog,
        val spellCatalog: SpellCatalog,
        val zoneSpawnConfigs: Map<String, SpawnConfig>
    )

    fun load(): LoadResult {
        val classCatalog = ClassCatalog.load()
        val itemCatalog = ItemCatalog.load()
        val lootTableCatalog = LootTableCatalog.load()
        val promptTemplateCatalog = PromptTemplateCatalog.load()
        val skillCatalog = SkillCatalog.load()
        val raceCatalog = RaceCatalog.load()
        val spellCatalog = SpellCatalog.load()
        val worldGraph = WorldGraph()
        val allNpcData = mutableListOf<Pair<NpcData, String>>()
        val zoneSpawnConfigs = mutableMapOf<String, SpawnConfig>()
        val zoneFiles = listOf("world/town.zone.json", "world/forest.zone.json")

        for (file in zoneFiles) {
            val resource = WorldLoader::class.java.classLoader.getResourceAsStream(file)
            if (resource == null) {
                logger.warn("Zone file not found: $file")
                continue
            }

            val content = resource.bufferedReader().use { it.readText() }
            val zone = json.decodeFromString<ZoneData>(content)

            logger.info("Loading zone: ${zone.name} (${zone.rooms.size} rooms, ${zone.npcs.size} NPCs)")

            for (roomData in zone.rooms) {
                val room = Room(
                    id = roomData.id,
                    name = roomData.name,
                    description = roomData.description,
                    exits = roomData.exits,
                    zoneId = zone.id,
                    x = roomData.x,
                    y = roomData.y,
                    backgroundImage = roomData.backgroundImage
                )
                worldGraph.addRoom(room)
            }

            allNpcData.addAll(zone.npcs.map { it to zone.id })
            zoneSpawnConfigs[zone.id] = zone.spawnConfig
        }

        worldGraph.setDefaultSpawn("town:square")
        logger.info("World loaded: ${worldGraph.roomCount} rooms, ${allNpcData.size} NPCs")

        return LoadResult(worldGraph, allNpcData, classCatalog, itemCatalog, lootTableCatalog, promptTemplateCatalog, skillCatalog, raceCatalog, spellCatalog, zoneSpawnConfigs)
    }
}

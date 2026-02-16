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
        var dataDefinedSpawn: String? = null

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
                    backgroundImage = roomData.backgroundImage,
                    healPerTick = roomData.healPerTick,
                    bgm = roomData.bgm.ifEmpty { zone.bgm },
                    departSound = roomData.departSound
                )
                worldGraph.addRoom(room)
            }

            allNpcData.addAll(zone.npcs.map { it to zone.id })
            zoneSpawnConfigs[zone.id] = zone.spawnConfig

            if (dataDefinedSpawn == null && zone.spawnRoom != null) {
                dataDefinedSpawn = zone.spawnRoom
            }
        }

        worldGraph.setDefaultSpawn(dataDefinedSpawn ?: "town:square")
        logger.info("World loaded: ${worldGraph.roomCount} rooms, ${allNpcData.size} NPCs")

        // Item data validation
        val validSlots = setOf("weapon", "head", "chest", "legs", "feet", "shield", "hands")
        for (item in itemCatalog.getAllItems()) {
            if (item.type == "weapon") {
                if (item.slot.isBlank()) logger.warn("Weapon '${item.id}' missing slot (should be \"weapon\")")
                if (item.damageBonus == 0 && item.damageRange == 0) logger.warn("Weapon '${item.id}' has zero damageBonus and zero damageRange")
            }
            if (item.type == "armor" && item.armorValue == 0) {
                logger.warn("Armor '${item.id}' has zero armorValue")
            }
            if (item.type == "consumable" && item.useEffect.isBlank()) {
                logger.warn("Consumable '${item.id}' missing useEffect")
            }
            if (item.stackable && item.maxStack <= 1) {
                logger.warn("Item '${item.id}' is stackable but maxStack=${item.maxStack}")
            }
            if (item.slot.isNotBlank() && item.slot !in validSlots) {
                logger.warn("Item '${item.id}' has unknown slot '${item.slot}'")
            }
        }

        // NPC data validation
        for ((npcData, _) in allNpcData) {
            if (npcData.hostile) {
                if (npcData.maxHp == 0) logger.warn("Hostile NPC '${npcData.id}' has maxHp=0")
                if (npcData.damage == 0) logger.warn("Hostile NPC '${npcData.id}' has damage=0")
                if (npcData.xpReward == 0L) logger.warn("Hostile NPC '${npcData.id}' has xpReward=0")
                if (lootTableCatalog.getLootTable(npcData.id).isEmpty()) {
                    logger.warn("Hostile NPC '${npcData.id}' has no loot table entry")
                }
            }
            if (npcData.vendorItems.isNotEmpty()) {
                for (itemId in npcData.vendorItems) {
                    if (itemCatalog.getItem(itemId) == null) {
                        logger.warn("Vendor NPC '${npcData.id}' references unknown item '$itemId'")
                    }
                }
            }
            if (npcData.behaviorType == "vendor" && npcData.vendorItems.isEmpty()) {
                logger.warn("Vendor NPC '${npcData.id}' has empty vendorItems")
            }
            if (npcData.behaviorType == "patrol" && npcData.patrolRoute.isEmpty()) {
                logger.warn("Patrol NPC '${npcData.id}' has empty patrolRoute")
            }
            if (worldGraph.getRoom(npcData.startRoomId) == null) {
                logger.warn("NPC '${npcData.id}' startRoomId '${npcData.startRoomId}' not found in loaded rooms")
            }
        }

        // Room data validation
        for (room in worldGraph.getAllRooms()) {
            if (room.exits.isEmpty()) logger.warn("Room '${room.id}' has zero exits (isolated)")
            if (room.backgroundImage.isBlank()) logger.warn("Room '${room.id}' missing backgroundImage")
            for ((direction, targetId) in room.exits) {
                if (worldGraph.getRoom(targetId) == null) {
                    logger.warn("Room '${room.id}' exit $direction points to unknown room '$targetId'")
                }
            }
        }

        // Loot table cross-reference validation
        for ((npcId, entry) in lootTableCatalog.getAllEntries()) {
            for (lootEntry in entry.items) {
                if (itemCatalog.getItem(lootEntry.itemId) == null) {
                    logger.warn("Loot table '$npcId' references unknown item '${lootEntry.itemId}'")
                }
            }
        }

        // Sound field validation
        for (item in itemCatalog.getAllItems()) {
            if (item.type == "weapon") {
                if (item.attackSound.isBlank()) logger.warn("Weapon '${item.id}' missing attackSound")
                if (item.missSound.isBlank()) logger.warn("Weapon '${item.id}' missing missSound")
            }
            if (item.type == "consumable" && item.useSound.isBlank()) {
                logger.warn("Consumable '${item.id}' missing useSound")
            }
        }
        for ((npcData, _) in allNpcData) {
            if (npcData.hostile) {
                if (npcData.attackSound.isBlank()) logger.warn("Hostile NPC '${npcData.id}' missing attackSound")
                if (npcData.missSound.isBlank()) logger.warn("Hostile NPC '${npcData.id}' missing missSound")
                if (npcData.deathSound.isBlank()) logger.warn("Hostile NPC '${npcData.id}' missing deathSound")
            }
        }
        for (spell in spellCatalog.getAllSpells()) {
            if (spell.castSound.isBlank()) logger.warn("Spell '${spell.id}' missing castSound")
            if ((spell.spellType == com.neomud.shared.model.SpellType.DAMAGE || spell.spellType == com.neomud.shared.model.SpellType.DOT)) {
                if (spell.impactSound.isBlank()) logger.warn("Damage spell '${spell.id}' missing impactSound")
                if (spell.missSound.isBlank()) logger.warn("Damage spell '${spell.id}' missing missSound")
            }
        }
        for (room in worldGraph.getAllRooms()) {
            if (room.departSound.isBlank()) logger.info("Room '${room.id}' missing departSound")
        }

        return LoadResult(worldGraph, allNpcData, classCatalog, itemCatalog, lootTableCatalog, promptTemplateCatalog, skillCatalog, raceCatalog, spellCatalog, zoneSpawnConfigs)
    }
}

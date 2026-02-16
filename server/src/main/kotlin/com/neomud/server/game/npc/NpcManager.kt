package com.neomud.server.game.npc

import com.neomud.server.game.npc.behavior.BehaviorNode
import com.neomud.server.game.npc.behavior.IdleBehavior
import com.neomud.server.game.npc.behavior.NpcAction
import com.neomud.server.game.npc.behavior.PatrolBehavior
import com.neomud.server.game.npc.behavior.WanderBehavior
import com.neomud.server.world.NpcData
import com.neomud.server.world.SpawnConfig
import com.neomud.server.world.WorldGraph
import com.neomud.shared.model.Direction
import com.neomud.shared.model.Npc
import com.neomud.shared.model.RoomId

data class NpcState(
    val id: String,
    val name: String,
    val description: String,
    var currentRoomId: RoomId,
    val behavior: BehaviorNode,
    val hostile: Boolean = false,
    val maxHp: Int = 0,
    var currentHp: Int = 0,
    val damage: Int = 0,
    val level: Int = 1,
    val perception: Int = 0,
    val xpReward: Long = 0,
    val behaviorType: String = "idle",
    val zoneId: String = "",
    val startRoomId: RoomId = "",
    val templateId: String = "",
    val vendorItems: List<String> = emptyList(),
    val accuracy: Int = 0,
    val defense: Int = 0,
    val evasion: Int = 0,
    val attackSound: String = "",
    val missSound: String = "",
    val deathSound: String = ""
) {
    /** Set by [NpcManager.markDead] to prevent double-processing kills. */
    var deathProcessed: Boolean = false
    val isAlive: Boolean get() = !deathProcessed && (maxHp == 0 || currentHp > 0)
}

data class NpcEvent(
    val npcName: String,
    val fromRoomId: RoomId?,
    val toRoomId: RoomId?,
    val direction: Direction?,
    val npcId: String = "",
    val hostile: Boolean = false,
    val currentHp: Int = 0,
    val maxHp: Int = 0,
    val templateId: String = ""
)

class NpcManager(
    private val worldGraph: WorldGraph,
    private val zoneSpawnConfigs: Map<String, SpawnConfig> = emptyMap()
) {
    private val logger = org.slf4j.LoggerFactory.getLogger(NpcManager::class.java)
    private val npcs = mutableListOf<NpcState>()
    private val zoneTemplates = mutableMapOf<String, List<Pair<NpcData, String>>>()
    private val zoneSpawnTimers = mutableMapOf<String, Int>()
    private var nextSpawnIndex = 1

    fun loadNpcs(npcDataList: List<Pair<NpcData, String>>) {
        // Store hostile templates per zone for continuous spawning
        zoneTemplates.putAll(
            npcDataList
                .filter { it.first.hostile }
                .groupBy { it.second }
        )

        for ((data, zoneId) in npcDataList) {
            npcs.add(createNpcState(data, zoneId, data.id))
        }
    }

    private fun createNpcState(data: NpcData, zoneId: String, instanceId: String): NpcState {
        val behavior: BehaviorNode = when (data.behaviorType) {
            "patrol" -> PatrolBehavior(data.patrolRoute)
            "wander" -> WanderBehavior()
            else -> IdleBehavior()
        }

        return NpcState(
            id = instanceId,
            name = data.name,
            description = data.description,
            currentRoomId = data.startRoomId,
            behavior = behavior,
            hostile = data.hostile,
            maxHp = data.maxHp,
            currentHp = data.maxHp,
            damage = data.damage,
            level = data.level,
            perception = data.perception,
            xpReward = data.xpReward,
            behaviorType = data.behaviorType,
            zoneId = zoneId,
            startRoomId = data.startRoomId,
            templateId = data.id,
            vendorItems = data.vendorItems,
            accuracy = data.accuracy,
            defense = data.defense,
            evasion = data.evasion,
            attackSound = data.attackSound,
            missSound = data.missSound,
            deathSound = data.deathSound
        )
    }

    private fun aliveNpcsInRoom(roomId: RoomId): Int =
        npcs.count { it.currentRoomId == roomId && it.isAlive }

    private fun aliveNpcsInZone(zoneId: String): Int =
        npcs.count { it.zoneId == zoneId && it.isAlive }

    fun tick(): List<NpcEvent> {
        val events = mutableListOf<NpcEvent>()

        // 1. Process living NPC behaviors
        for (npc in npcs) {
            if (!npc.isAlive) continue

            val spawnConfig = zoneSpawnConfigs[npc.zoneId]
            val maxPerRoom = spawnConfig?.maxPerRoom ?: 0

            val canMoveTo: (RoomId) -> Boolean = { targetRoomId ->
                maxPerRoom == 0 || aliveNpcsInRoom(targetRoomId) < maxPerRoom
            }

            when (val action = npc.behavior.tick(npc, worldGraph, canMoveTo)) {
                is NpcAction.MoveTo -> {
                    val oldRoom = npc.currentRoomId
                    val newRoom = action.targetRoomId

                    val room = worldGraph.getRoom(oldRoom)
                    val direction = room?.exits?.entries?.find { it.value == newRoom }?.key

                    npc.currentRoomId = newRoom

                    events.add(
                        NpcEvent(
                            npcName = npc.name,
                            fromRoomId = oldRoom,
                            toRoomId = newRoom,
                            direction = direction,
                            npcId = npc.id,
                            hostile = npc.hostile,
                            currentHp = npc.currentHp,
                            maxHp = npc.maxHp,
                            templateId = npc.templateId
                        )
                    )
                }
                is NpcAction.None -> { /* do nothing */ }
            }
        }

        // 2. Clean up dead NPCs (remove from list)
        npcs.removeAll { !it.isAlive }

        // 3. Zone spawn timers — spawn new NPC copies up to maxEntities
        for ((zoneId, config) in zoneSpawnConfigs) {
            if (config.rateTicks == 0 || config.maxEntities == 0) continue

            val timer = zoneSpawnTimers.getOrPut(zoneId) { 0 } + 1
            zoneSpawnTimers[zoneId] = timer

            if (timer < config.rateTicks) continue
            zoneSpawnTimers[zoneId] = 0

            if (aliveNpcsInZone(zoneId) >= config.maxEntities) continue

            val templates = zoneTemplates[zoneId] ?: continue
            if (templates.isEmpty()) continue

            // Pick a random template and try to spawn at its startRoomId
            val (template, _) = templates.random()
            if (config.maxPerRoom > 0 && aliveNpcsInRoom(template.startRoomId) >= config.maxPerRoom) continue

            val instanceId = "${template.id}#${nextSpawnIndex++}"
            val spawned = createNpcState(template, zoneId, instanceId)
            npcs.add(spawned)
            logger.info("Spawned ${spawned.name} ($instanceId) at ${spawned.startRoomId}")

            events.add(
                NpcEvent(
                    npcName = spawned.name,
                    fromRoomId = null,
                    toRoomId = spawned.startRoomId,
                    direction = null,
                    npcId = spawned.id,
                    hostile = spawned.hostile,
                    currentHp = spawned.currentHp,
                    maxHp = spawned.maxHp,
                    templateId = spawned.templateId
                )
            )
        }

        return events
    }

    fun getNpcsInRoom(roomId: RoomId): List<Npc> {
        val all = npcs.filter { it.currentRoomId == roomId }
        val alive = all.filter { it.isAlive }
        if (all.isNotEmpty()) {
            logger.info("getNpcsInRoom($roomId): ${all.size} total, ${alive.size} alive: ${all.map { "${it.name}(hp=${it.currentHp}/${it.maxHp},alive=${it.isAlive})" }}")
        }
        return alive.map { npcState ->
            Npc(
                id = npcState.id,
                name = npcState.name,
                description = npcState.description,
                currentRoomId = npcState.currentRoomId,
                behaviorType = npcState.behaviorType,
                hostile = npcState.hostile,
                currentHp = npcState.currentHp,
                maxHp = npcState.maxHp,
                attackSound = npcState.attackSound,
                missSound = npcState.missSound,
                deathSound = npcState.deathSound
            )
        }
    }

    fun getLivingHostileNpcsInRoom(roomId: RoomId): List<NpcState> =
        npcs.filter { it.currentRoomId == roomId && it.hostile && it.currentHp > 0 }

    fun getLivingNpcsInRoom(roomId: RoomId): List<NpcState> =
        npcs.filter { it.currentRoomId == roomId && it.currentHp > 0 }

    fun getNpcState(npcId: String): NpcState? =
        npcs.find { it.id == npcId && it.isAlive }

    @Synchronized
    fun markDead(npcId: String): Boolean {
        val npc = npcs.find { it.id == npcId } ?: return false
        if (npc.deathProcessed) return false
        npc.deathProcessed = true
        npc.currentHp = 0
        return true
    }

    fun getTrainerInRoom(roomId: RoomId): NpcState? =
        npcs.find { it.currentRoomId == roomId && it.behaviorType == "trainer" && it.isAlive }

    fun getVendorInRoom(roomId: RoomId): NpcState? =
        npcs.find { it.currentRoomId == roomId && it.behaviorType == "vendor" && it.isAlive }
}

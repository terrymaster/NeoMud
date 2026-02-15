package com.neomud.server.game.npc

import com.neomud.server.game.npc.behavior.BehaviorNode
import com.neomud.server.game.npc.behavior.IdleBehavior
import com.neomud.server.game.npc.behavior.NpcAction
import com.neomud.server.game.npc.behavior.PatrolBehavior
import com.neomud.server.game.npc.behavior.WanderBehavior
import com.neomud.server.world.NpcData
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
    val zoneId: String = ""
) {
    val isAlive: Boolean get() = maxHp == 0 || currentHp > 0
}

data class NpcEvent(
    val npcName: String,
    val fromRoomId: RoomId?,
    val toRoomId: RoomId?,
    val direction: Direction?,
    val npcId: String = "",
    val hostile: Boolean = false,
    val currentHp: Int = 0,
    val maxHp: Int = 0
)

class NpcManager(private val worldGraph: WorldGraph) {
    private val logger = org.slf4j.LoggerFactory.getLogger(NpcManager::class.java)
    private val npcs = mutableListOf<NpcState>()

    fun loadNpcs(npcDataList: List<Pair<NpcData, String>>) {
        for ((data, zoneId) in npcDataList) {
            val behavior: BehaviorNode = when (data.behaviorType) {
                "patrol" -> PatrolBehavior(data.patrolRoute)
                "wander" -> WanderBehavior()
                else -> IdleBehavior()
            }

            npcs.add(
                NpcState(
                    id = data.id,
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
                    zoneId = zoneId
                )
            )
        }
    }

    fun tick(): List<NpcEvent> {
        val events = mutableListOf<NpcEvent>()

        for (npc in npcs) {
            if (!npc.isAlive) continue // skip dead NPCs

            when (val action = npc.behavior.tick(npc, worldGraph)) {
                is NpcAction.MoveTo -> {
                    val oldRoom = npc.currentRoomId
                    val newRoom = action.targetRoomId

                    // Find the direction of movement
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
                            maxHp = npc.maxHp
                        )
                    )
                }
                is NpcAction.None -> { /* do nothing */ }
            }
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
                behaviorType = "unknown",
                hostile = npcState.hostile,
                currentHp = npcState.currentHp,
                maxHp = npcState.maxHp
            )
        }
    }

    fun getLivingHostileNpcsInRoom(roomId: RoomId): List<NpcState> =
        npcs.filter { it.currentRoomId == roomId && it.hostile && it.currentHp > 0 }

    fun getLivingNpcsInRoom(roomId: RoomId): List<NpcState> =
        npcs.filter { it.currentRoomId == roomId && it.currentHp > 0 }

    fun getNpcState(npcId: String): NpcState? =
        npcs.find { it.id == npcId && it.isAlive }

    fun markDead(npcId: String) {
        npcs.find { it.id == npcId }?.let { it.currentHp = 0 }
    }
}

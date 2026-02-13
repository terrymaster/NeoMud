package com.neomud.server.game.npc.behavior

import com.neomud.server.game.npc.NpcState
import com.neomud.server.world.WorldGraph

class WanderBehavior(
    private val moveEveryNTicks: Int = 15
) : BehaviorNode {
    private var tickCounter = 0

    override fun tick(npc: NpcState, world: WorldGraph): NpcAction {
        tickCounter++
        if (tickCounter < moveEveryNTicks) return NpcAction.None
        tickCounter = 0

        val currentRoom = world.getRoom(npc.currentRoomId) ?: return NpcAction.None

        // Filter exits to only rooms in the same zone (prevents forest monsters entering town)
        val sameZoneExits = currentRoom.exits.values.filter { targetRoomId ->
            val targetRoom = world.getRoom(targetRoomId)
            targetRoom != null && targetRoom.zoneId == npc.zoneId
        }

        if (sameZoneExits.isEmpty()) return NpcAction.None

        val targetRoomId = sameZoneExits.random()
        return NpcAction.MoveTo(targetRoomId)
    }
}

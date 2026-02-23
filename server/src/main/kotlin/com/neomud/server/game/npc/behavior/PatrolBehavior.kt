package com.neomud.server.game.npc.behavior

import com.neomud.server.game.GameConfig
import com.neomud.server.game.npc.NpcState
import com.neomud.shared.model.RoomId
import com.neomud.server.world.WorldGraph

class PatrolBehavior(
    private val route: List<String>,
    private val moveEveryNTicks: Int = GameConfig.Npc.PATROL_MOVE_TICKS
) : BehaviorNode {
    private var routeIndex = 0
    private var tickCounter = 0

    override fun tick(npc: NpcState, world: WorldGraph, canMoveTo: (RoomId) -> Boolean): NpcAction {
        if (route.size < 2) return NpcAction.None

        tickCounter++
        if (tickCounter < moveEveryNTicks) return NpcAction.None
        tickCounter = 0

        routeIndex = (routeIndex + 1) % route.size
        val targetRoomId = route[routeIndex]

        return if (targetRoomId != npc.currentRoomId && canMoveTo(targetRoomId)) {
            NpcAction.MoveTo(targetRoomId)
        } else {
            NpcAction.None
        }
    }
}

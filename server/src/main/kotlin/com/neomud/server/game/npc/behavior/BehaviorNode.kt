package com.neomud.server.game.npc.behavior

import com.neomud.server.game.npc.NpcState
import com.neomud.server.world.WorldGraph

sealed class NpcAction {
    data object None : NpcAction()
    data class MoveTo(val targetRoomId: String) : NpcAction()
}

interface BehaviorNode {
    fun tick(npc: NpcState, world: WorldGraph): NpcAction
}

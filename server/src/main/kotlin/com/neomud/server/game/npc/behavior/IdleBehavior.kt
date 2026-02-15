package com.neomud.server.game.npc.behavior

import com.neomud.server.game.npc.NpcState
import com.neomud.shared.model.RoomId
import com.neomud.server.world.WorldGraph

class IdleBehavior : BehaviorNode {
    override fun tick(npc: NpcState, world: WorldGraph, canMoveTo: (RoomId) -> Boolean): NpcAction = NpcAction.None
}

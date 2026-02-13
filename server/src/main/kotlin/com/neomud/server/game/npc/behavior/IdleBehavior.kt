package com.neomud.server.game.npc.behavior

import com.neomud.server.game.npc.NpcState
import com.neomud.server.world.WorldGraph

class IdleBehavior : BehaviorNode {
    override fun tick(npc: NpcState, world: WorldGraph): NpcAction = NpcAction.None
}

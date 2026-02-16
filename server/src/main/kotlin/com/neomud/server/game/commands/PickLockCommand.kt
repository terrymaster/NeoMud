package com.neomud.server.game.commands

import com.neomud.server.game.combat.CombatUtils
import com.neomud.server.session.PlayerSession
import com.neomud.server.world.LockStateManager
import com.neomud.server.world.WorldGraph
import com.neomud.shared.protocol.ServerMessage

class PickLockCommand(
    private val worldGraph: WorldGraph,
    private val lockStateManager: LockStateManager
) {
    suspend fun execute(session: PlayerSession) {
        val roomId = session.currentRoomId ?: return
        val player = session.player ?: return

        val cooldown = session.skillCooldowns["PICK_LOCK"]
        if (cooldown != null && cooldown > 0) {
            session.send(ServerMessage.SystemMessage("Pick Lock is on cooldown ($cooldown ticks remaining)."))
            return
        }

        val room = worldGraph.getRoom(roomId) ?: return

        // Find a locked exit that hasn't been unlocked yet
        val lockedExit = room.lockedExits.entries.firstOrNull { (dir, _) ->
            !lockStateManager.isUnlocked(roomId, dir)
        }

        if (lockedExit == null) {
            session.send(ServerMessage.SystemMessage("You don't see anything locked here."))
            return
        }

        session.skillCooldowns["PICK_LOCK"] = 3

        val (direction, difficulty) = lockedExit
        val effStats = CombatUtils.effectiveStats(player.stats, session.activeEffects.toList())
        val roll = (1..20).random()
        val check = effStats.agility + effStats.intellect / 2 + roll

        if (check >= difficulty) {
            lockStateManager.unlock(roomId, direction)
            session.send(ServerMessage.SystemMessage("You pick the lock on the door to the ${direction.name}."))
        } else {
            session.send(ServerMessage.SystemMessage("You fail to pick the lock."))
        }
    }
}

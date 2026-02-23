package com.neomud.server.game.commands

import com.neomud.server.game.GameConfig
import com.neomud.server.game.MapRoomFilter
import com.neomud.server.game.MovementTrailManager
import com.neomud.server.game.npc.NpcManager
import com.neomud.server.session.PlayerSession
import com.neomud.server.session.SessionManager
import com.neomud.server.world.WorldGraph
import com.neomud.shared.model.Direction
import com.neomud.shared.protocol.ServerMessage

class KickCommand(
    private val npcManager: NpcManager,
    private val sessionManager: SessionManager,
    private val killHandler: SkillKillHandler,
    private val worldGraph: WorldGraph,
    private val movementTrailManager: MovementTrailManager? = null
) {
    suspend fun execute(session: PlayerSession, targetId: String?) {
        val roomId = session.currentRoomId ?: return
        val playerName = session.playerName ?: return
        val player = session.player ?: return

        val cooldown = session.skillCooldowns["KICK"]
        if (cooldown != null && cooldown > 0) {
            session.send(ServerMessage.SystemMessage("Kick is on cooldown ($cooldown ticks remaining)."))
            return
        }

        // Parse compound targetId format: "npcId:DIRECTION"
        val resolvedInput = targetId ?: session.selectedTargetId
        val npcId: String?
        val kickDirection: Direction?

        if (resolvedInput != null && resolvedInput.contains(':')) {
            val lastColon = resolvedInput.lastIndexOf(':')
            npcId = resolvedInput.substring(0, lastColon)
            kickDirection = try {
                Direction.valueOf(resolvedInput.substring(lastColon + 1).uppercase())
            } catch (_: IllegalArgumentException) {
                session.send(ServerMessage.SystemMessage("Invalid kick direction."))
                return
            }
        } else {
            npcId = resolvedInput
            kickDirection = null
        }

        val target = if (npcId != null) {
            val npc = npcManager.getNpcState(npcId)
            if (npc != null && npc.currentRoomId == roomId && npc.hostile) npc else null
        } else {
            npcManager.getLivingHostileNpcsInRoom(roomId).firstOrNull()
        }

        if (target == null) {
            session.send(ServerMessage.SystemMessage("No valid target for kick."))
            return
        }

        if (kickDirection == null) {
            session.send(ServerMessage.SystemMessage("You must choose a direction to kick ${target.name}!"))
            return
        }

        if (!session.attackMode) {
            session.attackMode = true
            session.selectedTargetId = target.id
            session.send(ServerMessage.AttackModeUpdate(true))
        }

        // Aggressive action breaks grace period
        session.combatGraceTicks = 0

        // Track engagement
        target.engagedPlayerIds.add(playerName)

        // Damage always applies
        val effStats = session.effectiveStats()
        val damage = effStats.strength / 4 + effStats.agility / 4 + (1..GameConfig.Skills.KICK_DAMAGE_RANGE).random()
        target.currentHp -= damage

        session.skillCooldowns["KICK"] = GameConfig.Skills.KICK_COOLDOWN_TICKS

        // Check for kill before knockback
        if (target.currentHp <= 0) {
            session.send(ServerMessage.SystemMessage("You kick ${target.name} for $damage damage, finishing them off!"))
            sessionManager.broadcastToRoom(
                roomId,
                ServerMessage.CombatHit(
                    attackerName = playerName,
                    defenderName = target.name,
                    damage = damage,
                    defenderHp = 0,
                    defenderMaxHp = target.maxHp,
                    isPlayerDefender = false
                )
            )
            killHandler.handleNpcKill(target, playerName, roomId, session)
            return
        }

        // Validate knockback direction
        val currentRoom = worldGraph.getRoom(roomId)
        val targetRoomId = currentRoom?.exits?.get(kickDirection)
        val isLocked = currentRoom?.lockedExits?.containsKey(kickDirection) == true
        val isUp = kickDirection == Direction.UP

        if (targetRoomId != null && !isLocked && !isUp) {
            // Successful knockback
            session.send(ServerMessage.SystemMessage("You kick ${target.name} for $damage damage, sending them flying ${kickDirection.name.lowercase()}!"))

            sessionManager.broadcastToRoom(
                roomId,
                ServerMessage.CombatHit(
                    attackerName = playerName,
                    defenderName = target.name,
                    damage = damage,
                    defenderHp = target.currentHp.coerceAtLeast(0),
                    defenderMaxHp = target.maxHp,
                    isPlayerDefender = false
                )
            )

            // Move the NPC
            val moveEvent = npcManager.moveNpc(target.id, targetRoomId)
            if (moveEvent != null) {
                // Broadcast NPC left old room
                sessionManager.broadcastToRoom(
                    roomId,
                    ServerMessage.NpcLeft(target.name, roomId, kickDirection, target.id)
                )

                // Auto-disable attack mode for players who lost their target
                for (s in sessionManager.getSessionsInRoom(roomId)) {
                    if (s.attackMode && s.selectedTargetId == target.id) {
                        s.selectedTargetId = null
                        val remaining = npcManager.getLivingHostileNpcsInRoom(roomId)
                        if (remaining.isEmpty()) {
                            s.attackMode = false
                            s.selectedTargetId = null
                            try { s.send(ServerMessage.AttackModeUpdate(false)) } catch (_: Exception) {}
                        }
                    }
                }

                // Broadcast NPC entered new room
                sessionManager.broadcastToRoom(
                    targetRoomId,
                    ServerMessage.NpcEntered(
                        target.name, targetRoomId,
                        target.id, target.hostile, target.currentHp, target.maxHp,
                        spawned = false,
                        templateId = target.templateId
                    )
                )

                // Update map data for players in both rooms
                updateMapForPlayersInRoom(roomId)
                updateMapForPlayersInRoom(targetRoomId)

                target.stunTicks = GameConfig.Skills.KICK_KNOCKBACK_STUN_TICKS

                // Trigger pursuit: NPC was forcibly separated from combat
                val trailMgr = movementTrailManager
                if (trailMgr != null) {
                    npcManager.engagePursuit(target.id, playerName, trailMgr, sessionManager)
                }
            }
        } else {
            // Wall slam — damage applies but NPC stays
            val reason = when {
                isUp -> "into the ceiling"
                isLocked -> "against the locked door"
                else -> "against the wall"
            }
            session.send(ServerMessage.SystemMessage("You kick ${target.name} for $damage damage but they slam $reason!"))

            sessionManager.broadcastToRoom(
                roomId,
                ServerMessage.CombatHit(
                    attackerName = playerName,
                    defenderName = target.name,
                    damage = damage,
                    defenderHp = target.currentHp.coerceAtLeast(0),
                    defenderMaxHp = target.maxHp,
                    isPlayerDefender = false
                )
            )
        }
    }

    private suspend fun updateMapForPlayersInRoom(roomId: String) {
        for (s in sessionManager.getSessionsInRoom(roomId)) {
            try {
                val mapRooms = MapRoomFilter.enrichForPlayer(
                    worldGraph.getRoomsNear(roomId), s, worldGraph, sessionManager, npcManager
                )
                s.send(ServerMessage.MapData(mapRooms, roomId))
            } catch (_: Exception) {}
        }
    }
}

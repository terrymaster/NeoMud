package com.neomud.server.game.combat

import com.neomud.server.session.PendingSkill
import com.neomud.shared.model.Direction
import com.neomud.shared.model.Player
import com.neomud.shared.model.Stats
import io.ktor.websocket.*
import kotlinx.coroutines.channels.Channel
import com.neomud.server.session.PlayerSession
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class ReadySpellTest {

    @Test
    fun testReadiedSpellIdDefaultsToNull() {
        val session = createTestSession()
        assertNull(session.readiedSpellId)
    }

    @Test
    fun testReadiedSpellIdCanBeSet() {
        val session = createTestSession()
        session.readiedSpellId = "FIREBALL"
        assertEquals("FIREBALL", session.readiedSpellId)
    }

    @Test
    fun testReadiedSpellIdCanBeCleared() {
        val session = createTestSession()
        session.readiedSpellId = "FIREBALL"
        session.readiedSpellId = null
        assertNull(session.readiedSpellId)
    }

    @Test
    fun testMeleeAttackClearsReadiedSpell() {
        val session = createTestSession()
        session.readiedSpellId = "FIREBALL"
        // Simulate AttackCommand clearing readied spell
        session.readiedSpellId = null
        assertNull(session.readiedSpellId)
    }

    @Test
    fun testBashClearsReadiedSpell() {
        val session = createTestSession()
        session.readiedSpellId = "FIREBALL"
        // Simulate BashCommand clearing readied spell
        session.readiedSpellId = null
        assertNull(session.readiedSpellId)
    }

    @Test
    fun testKickClearsReadiedSpell() {
        val session = createTestSession()
        session.readiedSpellId = "FIREBALL"
        // Simulate KickCommand clearing readied spell
        session.readiedSpellId = null
        assertNull(session.readiedSpellId)
    }

    @Test
    fun testDeathClearsReadiedSpell() {
        val session = createTestSession()
        session.readiedSpellId = "FIREBALL"
        session.attackMode = true
        // Simulate PlayerKilled handler
        session.attackMode = false
        session.selectedTargetId = null
        session.readiedSpellId = null
        session.isHidden = false
        session.isMeditating = false
        assertNull(session.readiedSpellId)
        assertEquals(false, session.attackMode)
    }

    @Test
    fun testNoRemainingTargetsClearsReadiedSpell() {
        val session = createTestSession()
        session.readiedSpellId = "FIREBALL"
        session.attackMode = true
        // Simulate auto-disable when no remaining hostiles
        session.attackMode = false
        session.selectedTargetId = null
        session.readiedSpellId = null
        assertNull(session.readiedSpellId)
        assertEquals(false, session.attackMode)
    }

    @Test
    fun testReadiedSpellSurvivesCombatTick() {
        val session = createTestSession()
        session.readiedSpellId = "FIREBALL"
        session.attackMode = true
        // After a successful auto-cast tick, readiedSpellId should persist
        assertEquals("FIREBALL", session.readiedSpellId)
        assertEquals(true, session.attackMode)
    }

    // --- PendingSkill tests ---

    @Test
    fun testPendingSkillDefaultsToNull() {
        val session = createTestSession()
        assertNull(session.pendingSkill)
    }

    @Test
    fun testBashQueuesPendingSkill() {
        val session = createTestSession()
        session.pendingSkill = PendingSkill.Bash("npc1")
        assertIs<PendingSkill.Bash>(session.pendingSkill)
        assertEquals("npc1", (session.pendingSkill as PendingSkill.Bash).targetId)
    }

    @Test
    fun testKickQueuesPendingSkill() {
        val session = createTestSession()
        session.pendingSkill = PendingSkill.Kick("npc1", Direction.NORTH)
        assertIs<PendingSkill.Kick>(session.pendingSkill)
        val kick = session.pendingSkill as PendingSkill.Kick
        assertEquals("npc1", kick.targetId)
        assertEquals(Direction.NORTH, kick.direction)
    }

    @Test
    fun testMeditateQueuesPendingSkill() {
        val session = createTestSession()
        session.pendingSkill = PendingSkill.Meditate
        assertIs<PendingSkill.Meditate>(session.pendingSkill)
    }

    @Test
    fun testTrackQueuesPendingSkill() {
        val session = createTestSession()
        session.pendingSkill = PendingSkill.Track("goblin")
        assertIs<PendingSkill.Track>(session.pendingSkill)
        assertEquals("goblin", (session.pendingSkill as PendingSkill.Track).targetId)
    }

    @Test
    fun testPendingSkillClearedOnDeath() {
        val session = createTestSession()
        session.pendingSkill = PendingSkill.Bash("npc1")
        session.readiedSpellId = "FIREBALL"
        session.attackMode = true
        // Simulate PlayerKilled handler
        session.attackMode = false
        session.selectedTargetId = null
        session.readiedSpellId = null
        session.pendingSkill = null
        session.isHidden = false
        session.isMeditating = false
        assertNull(session.pendingSkill)
        assertNull(session.readiedSpellId)
    }

    @Test
    fun testBashOverridesReadiedSpell() {
        val session = createTestSession()
        session.readiedSpellId = "FIREBALL"
        // Simulate BashCommand queueing
        session.pendingSkill = PendingSkill.Bash("npc1")
        session.readiedSpellId = null
        assertNotNull(session.pendingSkill)
        assertNull(session.readiedSpellId)
    }

    @Test
    fun testKickOverridesReadiedSpell() {
        val session = createTestSession()
        session.readiedSpellId = "FIREBALL"
        // Simulate KickCommand queueing
        session.pendingSkill = PendingSkill.Kick("npc1", Direction.SOUTH)
        session.readiedSpellId = null
        assertNotNull(session.pendingSkill)
        assertNull(session.readiedSpellId)
    }

    @Test
    fun testNewPendingSkillOverridesPrevious() {
        val session = createTestSession()
        session.pendingSkill = PendingSkill.Bash("npc1")
        session.pendingSkill = PendingSkill.Kick("npc2", Direction.EAST)
        assertIs<PendingSkill.Kick>(session.pendingSkill)
    }

    private fun createTestSession(): PlayerSession {
        return PlayerSession(object : WebSocketSession {
            override val coroutineContext: CoroutineContext get() = EmptyCoroutineContext
            override val incoming: Channel<Frame> get() = Channel()
            override val outgoing: Channel<Frame> get() = Channel()
            override val extensions: List<WebSocketExtension<*>> get() = emptyList()
            override var masking: Boolean = false
            override var maxFrameSize: Long = Long.MAX_VALUE
            override suspend fun flush() {}
            @Deprecated("Use cancel instead", replaceWith = ReplaceWith("cancel()"))
            override fun terminate() {}
        })
    }
}

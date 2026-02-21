package com.neomud.server.game

import com.neomud.shared.model.RoomInteractable
import com.neomud.shared.model.Stats
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests the difficulty check logic used by InteractCommand.
 * The check rolls: statValue + level/2 + d20 >= difficulty
 * This test validates the algorithm boundaries without needing
 * the full WebSocket/session infrastructure.
 */
class InteractDifficultyTest {

    /** Reproduces the stat lookup logic from InteractCommand */
    private fun getStatForCheck(stats: Stats, difficultyCheck: String): Int = when (difficultyCheck) {
        "STRENGTH" -> stats.strength
        "AGILITY" -> stats.agility
        "INTELLECT" -> stats.intellect
        "WILLPOWER" -> stats.willpower
        else -> 0
    }

    /** Simulates the difficulty check with a fixed roll instead of random */
    private fun checkPasses(stats: Stats, level: Int, feat: RoomInteractable, diceRoll: Int): Boolean {
        if (feat.difficulty <= 0 || feat.difficultyCheck.isEmpty()) return true
        val statValue = getStatForCheck(stats, feat.difficultyCheck)
        val roll = statValue + level / 2 + diceRoll
        return roll >= feat.difficulty
    }

    @Test
    fun testNoDifficultyAlwaysPasses() {
        val feat = RoomInteractable(
            id = "easy", label = "Easy", description = "desc",
            actionType = "EXIT_OPEN", difficulty = 0, difficultyCheck = ""
        )
        val stats = Stats(strength = 1, agility = 1, intellect = 1, willpower = 1)
        assertTrue(checkPasses(stats, 1, feat, 1), "No difficulty should always pass")
    }

    @Test
    fun testDifficultyWithoutCheckAlwaysPasses() {
        val feat = RoomInteractable(
            id = "no_check", label = "Test", description = "desc",
            actionType = "EXIT_OPEN", difficulty = 99, difficultyCheck = ""
        )
        val stats = Stats(strength = 1)
        assertTrue(checkPasses(stats, 1, feat, 1), "difficulty > 0 but empty check should pass")
    }

    @Test
    fun testStrengthCheckPass() {
        val feat = RoomInteractable(
            id = "door", label = "Heavy Door", description = "Open!",
            failureMessage = "Can't budge it.",
            actionType = "EXIT_OPEN", difficulty = 25, difficultyCheck = "STRENGTH"
        )
        val stats = Stats(strength = 20)
        // 20 (str) + 5 (level 10 / 2) + 1 (min roll) = 26 >= 25
        assertTrue(checkPasses(stats, 10, feat, 1))
    }

    @Test
    fun testStrengthCheckFail() {
        val feat = RoomInteractable(
            id = "door", label = "Heavy Door", description = "Open!",
            failureMessage = "Can't budge it.",
            actionType = "EXIT_OPEN", difficulty = 25, difficultyCheck = "STRENGTH"
        )
        val stats = Stats(strength = 10)
        // 10 (str) + 1 (level 2 / 2) + 1 (min roll) = 12 < 25
        assertFalse(checkPasses(stats, 2, feat, 1))
    }

    @Test
    fun testAgilityCheck() {
        val feat = RoomInteractable(
            id = "trap", label = "Trap", description = "Disarmed!",
            failureMessage = "Ouch!",
            actionType = "ROOM_EFFECT", difficulty = 20, difficultyCheck = "AGILITY"
        )
        val stats = Stats(agility = 15)
        // 15 + 2 (level 4 / 2) + 5 = 22 >= 20
        assertTrue(checkPasses(stats, 4, feat, 5))
        // 15 + 2 + 1 = 18 < 20
        assertFalse(checkPasses(stats, 4, feat, 1))
    }

    @Test
    fun testIntellectCheck() {
        val feat = RoomInteractable(
            id = "puzzle", label = "Puzzle", description = "Solved!",
            failureMessage = "Too complex.",
            actionType = "TELEPORT", difficulty = 30, difficultyCheck = "INTELLECT"
        )
        val stats = Stats(intellect = 25)
        // 25 + 5 (level 10 / 2) + 1 = 31 >= 30
        assertTrue(checkPasses(stats, 10, feat, 1))
        // 25 + 0 (level 0 / 2) + 1 = 26 < 30
        assertFalse(checkPasses(stats, 0, feat, 1))
    }

    @Test
    fun testWillpowerCheck() {
        val feat = RoomInteractable(
            id = "shrine", label = "Shrine", description = "Blessed!",
            failureMessage = "Your mind wavers.",
            actionType = "ROOM_EFFECT", difficulty = 18, difficultyCheck = "WILLPOWER"
        )
        val stats = Stats(willpower = 12)
        // 12 + 3 (level 6 / 2) + 5 = 20 >= 18
        assertTrue(checkPasses(stats, 6, feat, 5))
        // 12 + 0 + 1 = 13 < 18
        assertFalse(checkPasses(stats, 0, feat, 1))
    }

    @Test
    fun testExactThresholdPasses() {
        val feat = RoomInteractable(
            id = "test", label = "Test", description = "desc",
            actionType = "EXIT_OPEN", difficulty = 20, difficultyCheck = "STRENGTH"
        )
        val stats = Stats(strength = 15)
        // 15 + 2 (level 4 / 2) + 3 = 20 == 20 → passes
        assertTrue(checkPasses(stats, 4, feat, 3), "Exact threshold should pass")
    }

    @Test
    fun testOneUnderThresholdFails() {
        val feat = RoomInteractable(
            id = "test", label = "Test", description = "desc",
            actionType = "EXIT_OPEN", difficulty = 20, difficultyCheck = "STRENGTH"
        )
        val stats = Stats(strength = 15)
        // 15 + 2 + 2 = 19 < 20 → fails
        assertFalse(checkPasses(stats, 4, feat, 2), "One under threshold should fail")
    }

    @Test
    fun testLevelContribution() {
        val feat = RoomInteractable(
            id = "test", label = "Test", description = "desc",
            actionType = "EXIT_OPEN", difficulty = 20, difficultyCheck = "AGILITY"
        )
        val stats = Stats(agility = 10)
        // Level 1: 10 + 0 + 10 = 20 >= 20 → passes (level 1 / 2 = 0)
        assertTrue(checkPasses(stats, 1, feat, 10))
        // Level 20: 10 + 10 + 1 = 21 >= 20 → passes
        assertTrue(checkPasses(stats, 20, feat, 1))
        // Level 1: 10 + 0 + 9 = 19 < 20 → fails
        assertFalse(checkPasses(stats, 1, feat, 9))
    }

    @Test
    fun testUnknownCheckTypeUsesZero() {
        val feat = RoomInteractable(
            id = "test", label = "Test", description = "desc",
            actionType = "EXIT_OPEN", difficulty = 5, difficultyCheck = "CHARISMA"
        )
        val stats = Stats(strength = 50, agility = 50, intellect = 50, willpower = 50)
        // Unknown check → stat = 0, so 0 + 0 + 1 = 1 < 5
        assertFalse(checkPasses(stats, 0, feat, 1))
        // 0 + 0 + 5 = 5 >= 5 → passes
        assertTrue(checkPasses(stats, 0, feat, 5))
    }

    @Test
    fun testMaxRollD20() {
        val feat = RoomInteractable(
            id = "hard", label = "Hard", description = "desc",
            actionType = "EXIT_OPEN", difficulty = 50, difficultyCheck = "STRENGTH"
        )
        val stats = Stats(strength = 20)
        // 20 + 5 (level 10) + 20 (max d20) = 45 < 50
        assertFalse(checkPasses(stats, 10, feat, 20))
        // 20 + 10 (level 20) + 20 = 50 >= 50
        assertTrue(checkPasses(stats, 20, feat, 20))
    }
}

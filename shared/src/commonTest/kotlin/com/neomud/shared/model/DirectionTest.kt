package com.neomud.shared.model

import kotlin.test.Test
import kotlin.test.assertEquals

class DirectionTest {

    @Test
    fun testLockedExitPhraseCardinalDirections() {
        assertEquals("the door to the north", Direction.NORTH.lockedExitPhrase)
        assertEquals("the door to the south", Direction.SOUTH.lockedExitPhrase)
        assertEquals("the door to the east", Direction.EAST.lockedExitPhrase)
        assertEquals("the door to the west", Direction.WEST.lockedExitPhrase)
    }

    @Test
    fun testLockedExitPhraseVerticalDirections() {
        assertEquals("the way up", Direction.UP.lockedExitPhrase)
        assertEquals("the way down", Direction.DOWN.lockedExitPhrase)
    }

    @Test
    fun testLockedExitPhraseDiagonalDirections() {
        assertEquals("the door to the northeast", Direction.NORTHEAST.lockedExitPhrase)
        assertEquals("the door to the southwest", Direction.SOUTHWEST.lockedExitPhrase)
    }
}

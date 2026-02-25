package com.neomud.server.game.commands

import com.neomud.shared.model.EquipmentSlots
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Verifies that EquipmentSlots.DEFAULT_SLOTS (which InventoryCommand uses as VALID_SLOTS)
 * includes neck and ring slots so players can equip accessories.
 */
class InventoryCommandValidSlotsTest {

    private val validSlots = EquipmentSlots.DEFAULT_SLOTS.toSet()

    @Test
    fun testNeckIsValidEquipSlot() {
        assertTrue("neck" in validSlots, "neck should be a valid equipment slot")
    }

    @Test
    fun testRingIsValidEquipSlot() {
        assertTrue("ring" in validSlots, "ring should be a valid equipment slot")
    }

    @Test
    fun testAllOriginalSlotsStillValid() {
        for (slot in listOf("weapon", "shield", "head", "chest", "legs", "feet", "hands")) {
            assertTrue(slot in validSlots, "$slot should still be a valid equipment slot")
        }
    }
}

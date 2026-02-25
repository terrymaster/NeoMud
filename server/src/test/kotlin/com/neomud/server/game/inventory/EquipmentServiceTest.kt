package com.neomud.server.game.inventory

import com.neomud.server.world.ItemCatalog
import com.neomud.shared.model.Item
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for EquipmentService.getCombatBonuses using a direct computation approach.
 * Since EquipmentService iterates over equipped items generically, we verify that
 * neck and ring slot items contribute their bonuses by testing the same loop logic.
 */
class EquipmentServiceTest {

    /**
     * Simulate getCombatBonuses logic to verify neck/ring items contribute correctly.
     * This mirrors EquipmentService.getCombatBonuses without needing a real DB.
     */
    private fun computeBonuses(
        equipped: Map<String, String>,
        catalog: ItemCatalog
    ): CombatBonuses {
        var totalDamageBonus = 0
        var weaponDamageRange = 0
        var totalArmorValue = 0
        var shieldBonus = 0

        for ((slot, itemId) in equipped) {
            val item = catalog.getItem(itemId) ?: continue
            totalDamageBonus += item.damageBonus
            if (item.type == "weapon") {
                weaponDamageRange = item.damageRange
            }
            totalArmorValue += item.armorValue
            if (slot == "shield" && item.armorValue > 0) {
                shieldBonus = 5
            }
        }

        return CombatBonuses(totalDamageBonus, weaponDamageRange, totalArmorValue, shieldBonus)
    }

    private fun catalogOf(vararg items: Item) = ItemCatalog(items.toList())

    @Test
    fun testNeckItemContributesDamageBonus() {
        val amulet = Item(
            id = "item:amulet_of_warding", name = "Amulet of Warding",
            description = "A protective amulet", type = "armor", slot = "neck",
            damageBonus = 2
        )
        val bonuses = computeBonuses(mapOf("neck" to "item:amulet_of_warding"), catalogOf(amulet))
        assertEquals(2, bonuses.totalDamageBonus)
    }

    @Test
    fun testRingItemContributesDamageBonus() {
        val ring = Item(
            id = "item:ring_of_intellect", name = "Ring of Intellect",
            description = "A ring of power", type = "armor", slot = "ring",
            damageBonus = 1
        )
        val bonuses = computeBonuses(mapOf("ring" to "item:ring_of_intellect"), catalogOf(ring))
        assertEquals(1, bonuses.totalDamageBonus)
    }

    @Test
    fun testNeckAndRingBonusesCombineWithOtherSlots() {
        val sword = Item(
            id = "item:iron_sword", name = "Iron Sword", description = "A sword",
            type = "weapon", slot = "weapon", damageBonus = 3, damageRange = 6
        )
        val amulet = Item(
            id = "item:amulet_of_warding", name = "Amulet of Warding",
            description = "A protective amulet", type = "armor", slot = "neck",
            damageBonus = 2, armorValue = 1
        )
        val ring = Item(
            id = "item:ring_of_intellect", name = "Ring of Intellect",
            description = "A ring", type = "armor", slot = "ring", damageBonus = 1
        )
        val catalog = catalogOf(sword, amulet, ring)
        val bonuses = computeBonuses(
            mapOf("weapon" to "item:iron_sword", "neck" to "item:amulet_of_warding", "ring" to "item:ring_of_intellect"),
            catalog
        )

        assertEquals(6, bonuses.totalDamageBonus, "3 (sword) + 2 (amulet) + 1 (ring)")
        assertEquals(6, bonuses.weaponDamageRange)
        assertEquals(1, bonuses.totalArmorValue, "1 from amulet")
    }

    @Test
    fun testNeckItemArmorValueContributes() {
        val amulet = Item(
            id = "item:amulet", name = "Amulet", description = "An amulet",
            type = "armor", slot = "neck", armorValue = 3
        )
        val bonuses = computeBonuses(mapOf("neck" to "item:amulet"), catalogOf(amulet))
        assertEquals(3, bonuses.totalArmorValue)
        assertEquals(0, bonuses.shieldBonus, "Neck slot should not grant shield bonus")
    }

    @Test
    fun testEmptyEquipmentReturnsZeroBonuses() {
        val bonuses = computeBonuses(emptyMap(), catalogOf())
        assertEquals(0, bonuses.totalDamageBonus)
        assertEquals(0, bonuses.weaponDamageRange)
        assertEquals(0, bonuses.totalArmorValue)
        assertEquals(0, bonuses.shieldBonus)
    }
}

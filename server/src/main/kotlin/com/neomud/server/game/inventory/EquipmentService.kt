package com.neomud.server.game.inventory

import com.neomud.server.persistence.repository.InventoryRepository
import com.neomud.server.world.ItemCatalog

data class CombatBonuses(
    val totalDamageBonus: Int = 0,
    val weaponDamageRange: Int = 0,
    val totalArmorValue: Int = 0,
    val shieldBonus: Int = 0
)

class EquipmentService(
    private val inventoryRepository: InventoryRepository,
    private val itemCatalog: ItemCatalog
) {
    fun getCombatBonuses(playerName: String): CombatBonuses {
        val equipped = inventoryRepository.getEquippedItems(playerName)
        var totalDamageBonus = 0
        var weaponDamageRange = 0
        var totalArmorValue = 0
        var shieldBonus = 0

        for ((slot, itemId) in equipped) {
            val item = itemCatalog.getItem(itemId) ?: continue
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
}

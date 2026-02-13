package com.neomud.shared.model

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class ItemSerializationTest {

    private val json = Json { encodeDefaults = true }

    @Test
    fun testItemRoundTrip() {
        val original = Item(
            id = "item:iron_sword",
            name = "Iron Sword",
            description = "A sturdy blade.",
            type = "weapon",
            slot = "weapon",
            damageBonus = 3,
            damageRange = 6,
            value = 50,
            weight = 5,
            levelRequirement = 1
        )
        val encoded = json.encodeToString(Item.serializer(), original)
        val decoded = json.decodeFromString(Item.serializer(), encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun testArmorItemRoundTrip() {
        val original = Item(
            id = "item:leather_chest",
            name = "Leather Vest",
            description = "A tough vest.",
            type = "armor",
            slot = "chest",
            armorValue = 3,
            value = 40,
            weight = 4
        )
        val encoded = json.encodeToString(Item.serializer(), original)
        val decoded = json.decodeFromString(Item.serializer(), encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun testConsumableItemRoundTrip() {
        val original = Item(
            id = "item:health_potion",
            name = "Health Potion",
            description = "Restores health.",
            type = "consumable",
            useEffect = "heal:25",
            stackable = true,
            maxStack = 10,
            value = 20,
            weight = 1
        )
        val encoded = json.encodeToString(Item.serializer(), original)
        val decoded = json.decodeFromString(Item.serializer(), encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun testInventoryItemRoundTrip() {
        val original = InventoryItem(
            itemId = "item:iron_sword",
            quantity = 1,
            equipped = true,
            slot = "weapon"
        )
        val encoded = json.encodeToString(InventoryItem.serializer(), original)
        val decoded = json.decodeFromString(InventoryItem.serializer(), encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun testInventoryItemDefaultsRoundTrip() {
        val original = InventoryItem(itemId = "item:health_potion", quantity = 5)
        val encoded = json.encodeToString(InventoryItem.serializer(), original)
        val decoded = json.decodeFromString(InventoryItem.serializer(), encoded)
        assertEquals(original, decoded)
    }
}

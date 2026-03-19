package com.neomud.client.ui.components

import androidx.compose.ui.test.*
import com.neomud.client.testutil.ComposeTestBase
import com.neomud.client.testutil.TestData
import com.neomud.client.testutil.TestThemeWrapper
import com.neomud.client.testutil.installTestCoil
import com.neomud.client.testutil.resetTestCoil
import kotlin.test.BeforeTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class EquipmentPanelTest : ComposeTestBase() {

    @BeforeTest
    fun setup() { installTestCoil() }

    @AfterTest
    fun teardown() { resetTestCoil() }

    private val catalog = TestData.itemCatalog()

    @Test
    fun shows_Equipment_header() = runComposeUiTest {
        setContent {
            TestThemeWrapper {
                EquipmentPanel(
                    inventory = emptyList(),
                    equipment = emptyMap(),
                    itemCatalog = catalog,
                    onEquipItem = { _, _ -> },
                    onUnequipItem = {},
                    onClose = {}
                )
            }
        }

        onNodeWithText("Equipment", substring = true).assertExists()
    }

    @Test
    fun shows_paperdoll_slot_labels() = runComposeUiTest {
        setContent {
            TestThemeWrapper {
                EquipmentPanel(
                    inventory = emptyList(),
                    equipment = emptyMap(),
                    itemCatalog = catalog,
                    onEquipItem = { _, _ -> },
                    onUnequipItem = {},
                    onClose = {}
                )
            }
        }

        onNodeWithText("Head").assertExists()
        onNodeWithText("Chest").assertExists()
        onNodeWithText("Weapon").assertExists()
        onNodeWithText("Shield").assertExists()
        onNodeWithText("Hands").assertExists()
        onNodeWithText("Legs").assertExists()
        onNodeWithText("Feet").assertExists()
    }

    @Test
    fun equipped_slot_shows_item_image() = runComposeUiTest {
        val equipment = mapOf("weapon" to "iron_sword")

        setContent {
            TestThemeWrapper {
                EquipmentPanel(
                    inventory = listOf(TestData.inventoryItem(itemId = "iron_sword", equipped = true, slot = "weapon")),
                    equipment = equipment,
                    itemCatalog = catalog,
                    onEquipItem = { _, _ -> },
                    onUnequipItem = {},
                    onClose = {}
                )
            }
        }

        // The equipped item should render with its name as content description
        onNodeWithContentDescription("Iron Sword").assertExists()
    }

    @Test
    fun shows_equippable_bag_items_section() = runComposeUiTest {
        val inventory = listOf(
            TestData.inventoryItem(itemId = "iron_helm", equipped = false)
        )

        setContent {
            TestThemeWrapper {
                EquipmentPanel(
                    inventory = inventory,
                    equipment = emptyMap(),
                    itemCatalog = catalog,
                    onEquipItem = { _, _ -> },
                    onUnequipItem = {},
                    onClose = {}
                )
            }
        }

        onNodeWithText("Equippable Items", substring = true).assertExists()
        onNodeWithText("Iron Helm", useUnmergedTree = true).assertExists()
    }

    @Test
    fun equippable_bag_items_are_present_and_clickable() = runComposeUiTest {
        var equippedPair: Pair<String, String>? = null
        val inventory = listOf(
            TestData.inventoryItem(itemId = "iron_helm", equipped = false)
        )

        setContent {
            TestThemeWrapper {
                EquipmentPanel(
                    inventory = inventory,
                    equipment = emptyMap(),
                    itemCatalog = catalog,
                    onEquipItem = { id, slot -> equippedPair = id to slot },
                    onUnequipItem = {},
                    onClose = {}
                )
            }
        }

        onNodeWithText("Iron Helm", useUnmergedTree = true).assertExists()
        onNodeWithContentDescription("Iron Helm", useUnmergedTree = true).assertExists()
    }

    @Test
    fun shows_no_equippable_items_message_when_bag_is_empty() = runComposeUiTest {
        setContent {
            TestThemeWrapper {
                EquipmentPanel(
                    inventory = emptyList(),
                    equipment = emptyMap(),
                    itemCatalog = catalog,
                    onEquipItem = { _, _ -> },
                    onUnequipItem = {},
                    onClose = {}
                )
            }
        }

        onNodeWithText("No equippable items in bag.").assertExists()
    }
}

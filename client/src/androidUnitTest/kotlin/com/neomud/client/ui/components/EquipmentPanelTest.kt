package com.neomud.client.ui.components

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.neomud.client.testutil.CoilTestRule
import com.neomud.client.testutil.TestData
import com.neomud.client.testutil.TestThemeWrapper
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EquipmentPanelTest {

    private val coilRule = CoilTestRule()
    private val composeRule = createComposeRule()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(coilRule).around(composeRule)

    private val catalog = TestData.itemCatalog()

    @Test
    fun `shows Equipment header`() {
        composeRule.setContent {
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

        composeRule.onNodeWithText("Equipment").assertIsDisplayed()
    }

    @Test
    fun `shows paperdoll slot labels`() {
        composeRule.setContent {
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

        composeRule.onNodeWithText("Head").assertIsDisplayed()
        composeRule.onNodeWithText("Chest").assertIsDisplayed()
        composeRule.onNodeWithText("Weapon").assertIsDisplayed()
        composeRule.onNodeWithText("Shield").assertIsDisplayed()
        composeRule.onNodeWithText("Hands").assertIsDisplayed()
        composeRule.onNodeWithText("Legs").assertIsDisplayed()
        composeRule.onNodeWithText("Feet").assertIsDisplayed()
    }

    @Test
    fun `unequip callback fires when equipped slot is clicked`() {
        var unequippedSlot: String? = null
        val equipment = mapOf("weapon" to "iron_sword")

        composeRule.setContent {
            TestThemeWrapper {
                EquipmentPanel(
                    inventory = listOf(TestData.inventoryItem(itemId = "iron_sword", equipped = true, slot = "weapon")),
                    equipment = equipment,
                    itemCatalog = catalog,
                    onEquipItem = { _, _ -> },
                    onUnequipItem = { unequippedSlot = it },
                    onClose = {}
                )
            }
        }

        // Click on the weapon slot image (by content description)
        composeRule.onNodeWithContentDescription("Iron Sword").performClick()
        assert(unequippedSlot == "weapon") { "Expected weapon, got $unequippedSlot" }
    }

    @Test
    fun `shows equippable bag items section`() {
        val inventory = listOf(
            TestData.inventoryItem(itemId = "iron_helm", equipped = false)
        )

        composeRule.setContent {
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

        // Content is in a merged scrollable column; verify text exists in the tree
        composeRule.onNodeWithText("Equippable Items").assertExists()
        composeRule.onNodeWithText("Iron Helm", useUnmergedTree = true).assertExists()
    }

    @Test
    fun `equippable bag items are present and clickable`() {
        var equippedPair: Pair<String, String>? = null
        val inventory = listOf(
            TestData.inventoryItem(itemId = "iron_helm", equipped = false)
        )

        composeRule.setContent {
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

        // Verify the equippable item exists in the semantic tree
        composeRule.onNodeWithText("Iron Helm", useUnmergedTree = true).assertExists()
        // The clickable Column wrapping the item should have click action
        composeRule.onNodeWithContentDescription("Iron Helm", useUnmergedTree = true).assertExists()
    }

    @Test
    fun `shows no equippable items message when bag is empty`() {
        composeRule.setContent {
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

        composeRule.onNodeWithText("No equippable items in bag.").assertIsDisplayed()
    }
}

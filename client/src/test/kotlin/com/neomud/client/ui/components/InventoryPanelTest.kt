package com.neomud.client.ui.components

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.neomud.client.testutil.CoilTestRule
import com.neomud.client.testutil.TestData
import com.neomud.client.testutil.TestThemeWrapper
import com.neomud.shared.model.Coins
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class InventoryPanelTest {

    private val coilRule = CoilTestRule()
    private val composeRule = createComposeRule()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(coilRule).around(composeRule)

    private val catalog = TestData.itemCatalog()

    @Test
    fun `header shows Inventory title`() {
        composeRule.setContent {
            TestThemeWrapper {
                InventoryPanel(
                    inventory = emptyList(),
                    itemCatalog = catalog,
                    playerCoins = Coins(),
                    onUseItem = {},
                    onClose = {}
                )
            }
        }

        composeRule.onNodeWithText("Inventory").assertIsDisplayed()
    }

    @Test
    fun `close button fires onClose`() {
        var closed = false
        composeRule.setContent {
            TestThemeWrapper {
                InventoryPanel(
                    inventory = emptyList(),
                    itemCatalog = catalog,
                    playerCoins = Coins(),
                    onUseItem = {},
                    onClose = { closed = true }
                )
            }
        }

        composeRule.onNodeWithText("X").performClick()
        assert(closed) { "Expected onClose to fire" }
    }

    @Test
    fun `shows coin badges when player has coins`() {
        composeRule.setContent {
            TestThemeWrapper {
                InventoryPanel(
                    inventory = emptyList(),
                    itemCatalog = catalog,
                    playerCoins = TestData.coins(copper = 50, silver = 10, gold = 2),
                    onUseItem = {},
                    onClose = {}
                )
            }
        }

        composeRule.onNodeWithText("2 GP").assertIsDisplayed()
        composeRule.onNodeWithText("10 SP").assertIsDisplayed()
        composeRule.onNodeWithText("50 CP").assertIsDisplayed()
    }

    @Test
    fun `hides coin section when coins are empty`() {
        composeRule.setContent {
            TestThemeWrapper {
                InventoryPanel(
                    inventory = emptyList(),
                    itemCatalog = catalog,
                    playerCoins = Coins(),
                    onUseItem = {},
                    onClose = {}
                )
            }
        }

        composeRule.onNodeWithText("Coins").assertDoesNotExist()
    }

    @Test
    fun `shows empty bag message when no non-equipment items`() {
        composeRule.setContent {
            TestThemeWrapper {
                InventoryPanel(
                    inventory = emptyList(),
                    itemCatalog = catalog,
                    playerCoins = Coins(),
                    onUseItem = {},
                    onClose = {}
                )
            }
        }

        composeRule.onNodeWithText("Your bag is empty.").assertIsDisplayed()
    }

    @Test
    fun `shows consumable Use badge and fires onUseItem`() {
        var usedItem: String? = null
        val inventory = listOf(
            TestData.inventoryItem(itemId = "health_potion", quantity = 3)
        )

        composeRule.setContent {
            TestThemeWrapper {
                InventoryPanel(
                    inventory = inventory,
                    itemCatalog = catalog,
                    playerCoins = Coins(),
                    onUseItem = { usedItem = it },
                    onClose = {}
                )
            }
        }

        composeRule.onNodeWithText("Use").assertIsDisplayed()
        composeRule.onNodeWithText("Health Potion").performClick()
        assert(usedItem == "health_potion") { "Expected health_potion, got $usedItem" }
    }

    @Test
    fun `shows quantity badge for stacked items`() {
        val inventory = listOf(
            TestData.inventoryItem(itemId = "health_potion", quantity = 5)
        )

        composeRule.setContent {
            TestThemeWrapper {
                InventoryPanel(
                    inventory = inventory,
                    itemCatalog = catalog,
                    playerCoins = Coins(),
                    onUseItem = {},
                    onClose = {}
                )
            }
        }

        composeRule.onNodeWithText("5").assertIsDisplayed()
    }

    @Test
    fun `excludes equipped items from bag`() {
        val inventory = listOf(
            TestData.inventoryItem(itemId = "gold_ring", quantity = 1, equipped = false),
            TestData.inventoryItem(itemId = "health_potion", quantity = 1, equipped = true)
        )

        composeRule.setContent {
            TestThemeWrapper {
                InventoryPanel(
                    inventory = inventory,
                    itemCatalog = catalog,
                    playerCoins = Coins(),
                    onUseItem = {},
                    onClose = {}
                )
            }
        }

        composeRule.onNodeWithText("Gold Ring").assertIsDisplayed()
        composeRule.onNodeWithText("Health Potion").assertDoesNotExist()
    }
}

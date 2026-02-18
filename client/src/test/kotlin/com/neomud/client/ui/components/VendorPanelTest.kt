package com.neomud.client.ui.components

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.neomud.client.testutil.CoilTestRule
import com.neomud.client.testutil.TestData
import com.neomud.client.testutil.TestThemeWrapper
import com.neomud.shared.model.Coins
import com.neomud.shared.model.Item
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VendorPanelTest {

    private val coilRule = CoilTestRule()
    private val composeRule = createComposeRule()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(coilRule).around(composeRule)

    private val catalog = TestData.itemCatalog()

    @Test
    fun `shows vendor name`() {
        composeRule.setContent {
            TestThemeWrapper {
                VendorPanel(
                    vendorInfo = TestData.vendorInfo(vendorName = "Grimjaw's Weapons"),
                    playerLevel = 5,
                    itemCatalog = catalog,
                    onBuy = {},
                    onSell = {},
                    onClose = {}
                )
            }
        }

        composeRule.onNodeWithText("Grimjaw's Weapons").assertIsDisplayed()
    }

    @Test
    fun `buy tab shows vendor items`() {
        val vendorItem = TestData.vendorItem(
            item = Item(
                id = "magic_staff",
                name = "Magic Staff",
                description = "A magical staff",
                type = "weapon",
                slot = "weapon",
                damageBonus = 8,
                value = 200
            ),
            price = Coins(gold = 2)
        )

        composeRule.setContent {
            TestThemeWrapper {
                VendorPanel(
                    vendorInfo = TestData.vendorInfo(items = listOf(vendorItem)),
                    playerLevel = 5,
                    itemCatalog = catalog,
                    onBuy = {},
                    onSell = {},
                    onClose = {}
                )
            }
        }

        composeRule.onNodeWithText("Magic Staff").assertIsDisplayed()
    }

    @Test
    fun `buy callback fires on buy button click`() {
        var boughtItem: String? = null
        val vendorItem = TestData.vendorItem(
            item = TestData.item(id = "cheap_sword", name = "Cheap Sword", value = 10),
            price = Coins(copper = 10)
        )

        composeRule.setContent {
            TestThemeWrapper {
                VendorPanel(
                    vendorInfo = TestData.vendorInfo(
                        items = listOf(vendorItem),
                        playerCoins = Coins(gold = 100)
                    ),
                    playerLevel = 10,
                    itemCatalog = catalog,
                    onBuy = { boughtItem = it },
                    onSell = {},
                    onClose = {}
                )
            }
        }

        // "Buy" appears as both a Tab and a Button; the Button is the second match
        composeRule.onAllNodesWithText("Buy")[1].performClick()
        assert(boughtItem == "cheap_sword") { "Expected cheap_sword, got $boughtItem" }
    }

    @Test
    fun `sell tab shows player inventory items`() {
        val inventory = listOf(
            TestData.inventoryItem(itemId = "health_potion", quantity = 3)
        )

        composeRule.setContent {
            TestThemeWrapper {
                VendorPanel(
                    vendorInfo = TestData.vendorInfo(playerInventory = inventory),
                    playerLevel = 5,
                    itemCatalog = catalog,
                    onBuy = {},
                    onSell = {},
                    onClose = {}
                )
            }
        }

        // Switch to Sell tab
        composeRule.onNodeWithText("Sell").performClick()
        composeRule.onNodeWithText("Health Potion").assertIsDisplayed()
    }

    @Test
    fun `close button fires onClose`() {
        var closed = false
        composeRule.setContent {
            TestThemeWrapper {
                VendorPanel(
                    vendorInfo = TestData.vendorInfo(),
                    playerLevel = 5,
                    itemCatalog = catalog,
                    onBuy = {},
                    onSell = {},
                    onClose = { closed = true }
                )
            }
        }

        composeRule.onNodeWithText("Close").performClick()
        assert(closed) { "Expected onClose to fire" }
    }
}

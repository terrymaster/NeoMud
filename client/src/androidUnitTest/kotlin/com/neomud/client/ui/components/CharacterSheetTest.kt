package com.neomud.client.ui.components

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.neomud.client.testutil.TestData
import com.neomud.client.testutil.TestThemeWrapper
import com.neomud.shared.model.Coins
import com.neomud.shared.model.EffectType
import com.neomud.shared.model.Stats
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CharacterSheetTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val classCatalog = mapOf("warrior" to TestData.characterClassDef())
    private val catalog = TestData.itemCatalog()
    private val skillCatalog = mapOf(
        "BASH" to TestData.skillDef(id = "BASH", name = "Bash"),
        "KICK" to TestData.skillDef(id = "KICK", name = "Kick")
    )

    @Test
    fun `shows character name and level`() {
        val player = TestData.player(name = "Aragorn", level = 10, race = "human")
        composeRule.setContent {
            TestThemeWrapper {
                CharacterSheet(
                    player = player,
                    classCatalog = classCatalog,
                    equipment = emptyMap(),
                    itemCatalog = catalog,
                    activeEffects = emptyList(),
                    playerCoins = Coins(),
                    onClose = {}
                )
            }
        }

        composeRule.onNodeWithText("Aragorn").assertIsDisplayed()
        composeRule.onNodeWithText("Character Sheet").assertIsDisplayed()
    }

    @Test
    fun `shows stats grid`() {
        val player = TestData.player(stats = Stats(strength = 45, agility = 38, intellect = 25, willpower = 30, health = 40, charm = 20))
        composeRule.setContent {
            TestThemeWrapper {
                CharacterSheet(
                    player = player,
                    classCatalog = classCatalog,
                    equipment = emptyMap(),
                    itemCatalog = catalog,
                    activeEffects = emptyList(),
                    playerCoins = Coins(),
                    onClose = {}
                )
            }
        }

        composeRule.onNodeWithText("STR").assertIsDisplayed()
        composeRule.onNodeWithText("45").assertIsDisplayed()
        composeRule.onNodeWithText("AGI").assertIsDisplayed()
        composeRule.onNodeWithText("38").assertIsDisplayed()
    }

    @Test
    fun `shows HP and MP vital bars`() {
        val player = TestData.player(currentHp = 75, maxHp = 100, currentMp = 20, maxMp = 50)
        composeRule.setContent {
            TestThemeWrapper {
                CharacterSheet(
                    player = player,
                    classCatalog = classCatalog,
                    equipment = emptyMap(),
                    itemCatalog = catalog,
                    activeEffects = emptyList(),
                    playerCoins = Coins(),
                    onClose = {}
                )
            }
        }

        composeRule.onNodeWithText("HP: 75/100").assertIsDisplayed()
        composeRule.onNodeWithText("MP: 20/50").assertIsDisplayed()
    }

    @Test
    fun `shows equipment slots with item names`() {
        val equipment = mapOf("weapon" to "iron_sword")
        composeRule.setContent {
            TestThemeWrapper {
                CharacterSheet(
                    player = TestData.player(),
                    classCatalog = classCatalog,
                    equipment = equipment,
                    itemCatalog = catalog,
                    activeEffects = emptyList(),
                    playerCoins = Coins(),
                    onClose = {}
                )
            }
        }

        composeRule.onNodeWithText("Iron Sword").assertExists()
        // Verify "-- empty --" text appears (merged semantics make exact count unreliable)
        composeRule.onNodeWithText("-- empty --").assertExists()
    }

    @Test
    fun `shows skill names from catalog`() {
        composeRule.setContent {
            TestThemeWrapper {
                CharacterSheet(
                    player = TestData.player(),
                    classCatalog = classCatalog,
                    equipment = emptyMap(),
                    itemCatalog = catalog,
                    activeEffects = emptyList(),
                    playerCoins = Coins(),
                    skillCatalog = skillCatalog,
                    onClose = {}
                )
            }
        }

        composeRule.onNodeWithText("Bash").assertIsDisplayed()
        composeRule.onNodeWithText("Kick").assertIsDisplayed()
    }

    @Test
    fun `shows active effects with tick counts`() {
        val effects = listOf(
            TestData.activeEffect(name = "Poison", type = EffectType.POISON, remainingTicks = 5)
        )
        composeRule.setContent {
            TestThemeWrapper {
                CharacterSheet(
                    player = TestData.player(),
                    classCatalog = classCatalog,
                    equipment = emptyMap(),
                    itemCatalog = catalog,
                    activeEffects = effects,
                    playerCoins = Coins(),
                    onClose = {}
                )
            }
        }

        composeRule.onNodeWithText("Poison").assertIsDisplayed()
        composeRule.onNodeWithText("5 ticks").assertIsDisplayed()
    }

    @Test
    fun `shows coins section`() {
        composeRule.setContent {
            TestThemeWrapper {
                CharacterSheet(
                    player = TestData.player(),
                    classCatalog = classCatalog,
                    equipment = emptyMap(),
                    itemCatalog = catalog,
                    activeEffects = emptyList(),
                    playerCoins = TestData.coins(gold = 5, silver = 20),
                    onClose = {}
                )
            }
        }

        composeRule.onNodeWithText("5 GP").assertIsDisplayed()
        composeRule.onNodeWithText("20 SP").assertIsDisplayed()
    }

    @Test
    fun `shows no coins message when empty`() {
        composeRule.setContent {
            TestThemeWrapper {
                CharacterSheet(
                    player = TestData.player(),
                    classCatalog = classCatalog,
                    equipment = emptyMap(),
                    itemCatalog = catalog,
                    activeEffects = emptyList(),
                    playerCoins = Coins(),
                    onClose = {}
                )
            }
        }

        composeRule.onNodeWithText("No coins").assertIsDisplayed()
    }

    @Test
    fun `close button fires onClose`() {
        var closed = false
        composeRule.setContent {
            TestThemeWrapper {
                CharacterSheet(
                    player = TestData.player(),
                    classCatalog = classCatalog,
                    equipment = emptyMap(),
                    itemCatalog = catalog,
                    activeEffects = emptyList(),
                    playerCoins = Coins(),
                    onClose = { closed = true }
                )
            }
        }

        composeRule.onNodeWithText("X").performClick()
        assert(closed) { "Expected onClose to fire" }
    }
}

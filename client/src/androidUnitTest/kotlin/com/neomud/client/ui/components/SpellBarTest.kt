package com.neomud.client.ui.components

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.neomud.client.testutil.TestData
import com.neomud.client.testutil.TestThemeWrapper
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SpellBarTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val fireball = TestData.spellDef(id = "fireball", name = "Fireball", school = "mage", manaCost = 10)
    private val heal = TestData.spellDef(id = "heal", name = "Heal", school = "priest", manaCost = 15)
    private val catalog = mapOf("fireball" to fireball, "heal" to heal)

    @Test
    fun `empty slots show slot numbers`() {
        composeRule.setContent {
            TestThemeWrapper {
                SpellBar(
                    spellSlots = listOf(null, null, null, null),
                    spellCatalog = catalog,
                    readiedSpellId = null,
                    currentMp = 50,
                    onReadySpell = {},
                    onOpenSpellPicker = {}
                )
            }
        }

        composeRule.onNodeWithText("1").assertIsDisplayed()
        composeRule.onNodeWithText("2").assertIsDisplayed()
        composeRule.onNodeWithText("3").assertIsDisplayed()
        composeRule.onNodeWithText("4").assertIsDisplayed()
    }

    @Test
    fun `filled slot shows mana cost`() {
        composeRule.setContent {
            TestThemeWrapper {
                SpellBar(
                    spellSlots = listOf("fireball", null, null, null),
                    spellCatalog = catalog,
                    readiedSpellId = null,
                    currentMp = 50,
                    onReadySpell = {},
                    onOpenSpellPicker = {}
                )
            }
        }

        // Mana cost "10" for fireball
        composeRule.onNodeWithText("10").assertIsDisplayed()
        // Slot 1 number should not be visible since it's filled
        composeRule.onNodeWithText("1").assertDoesNotExist()
    }

    @Test
    fun `tapping filled slot calls onReadySpell with correct index`() {
        var readiedIndex: Int? = null
        composeRule.setContent {
            TestThemeWrapper {
                SpellBar(
                    spellSlots = listOf("fireball", null, null, null),
                    spellCatalog = catalog,
                    readiedSpellId = null,
                    currentMp = 50,
                    onReadySpell = { readiedIndex = it },
                    onOpenSpellPicker = {}
                )
            }
        }

        // Click on the mana cost text of the fireball slot
        composeRule.onNodeWithText("10").performClick()
        assert(readiedIndex == 0) { "Expected index 0, got $readiedIndex" }
    }

    @Test
    fun `tapping empty slot calls onOpenSpellPicker`() {
        var pickerIndex: Int? = null
        composeRule.setContent {
            TestThemeWrapper {
                SpellBar(
                    spellSlots = listOf(null, null, null, null),
                    spellCatalog = catalog,
                    readiedSpellId = null,
                    currentMp = 50,
                    onReadySpell = {},
                    onOpenSpellPicker = { pickerIndex = it }
                )
            }
        }

        composeRule.onNodeWithText("2").performClick()
        assert(pickerIndex == 1) { "Expected index 1, got $pickerIndex" }
    }

    @Test
    fun `multiple spells show their school icons`() {
        composeRule.setContent {
            TestThemeWrapper {
                SpellBar(
                    spellSlots = listOf("fireball", "heal", null, null),
                    spellCatalog = catalog,
                    readiedSpellId = null,
                    currentMp = 50,
                    onReadySpell = {},
                    onOpenSpellPicker = {}
                )
            }
        }

        // Mage school icon: ✨ (U+2728), Priest school icon: ✡ (U+2721)
        composeRule.onNodeWithText("\u2728").assertIsDisplayed()
        composeRule.onNodeWithText("\u2721").assertIsDisplayed()
    }
}

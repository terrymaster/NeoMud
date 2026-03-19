package com.neomud.client.ui.components

import androidx.compose.ui.test.*
import com.neomud.client.testutil.ComposeTestBase
import com.neomud.client.testutil.TestData
import com.neomud.client.testutil.TestThemeWrapper
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class SpellBarTest : ComposeTestBase() {

    private val fireball = TestData.spellDef(id = "fireball", name = "Fireball", school = "mage", manaCost = 10)
    private val heal = TestData.spellDef(id = "heal", name = "Heal", school = "priest", manaCost = 15)
    private val catalog = mapOf("fireball" to fireball, "heal" to heal)

    @Test
    fun empty_slots_show_default_icon() = runComposeUiTest {
        setContent {
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

        // Empty slots now render MudIcons.SchoolDefault with contentDescription "Empty spell slot"
        onAllNodesWithContentDescription("Empty spell slot").assertCountEquals(4)
    }

    @Test
    fun filled_slot_shows_mana_cost() = runComposeUiTest {
        setContent {
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
        onNodeWithText("10").assertExists()
    }

    @Test
    fun tapping_filled_slot_calls_onReadySpell_with_correct_index() = runComposeUiTest {
        var readiedIndex: Int? = null
        setContent {
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
        onNodeWithText("10").performClick()
        assertEquals(0, readiedIndex)
    }

    @Test
    fun tapping_empty_slot_calls_onOpenSpellPicker() = runComposeUiTest {
        var pickerIndex: Int? = null
        setContent {
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

        // Click the second empty slot icon
        onAllNodesWithContentDescription("Empty spell slot")[1].performClick()
        assertEquals(1, pickerIndex)
    }

    @Test
    fun multiple_spells_show_their_spell_icons() = runComposeUiTest {
        setContent {
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

        // Spells now use Material Icons with contentDescription = spell.name
        onNodeWithContentDescription("Fireball").assertExists()
        onNodeWithContentDescription("Heal").assertExists()
    }
}

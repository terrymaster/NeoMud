package com.neomud.client.ui.components

import androidx.compose.ui.test.*
import com.neomud.client.testutil.ComposeTestBase
import com.neomud.client.testutil.TestData
import com.neomud.client.testutil.TestThemeWrapper
import com.neomud.client.testutil.installTestCoil
import com.neomud.client.testutil.resetTestCoil
import com.neomud.shared.model.Coins
import com.neomud.shared.model.Npc
import com.neomud.shared.model.SpellType
import com.neomud.shared.model.TargetType
import kotlin.test.BeforeTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class SpriteOverlayTest : ComposeTestBase() {

    @BeforeTest
    fun setup() { installTestCoil() }

    @AfterTest
    fun teardown() { resetTestCoil() }

    private val fireball = TestData.spellDef(
        id = "fireball", name = "Fireball", school = "mage",
        spellType = SpellType.DAMAGE, targetType = TargetType.ENEMY
    )
    private val frostbolt = TestData.spellDef(
        id = "frostbolt", name = "Frostbolt", school = "mage",
        spellType = SpellType.DAMAGE, targetType = TargetType.ENEMY
    )
    private val mageShield = TestData.spellDef(
        id = "mage_shield", name = "Mage Shield", school = "mage",
        spellType = SpellType.BUFF, targetType = TargetType.SELF
    )
    private val spellCatalog = mapOf(
        "fireball" to fireball,
        "frostbolt" to frostbolt,
        "mage_shield" to mageShield
    )

    private val mageClass = TestData.characterClassDef(
        id = "mage", name = "Mage",
        skills = emptyList(),
        magicSchools = mapOf("mage" to 1)
    )
    private val warriorClass = TestData.characterClassDef(
        id = "warrior", name = "Warrior",
        skills = listOf("BASH"),
        magicSchools = emptyMap()
    )
    private val classCatalog = mapOf("mage" to mageClass, "warrior" to warriorClass)

    private val hostileNpc = TestData.npc(id = "goblin_01", name = "Goblin", hostile = true)
    private val friendlyNpc = TestData.npc(id = "merchant_01", name = "Merchant", hostile = false)

    private fun ComposeUiTest.setOverlay(
        npcs: List<Npc> = listOf(hostileNpc),
        spellSlots: List<String?> = listOf("fireball", "frostbolt", "mage_shield", null),
        playerCharacterClass: String? = "mage",
        onAttackTarget: (String) -> Unit = {},
        onCastSpell: (String, String) -> Unit = { _, _ -> },
        onSelectTarget: (String?) -> Unit = {}
    ) {
        setContent {
            TestThemeWrapper {
                SpriteOverlay(
                    npcs = npcs,
                    groundItems = emptyList(),
                    groundCoins = Coins(),
                    itemCatalog = emptyMap(),
                    selectedTargetId = null,
                    onSelectTarget = onSelectTarget,
                    onPickupItem = { _, _ -> },
                    onPickupCoins = {},
                    spellSlots = spellSlots,
                    spellCatalog = spellCatalog,
                    classCatalog = classCatalog,
                    playerCharacterClass = playerCharacterClass,
                    onAttackTarget = onAttackTarget,
                    onCastSpell = onCastSpell
                )
            }
        }
    }

    @Test
    fun long_press_on_hostile_NPC_shows_context_menu_with_attack_button() = runComposeUiTest {
        setOverlay()

        onNodeWithContentDescription("Goblin").performTouchInput { longClick() }

        onNodeWithContentDescription("Attack").assertIsDisplayed()
    }

    @Test
    fun context_menu_shows_spell_abbreviations_for_mage() = runComposeUiTest {
        setOverlay()

        onNodeWithContentDescription("Goblin").performTouchInput { longClick() }

        onNodeWithText("Fi").assertIsDisplayed()
        onNodeWithText("Fr").assertIsDisplayed()
    }

    @Test
    fun context_menu_excludes_self_targeted_spells() = runComposeUiTest {
        setOverlay()

        onNodeWithContentDescription("Goblin").performTouchInput { longClick() }

        onNodeWithText("Ma").assertDoesNotExist()
    }

    @Test
    fun warrior_sees_only_attack_button_no_spell_buttons() = runComposeUiTest {
        setOverlay(playerCharacterClass = "warrior", spellSlots = listOf("fireball", null, null, null))

        onNodeWithContentDescription("Goblin").performTouchInput { longClick() }

        onNodeWithContentDescription("Attack").assertIsDisplayed()
        onNodeWithText("Fi").assertDoesNotExist()
    }

    @Test
    fun tapping_attack_button_calls_onAttackTarget() = runComposeUiTest {
        var attackedNpcId: String? = null
        setOverlay(onAttackTarget = { attackedNpcId = it })

        onNodeWithContentDescription("Goblin").performTouchInput { longClick() }
        onNodeWithContentDescription("Attack").performClick()

        assertEquals("goblin_01", attackedNpcId)
    }

    @Test
    fun tapping_spell_button_calls_onCastSpell_with_correct_ids() = runComposeUiTest {
        var castSpellId: String? = null
        var castTargetId: String? = null
        setOverlay(onCastSpell = { spellId, targetId ->
            castSpellId = spellId
            castTargetId = targetId
        })

        onNodeWithContentDescription("Goblin").performTouchInput { longClick() }
        onNodeWithText("Fi").performClick()

        assertEquals("fireball", castSpellId)
        assertEquals("goblin_01", castTargetId)
    }

    @Test
    fun tapping_attack_button_dismisses_context_menu() = runComposeUiTest {
        setOverlay()

        onNodeWithContentDescription("Goblin").performTouchInput { longClick() }
        onNodeWithContentDescription("Attack").assertIsDisplayed()

        onNodeWithContentDescription("Attack").performClick()

        onNodeWithText("Fi").assertDoesNotExist()
    }

    @Test
    fun tapping_spell_button_dismisses_context_menu() = runComposeUiTest {
        setOverlay()

        onNodeWithContentDescription("Goblin").performTouchInput { longClick() }
        onNodeWithText("Fi").performClick()

        onNodeWithText("Fr").assertDoesNotExist()
    }

    @Test
    fun normal_tap_on_hostile_NPC_selects_target_without_showing_menu() = runComposeUiTest {
        var selectedId: String? = null
        setOverlay(onSelectTarget = { selectedId = it })

        onNodeWithContentDescription("Goblin").performClick()

        assertEquals("goblin_01", selectedId)
        onNodeWithContentDescription("Attack").assertDoesNotExist()
    }
}

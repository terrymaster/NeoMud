package com.neomud.client.ui.components

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.neomud.client.testutil.CoilTestRule
import com.neomud.client.testutil.TestData
import com.neomud.client.testutil.TestThemeWrapper
import com.neomud.shared.model.Coins
import com.neomud.shared.model.SpellType
import com.neomud.shared.model.TargetType
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SpriteOverlayTest {

    private val coilRule = CoilTestRule()
    private val composeRule = createComposeRule()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(coilRule).around(composeRule)

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

    private fun setOverlay(
        npcs: List<com.neomud.shared.model.Npc> = listOf(hostileNpc),
        spellSlots: List<String?> = listOf("fireball", "frostbolt", "mage_shield", null),
        playerCharacterClass: String? = "mage",
        onAttackTarget: (String) -> Unit = {},
        onCastSpell: (String, String) -> Unit = { _, _ -> },
        onSelectTarget: (String?) -> Unit = {}
    ) {
        composeRule.setContent {
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
    fun `long press on hostile NPC shows context menu with attack button`() {
        setOverlay()

        // Long-press the NPC sprite
        composeRule.onNodeWithContentDescription("Goblin").performTouchInput { longClick() }

        // Attack sword icon should be visible
        composeRule.onNodeWithText("\u2694\uFE0F").assertIsDisplayed()
    }

    @Test
    fun `context menu shows spell abbreviations for mage`() {
        setOverlay()

        composeRule.onNodeWithContentDescription("Goblin").performTouchInput { longClick() }

        // "Fi" for Fireball, "Fr" for Frostbolt
        composeRule.onNodeWithText("Fi").assertIsDisplayed()
        composeRule.onNodeWithText("Fr").assertIsDisplayed()
    }

    @Test
    fun `context menu excludes self-targeted spells`() {
        setOverlay()

        composeRule.onNodeWithContentDescription("Goblin").performTouchInput { longClick() }

        // "Ma" for Mage Shield (SELF) should NOT appear
        composeRule.onNodeWithText("Ma").assertDoesNotExist()
    }

    @Test
    fun `warrior sees only attack button no spell buttons`() {
        setOverlay(playerCharacterClass = "warrior", spellSlots = listOf("fireball", null, null, null))

        composeRule.onNodeWithContentDescription("Goblin").performTouchInput { longClick() }

        composeRule.onNodeWithText("\u2694\uFE0F").assertIsDisplayed()
        composeRule.onNodeWithText("Fi").assertDoesNotExist()
    }

    @Test
    fun `tapping attack button calls onAttackTarget`() {
        var attackedNpcId: String? = null
        setOverlay(onAttackTarget = { attackedNpcId = it })

        composeRule.onNodeWithContentDescription("Goblin").performTouchInput { longClick() }
        composeRule.onNodeWithText("\u2694\uFE0F").performClick()

        assert(attackedNpcId == "goblin_01") { "Expected goblin_01, got $attackedNpcId" }
    }

    @Test
    fun `tapping spell button calls onCastSpell with correct ids`() {
        var castSpellId: String? = null
        var castTargetId: String? = null
        setOverlay(onCastSpell = { spellId, targetId ->
            castSpellId = spellId
            castTargetId = targetId
        })

        composeRule.onNodeWithContentDescription("Goblin").performTouchInput { longClick() }
        composeRule.onNodeWithText("Fi").performClick()

        assert(castSpellId == "fireball") { "Expected fireball, got $castSpellId" }
        assert(castTargetId == "goblin_01") { "Expected goblin_01, got $castTargetId" }
    }

    @Test
    fun `tapping attack button dismisses context menu`() {
        setOverlay()

        composeRule.onNodeWithContentDescription("Goblin").performTouchInput { longClick() }
        composeRule.onNodeWithText("\u2694\uFE0F").assertIsDisplayed()

        composeRule.onNodeWithText("\u2694\uFE0F").performClick()

        // Menu should be gone
        composeRule.onNodeWithText("Fi").assertDoesNotExist()
    }

    @Test
    fun `tapping spell button dismisses context menu`() {
        setOverlay()

        composeRule.onNodeWithContentDescription("Goblin").performTouchInput { longClick() }
        composeRule.onNodeWithText("Fi").performClick()

        composeRule.onNodeWithText("Fr").assertDoesNotExist()
    }

    @Test
    fun `normal tap on hostile NPC selects target without showing menu`() {
        var selectedId: String? = null
        setOverlay(onSelectTarget = { selectedId = it })

        composeRule.onNodeWithContentDescription("Goblin").performClick()

        // Should select, not show menu
        assert(selectedId == "goblin_01") { "Expected goblin_01, got $selectedId" }
        composeRule.onNodeWithText("\u2694\uFE0F").assertDoesNotExist()
    }
}

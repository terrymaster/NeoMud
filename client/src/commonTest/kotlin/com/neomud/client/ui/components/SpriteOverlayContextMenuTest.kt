package com.neomud.client.ui.components

import com.neomud.client.testutil.TestData
import com.neomud.shared.model.SpellType
import com.neomud.shared.model.TargetType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for the targetable-spell filtering logic used by the NPC context menu.
 */
class SpriteOverlayContextMenuTest {

    private val fireball = TestData.spellDef(
        id = "fireball", name = "Fireball", school = "mage",
        spellType = SpellType.DAMAGE, targetType = TargetType.ENEMY
    )
    private val frostbolt = TestData.spellDef(
        id = "frostbolt", name = "Frostbolt", school = "mage",
        spellType = SpellType.DAMAGE, targetType = TargetType.ENEMY
    )
    private val heal = TestData.spellDef(
        id = "heal", name = "Heal", school = "priest",
        spellType = SpellType.HEAL, targetType = TargetType.SELF
    )
    private val smite = TestData.spellDef(
        id = "smite", name = "Smite", school = "priest",
        spellType = SpellType.DAMAGE, targetType = TargetType.ENEMY
    )
    private val mageShield = TestData.spellDef(
        id = "mage_shield", name = "Mage Shield", school = "mage",
        spellType = SpellType.BUFF, targetType = TargetType.SELF
    )
    private val druidWrath = TestData.spellDef(
        id = "druid_wrath", name = "Druid Wrath", school = "druid",
        spellType = SpellType.DAMAGE, targetType = TargetType.ENEMY
    )

    private val catalog = mapOf(
        "fireball" to fireball,
        "frostbolt" to frostbolt,
        "heal" to heal,
        "smite" to smite,
        "mage_shield" to mageShield,
        "druid_wrath" to druidWrath
    )

    private val mageClass = TestData.characterClassDef(
        id = "mage", name = "Mage",
        skills = emptyList(),
        magicSchools = mapOf("mage" to 1)
    )
    private val priestClass = TestData.characterClassDef(
        id = "priest", name = "Priest",
        skills = emptyList(),
        magicSchools = mapOf("priest" to 1)
    )
    private val warriorClass = TestData.characterClassDef(
        id = "warrior", name = "Warrior",
        skills = listOf("BASH", "KICK"),
        magicSchools = emptyMap()
    )
    private val classCatalog = mapOf(
        "mage" to mageClass,
        "priest" to priestClass,
        "warrior" to warriorClass
    )

    // --- targetableSlottedSpells filtering ---

    @Test
    fun mage_sees_enemy_targeted_mage_spells() {
        val result = targetableSlottedSpells(
            spellSlots = listOf("fireball", null, null, null),
            spellCatalog = catalog,
            classCatalog = classCatalog,
            playerCharacterClass = "mage"
        )

        assertEquals(1, result.size)
        assertEquals("fireball", result[0].id)
    }

    @Test
    fun self_targeted_spells_are_excluded() {
        val result = targetableSlottedSpells(
            spellSlots = listOf("heal", "mage_shield", null, null),
            spellCatalog = catalog,
            classCatalog = classCatalog,
            playerCharacterClass = "mage"
        )

        assertTrue(result.isEmpty(), "Self-targeted spells should be excluded")
    }

    @Test
    fun priest_sees_only_targetable_priest_spells_not_self_spells() {
        val result = targetableSlottedSpells(
            spellSlots = listOf("heal", "smite", "fireball", null),
            spellCatalog = catalog,
            classCatalog = classCatalog,
            playerCharacterClass = "priest"
        )

        // heal is SELF → excluded, fireball is mage school → excluded
        assertEquals(1, result.size)
        assertEquals("smite", result[0].id)
    }

    @Test
    fun warrior_with_no_magic_schools_sees_no_spells() {
        val result = targetableSlottedSpells(
            spellSlots = listOf("fireball", "smite", null, null),
            spellCatalog = catalog,
            classCatalog = classCatalog,
            playerCharacterClass = "warrior"
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun null_player_class_returns_empty_list() {
        val result = targetableSlottedSpells(
            spellSlots = listOf("fireball", null, null, null),
            spellCatalog = catalog,
            classCatalog = classCatalog,
            playerCharacterClass = null
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun empty_spell_slots_returns_empty_list() {
        val result = targetableSlottedSpells(
            spellSlots = listOf(null, null, null, null),
            spellCatalog = catalog,
            classCatalog = classCatalog,
            playerCharacterClass = "mage"
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun duplicate_spell_ids_in_slots_are_deduplicated() {
        val result = targetableSlottedSpells(
            spellSlots = listOf("fireball", "fireball", null, null),
            spellCatalog = catalog,
            classCatalog = classCatalog,
            playerCharacterClass = "mage"
        )

        assertEquals(1, result.size)
    }

    @Test
    fun multiple_targetable_spells_from_same_school_all_returned() {
        val result = targetableSlottedSpells(
            spellSlots = listOf("fireball", "frostbolt", null, null),
            spellCatalog = catalog,
            classCatalog = classCatalog,
            playerCharacterClass = "mage"
        )

        assertEquals(2, result.size)
        assertEquals(setOf("fireball", "frostbolt"), result.map { it.id }.toSet())
    }

    @Test
    fun spells_from_wrong_school_are_excluded() {
        val result = targetableSlottedSpells(
            spellSlots = listOf("fireball", "druid_wrath", null, null),
            spellCatalog = catalog,
            classCatalog = classCatalog,
            playerCharacterClass = "mage"
        )

        assertEquals(1, result.size)
        assertEquals("fireball", result[0].id)
    }

    @Test
    fun unknown_spell_id_in_slot_is_skipped_gracefully() {
        val result = targetableSlottedSpells(
            spellSlots = listOf("fireball", "nonexistent_spell", null, null),
            spellCatalog = catalog,
            classCatalog = classCatalog,
            playerCharacterClass = "mage"
        )

        assertEquals(1, result.size)
        assertEquals("fireball", result[0].id)
    }
}

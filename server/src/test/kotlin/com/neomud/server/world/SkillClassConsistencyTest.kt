package com.neomud.server.world

import com.neomud.server.defaultWorldSource
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Validates that skill classRestrictions in skills.json are consistent
 * with the skills lists in classes.json, and that spell schoolLevel
 * requirements are enforceable.
 */
class SkillClassConsistencyTest {

    private val worldSource = defaultWorldSource()
    private val skillCatalog = SkillCatalog.load(worldSource)
    private val classCatalog = ClassCatalog.load(worldSource)
    private val spellCatalog = SpellCatalog.load(worldSource)

    @Test
    fun `skill classRestrictions match class skills lists`() {
        val errors = mutableListOf<String>()

        // For each class, check that every skill in its skills list includes
        // that class in the skill's classRestrictions (or has no restrictions)
        for (classDef in classCatalog.getAllClasses()) {
            for (skillId in classDef.skills) {
                val skill = skillCatalog.getSkill(skillId)
                if (skill == null) {
                    errors.add("Class ${classDef.id} references unknown skill $skillId")
                    continue
                }
                if (skill.classRestrictions.isNotEmpty() && classDef.id !in skill.classRestrictions) {
                    errors.add("Class ${classDef.id} lists skill ${skill.id} but is not in its classRestrictions: ${skill.classRestrictions}")
                }
            }
        }

        // Reverse check: for each skill with classRestrictions, verify each
        // listed class actually has that skill in its skills list
        for (skill in skillCatalog.getAllSkills()) {
            for (classId in skill.classRestrictions) {
                val classDef = classCatalog.getClass(classId)
                if (classDef == null) {
                    errors.add("Skill ${skill.id} references unknown class $classId")
                    continue
                }
                if (skill.id !in classDef.skills) {
                    errors.add("Skill ${skill.id} lists class $classId in classRestrictions but class doesn't have it in skills: ${classDef.skills}")
                }
            }
        }

        assertTrue(errors.isEmpty(), "Skill/class consistency errors:\n${errors.joinToString("\n")}")
    }

    @Test
    fun `spell schoolLevel requirements are satisfiable by at least one class`() {
        val knownOrphans = emptySet<String>()

        val errors = mutableListOf<String>()
        val allClasses = classCatalog.getAllClasses()

        for (spell in spellCatalog.getAllSpells()) {
            if (spell.id in knownOrphans) continue
            val canCast = allClasses.any { classDef ->
                val schoolLevel = classDef.magicSchools[spell.school]
                schoolLevel != null && schoolLevel >= spell.schoolLevel
            }
            if (!canCast) {
                errors.add("Spell ${spell.id} (school=${spell.school}, schoolLevel=${spell.schoolLevel}) cannot be cast by any class")
            }
        }

        assertTrue(errors.isEmpty(), "Orphaned spells:\n${errors.joinToString("\n")}")
    }

    @Test
    fun `classes with magicSchools can cast at least one spell`() {
        val errors = mutableListOf<String>()
        val allSpells = spellCatalog.getAllSpells()

        for (classDef in classCatalog.getAllClasses()) {
            for ((school, level) in classDef.magicSchools) {
                val castable = allSpells.any { it.school == school && it.schoolLevel <= level }
                if (!castable) {
                    errors.add("Class ${classDef.id} has $school at level $level but no spells are castable")
                }
            }
        }

        assertTrue(errors.isEmpty(), "Classes with no castable spells:\n${errors.joinToString("\n")}")
    }

    @Test
    fun `high-tier spells are correctly gated from low-tier classes`() {
        // Ranger has druid:1, should NOT be able to cast Nature's Wrath (druid, schoolLevel:3)
        val ranger = classCatalog.getClass("RANGER")!!
        val naturesWrath = spellCatalog.getSpell("NATURES_WRATH")!!
        val rangerDruidLevel = ranger.magicSchools["druid"]!!
        assertTrue(
            rangerDruidLevel < naturesWrath.schoolLevel,
            "Ranger (druid:$rangerDruidLevel) should not meet Nature's Wrath schoolLevel (${naturesWrath.schoolLevel})"
        )

        // Gypsy has mage:1, should NOT be able to cast Fireball (mage, schoolLevel:3)
        val gypsy = classCatalog.getClass("GYPSY")!!
        val fireball = spellCatalog.getSpell("FIREBALL")!!
        val gypsyMageLevel = gypsy.magicSchools["mage"]!!
        assertTrue(
            gypsyMageLevel < fireball.schoolLevel,
            "Gypsy (mage:$gypsyMageLevel) should not meet Fireball schoolLevel (${fireball.schoolLevel})"
        )

        // Mage has mage:3, SHOULD be able to cast Fireball
        val mage = classCatalog.getClass("MAGE")!!
        val mageMageLevel = mage.magicSchools["mage"]!!
        assertTrue(
            mageMageLevel >= fireball.schoolLevel,
            "Mage (mage:$mageMageLevel) should meet Fireball schoolLevel (${fireball.schoolLevel})"
        )
    }
}
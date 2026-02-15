package com.neomud.shared

import com.neomud.shared.model.SkillDef
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class SkillSerializationTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun testSkillDefRoundTrip() {
        val original = SkillDef(
            id = "BACKSTAB",
            name = "Backstab",
            description = "Strike from the shadows for massive damage.",
            category = "combat",
            primaryStat = "dexterity",
            secondaryStat = "strength",
            cooldownTicks = 4,
            manaCost = 0,
            difficulty = 13,
            isPassive = false,
            classRestrictions = listOf("ROGUE"),
            properties = mapOf("damageMultiplier" to "3")
        )

        val encoded = json.encodeToString(SkillDef.serializer(), original)
        val decoded = json.decodeFromString(SkillDef.serializer(), encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun testSkillDefMinimalFieldsRoundTrip() {
        val original = SkillDef(
            id = "DODGE",
            name = "Dodge",
            description = "Passively increases chance to avoid attacks.",
            category = "defense",
            primaryStat = "dexterity",
            secondaryStat = "wisdom"
        )

        val encoded = json.encodeToString(SkillDef.serializer(), original)
        val decoded = json.decodeFromString(SkillDef.serializer(), encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun testSkillDefListRoundTrip() {
        val skills = listOf(
            SkillDef("HIDE", "Hide", "Slip into the shadows.", "stealth", "dexterity", "intelligence", cooldownTicks = 2, difficulty = 15),
            SkillDef("BACKSTAB", "Backstab", "Strike from shadows.", "combat", "dexterity", "strength", cooldownTicks = 4)
        )

        val encoded = json.encodeToString(kotlinx.serialization.builtins.ListSerializer(SkillDef.serializer()), skills)
        val decoded = json.decodeFromString(kotlinx.serialization.builtins.ListSerializer(SkillDef.serializer()), encoded)
        assertEquals(skills, decoded)
    }
}

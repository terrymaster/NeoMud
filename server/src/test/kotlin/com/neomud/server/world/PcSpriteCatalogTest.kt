package com.neomud.server.world

import com.neomud.server.defaultWorldSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PcSpriteCatalogTest {

    private fun load() = PcSpriteCatalog.load(defaultWorldSource())

    @Test
    fun testLoadSpritesFromJson() {
        val catalog = load()
        assertEquals(270, catalog.spriteCount, "Should load all 270 PC sprite definitions (6 races x 3 genders x 15 classes)")
    }

    @Test
    fun testLookupById() {
        val catalog = load()
        val sprite = catalog.getSprite("human_male_warrior")
        assertNotNull(sprite)
        assertEquals("HUMAN", sprite.race)
        assertEquals("male", sprite.gender)
        assertEquals("WARRIOR", sprite.characterClass)
        assertTrue(sprite.imagePrompt.isNotBlank(), "Should have a non-empty imagePrompt")
        assertTrue(sprite.imageStyle.isNotBlank(), "Should have a non-empty imageStyle")
        assertTrue(sprite.imageNegativePrompt.isNotBlank(), "Should have a non-empty imageNegativePrompt")
    }

    @Test
    fun testGetSpriteFor() {
        val catalog = load()
        val sprite = catalog.getSpriteFor("DWARF", "female", "BARD")
        assertNotNull(sprite)
        assertEquals("dwarf_female_bard", sprite.id)
        assertTrue(sprite.imagePrompt.contains("dwarf", ignoreCase = true))
        assertTrue(sprite.imagePrompt.contains("bard", ignoreCase = true))
    }

    @Test
    fun testGetSpriteForCaseInsensitive() {
        val catalog = load()
        val sprite = catalog.getSpriteFor("ELF", "Male", "RANGER")
        assertNotNull(sprite)
        assertEquals("elf_male_ranger", sprite.id)
    }

    @Test
    fun testUnknownSpriteReturnsNull() {
        val catalog = load()
        assertNull(catalog.getSprite("nonexistent_combo"))
        assertNull(catalog.getSpriteFor("ALIEN", "male", "WARRIOR"))
    }

    @Test
    fun testAllSpritesHaveImagePrompts() {
        val catalog = load()
        val races = listOf("DWARF", "ELF", "HALFLING", "GNOME", "HALF_ORC", "HUMAN")
        val genders = listOf("male", "female", "neutral")
        val classes = listOf("BARD", "CLERIC", "DRUID", "GYPSY", "MAGE", "MISSIONARY", "MYSTIC",
            "NINJA", "PALADIN", "PRIEST", "RANGER", "THIEF", "WARLOCK", "WARRIOR", "WITCHHUNTER")

        for (race in races) {
            for (gender in genders) {
                for (cls in classes) {
                    val sprite = catalog.getSpriteFor(race, gender, cls)
                    assertNotNull(sprite, "Missing sprite for $race/$gender/$cls")
                    assertTrue(sprite.imagePrompt.isNotBlank(), "Empty imagePrompt for $race/$gender/$cls")
                }
            }
        }
    }
}

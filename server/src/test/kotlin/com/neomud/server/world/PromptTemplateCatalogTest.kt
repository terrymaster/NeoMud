package com.neomud.server.world

import com.neomud.server.defaultWorldSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PromptTemplateCatalogTest {

    private fun load() = PromptTemplateCatalog.load(defaultWorldSource())

    @Test
    fun testLoadPromptTemplates() {
        val catalog = load()
        assertEquals(29, catalog.templateCount, "Should load 29 prompt templates")
    }

    @Test
    fun testGetRoomTemplate() {
        val catalog = load()
        val template = catalog.getTemplate("room:town:square")
        assertNotNull(template)
        assertEquals("room", template.entityType)
        assertEquals("town:square", template.entityId)
        assertTrue(template.prompt.isNotEmpty())
        assertEquals(1024, template.width)
        assertEquals(576, template.height)
    }

    @Test
    fun testGetNpcTemplate() {
        val catalog = load()
        val template = catalog.getTemplate("npc:npc:shadow_wolf")
        assertNotNull(template)
        assertEquals("npc", template.entityType)
        assertEquals("npc:shadow_wolf", template.entityId)
    }

    @Test
    fun testGetItemTemplate() {
        val catalog = load()
        val template = catalog.getTemplate("item:item:iron_sword")
        assertNotNull(template)
        assertEquals("item", template.entityType)
        assertEquals("item:iron_sword", template.entityId)
    }

    @Test
    fun testGetTemplatesByType() {
        val catalog = load()
        val rooms = catalog.getTemplatesByType("room")
        assertEquals(10, rooms.size, "Should have 10 room templates")
        val npcs = catalog.getTemplatesByType("npc")
        assertEquals(5, npcs.size, "Should have 5 NPC templates")
        val items = catalog.getTemplatesByType("item")
        assertEquals(10, items.size, "Should have 10 item templates")
    }

    @Test
    fun testUnknownKeyReturnsNull() {
        val catalog = load()
        assertNull(catalog.getTemplate("room:nonexistent"))
    }
}

package com.neomud.server.world

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PromptTemplateCatalogTest {

    @Test
    fun testLoadPromptTemplates() {
        val catalog = PromptTemplateCatalog.load()
        assertEquals(28, catalog.templateCount, "Should load 28 prompt templates")
    }

    @Test
    fun testGetRoomTemplate() {
        val catalog = PromptTemplateCatalog.load()
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
        val catalog = PromptTemplateCatalog.load()
        val template = catalog.getTemplate("npc:npc:shadow_wolf")
        assertNotNull(template)
        assertEquals("npc", template.entityType)
        assertEquals("npc:shadow_wolf", template.entityId)
    }

    @Test
    fun testGetItemTemplate() {
        val catalog = PromptTemplateCatalog.load()
        val template = catalog.getTemplate("item:item:iron_sword")
        assertNotNull(template)
        assertEquals("item", template.entityType)
        assertEquals("item:iron_sword", template.entityId)
    }

    @Test
    fun testGetTemplatesByType() {
        val catalog = PromptTemplateCatalog.load()
        val rooms = catalog.getTemplatesByType("room")
        assertEquals(10, rooms.size, "Should have 10 room templates")
        val npcs = catalog.getTemplatesByType("npc")
        assertEquals(4, npcs.size, "Should have 4 NPC templates")
        val items = catalog.getTemplatesByType("item")
        assertEquals(10, items.size, "Should have 10 item templates")
    }

    @Test
    fun testUnknownKeyReturnsNull() {
        val catalog = PromptTemplateCatalog.load()
        assertNull(catalog.getTemplate("room:nonexistent"))
    }
}

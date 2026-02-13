package com.neomud.server.game.inventory

import com.neomud.shared.model.Coins
import com.neomud.shared.model.GroundItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RoomItemManagerTest {

    @Test
    fun testEmptyRoomReturnsNoItems() {
        val mgr = RoomItemManager()
        assertTrue(mgr.getGroundItems("room:1").isEmpty())
        assertTrue(mgr.getGroundCoins("room:1").isEmpty())
    }

    @Test
    fun testAddItemsStacks() {
        val mgr = RoomItemManager()
        mgr.addItems("room:1", listOf(GroundItem("item:sword", 1)))
        mgr.addItems("room:1", listOf(GroundItem("item:sword", 2)))

        val items = mgr.getGroundItems("room:1")
        assertEquals(1, items.size)
        assertEquals(3, items[0].quantity)
    }

    @Test
    fun testAddDifferentItems() {
        val mgr = RoomItemManager()
        mgr.addItems("room:1", listOf(
            GroundItem("item:sword", 1),
            GroundItem("item:shield", 2)
        ))

        val items = mgr.getGroundItems("room:1")
        assertEquals(2, items.size)
    }

    @Test
    fun testRemoveItemPartial() {
        val mgr = RoomItemManager()
        mgr.addItems("room:1", listOf(GroundItem("item:sword", 5)))

        val removed = mgr.removeItem("room:1", "item:sword", 3)
        assertEquals(3, removed)

        val items = mgr.getGroundItems("room:1")
        assertEquals(1, items.size)
        assertEquals(2, items[0].quantity)
    }

    @Test
    fun testRemoveItemAll() {
        val mgr = RoomItemManager()
        mgr.addItems("room:1", listOf(GroundItem("item:sword", 3)))

        val removed = mgr.removeItem("room:1", "item:sword", 3)
        assertEquals(3, removed)
        assertTrue(mgr.getGroundItems("room:1").isEmpty())
    }

    @Test
    fun testRemoveItemMoreThanAvailable() {
        val mgr = RoomItemManager()
        mgr.addItems("room:1", listOf(GroundItem("item:sword", 2)))

        val removed = mgr.removeItem("room:1", "item:sword", 10)
        assertEquals(2, removed)
        assertTrue(mgr.getGroundItems("room:1").isEmpty())
    }

    @Test
    fun testRemoveNonExistentItem() {
        val mgr = RoomItemManager()
        val removed = mgr.removeItem("room:1", "item:nope", 1)
        assertEquals(0, removed)
    }

    @Test
    fun testAddAndRemoveCoins() {
        val mgr = RoomItemManager()
        mgr.addCoins("room:1", Coins(copper = 10, silver = 5))

        assertEquals(Coins(copper = 10, silver = 5), mgr.getGroundCoins("room:1"))

        val copperTaken = mgr.removeCoins("room:1", "copper")
        assertEquals(10, copperTaken)
        assertEquals(Coins(copper = 0, silver = 5), mgr.getGroundCoins("room:1"))

        val silverTaken = mgr.removeCoins("room:1", "silver")
        assertEquals(5, silverTaken)
        // Room should be cleaned up since everything is empty
        assertTrue(mgr.getGroundCoins("room:1").isEmpty())
    }

    @Test
    fun testRemoveCoinsFromEmptyRoom() {
        val mgr = RoomItemManager()
        assertEquals(0, mgr.removeCoins("room:1", "gold"))
    }

    @Test
    fun testCoinsAccumulate() {
        val mgr = RoomItemManager()
        mgr.addCoins("room:1", Coins(copper = 5))
        mgr.addCoins("room:1", Coins(copper = 10, gold = 1))

        assertEquals(Coins(copper = 15, gold = 1), mgr.getGroundCoins("room:1"))
    }

    @Test
    fun testSeparateRooms() {
        val mgr = RoomItemManager()
        mgr.addItems("room:1", listOf(GroundItem("item:a", 1)))
        mgr.addItems("room:2", listOf(GroundItem("item:b", 2)))

        assertEquals(1, mgr.getGroundItems("room:1").size)
        assertEquals(1, mgr.getGroundItems("room:2").size)
        assertEquals("item:a", mgr.getGroundItems("room:1")[0].itemId)
        assertEquals("item:b", mgr.getGroundItems("room:2")[0].itemId)
    }
}

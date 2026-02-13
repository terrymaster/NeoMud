package com.neomud.server.game.inventory

import com.neomud.shared.model.Coins
import com.neomud.shared.model.GroundItem
import java.util.concurrent.ConcurrentHashMap

class RoomItemManager {

    private data class RoomGroundState(
        val items: MutableList<GroundItem> = mutableListOf(),
        var coins: Coins = Coins()
    )

    private val rooms = ConcurrentHashMap<String, RoomGroundState>()

    fun addItems(roomId: String, items: List<GroundItem>) {
        val state = rooms.getOrPut(roomId) { RoomGroundState() }
        synchronized(state) {
            for (item in items) {
                val existing = state.items.find { it.itemId == item.itemId }
                if (existing != null) {
                    val idx = state.items.indexOf(existing)
                    state.items[idx] = existing.copy(quantity = existing.quantity + item.quantity)
                } else {
                    state.items.add(item)
                }
            }
        }
    }

    fun addCoins(roomId: String, coins: Coins) {
        if (coins.isEmpty()) return
        val state = rooms.getOrPut(roomId) { RoomGroundState() }
        synchronized(state) {
            state.coins = state.coins + coins
        }
    }

    fun removeItem(roomId: String, itemId: String, quantity: Int): Int {
        val state = rooms[roomId] ?: return 0
        synchronized(state) {
            val existing = state.items.find { it.itemId == itemId } ?: return 0
            val actual = minOf(quantity, existing.quantity)
            if (actual >= existing.quantity) {
                state.items.remove(existing)
            } else {
                val idx = state.items.indexOf(existing)
                state.items[idx] = existing.copy(quantity = existing.quantity - actual)
            }
            cleanupIfEmpty(roomId, state)
            return actual
        }
    }

    fun removeCoins(roomId: String, coinType: String): Int {
        val state = rooms[roomId] ?: return 0
        synchronized(state) {
            val amount = when (coinType) {
                "copper" -> state.coins.copper
                "silver" -> state.coins.silver
                "gold" -> state.coins.gold
                "platinum" -> state.coins.platinum
                else -> return 0
            }
            if (amount == 0) return 0
            state.coins = when (coinType) {
                "copper" -> state.coins.copy(copper = 0)
                "silver" -> state.coins.copy(silver = 0)
                "gold" -> state.coins.copy(gold = 0)
                "platinum" -> state.coins.copy(platinum = 0)
                else -> state.coins
            }
            cleanupIfEmpty(roomId, state)
            return amount
        }
    }

    fun getGroundItems(roomId: String): List<GroundItem> {
        val state = rooms[roomId] ?: return emptyList()
        synchronized(state) {
            return state.items.toList()
        }
    }

    fun getGroundCoins(roomId: String): Coins {
        val state = rooms[roomId] ?: return Coins()
        synchronized(state) {
            return state.coins
        }
    }

    private fun cleanupIfEmpty(roomId: String, state: RoomGroundState) {
        if (state.items.isEmpty() && state.coins.isEmpty()) {
            rooms.remove(roomId)
        }
    }
}

package com.neomud.server.session

import com.neomud.server.persistence.repository.PlayerRepository
import com.neomud.shared.model.*
import com.neomud.shared.protocol.MessageSerializer
import com.neomud.shared.protocol.ServerMessage
import io.ktor.websocket.*

class PlayerSession(
    val webSocketSession: WebSocketSession
) {
    var playerName: String? = null
    var currentRoomId: RoomId? = null
    var player: Player? = null
    val activeEffects: MutableList<ActiveEffect> = mutableListOf()
    var attackMode: Boolean = false
    var selectedTargetId: String? = null
    var isHidden: Boolean = false
    var isMeditating: Boolean = false
    var godMode: Boolean = false
    val skillCooldowns: MutableMap<String, Int> = mutableMapOf()

    // Rate limiting (token bucket)
    private var messageTokens: Double = BURST_CAPACITY.toDouble()
    private var lastRefillTime: Long = System.currentTimeMillis()

    fun tryConsumeMessage(): Boolean {
        val now = System.currentTimeMillis()
        val elapsed = (now - lastRefillTime) / 1000.0
        lastRefillTime = now
        messageTokens = (messageTokens + elapsed * MAX_MESSAGES_PER_SECOND)
            .coerceAtMost(BURST_CAPACITY.toDouble())
        return if (messageTokens >= 1.0) {
            messageTokens -= 1.0
            true
        } else false
    }

    val isAuthenticated: Boolean get() = player != null

    /** Base stats + active buff magnitudes. */
    fun effectiveStats(): Stats {
        val p = player ?: return Stats()
        var str = p.stats.strength
        var agi = p.stats.agility
        var int = p.stats.intellect
        var wil = p.stats.willpower
        for (e in activeEffects) {
            when (e.type) {
                EffectType.BUFF_STRENGTH -> str += e.magnitude
                EffectType.BUFF_AGILITY -> agi += e.magnitude
                EffectType.BUFF_INTELLECT -> int += e.magnitude
                EffectType.BUFF_WILLPOWER -> wil += e.magnitude
                else -> {}
            }
        }
        return p.stats.copy(strength = str, agility = agi, intellect = int, willpower = wil)
    }

    fun toPlayerInfo(): PlayerInfo? {
        val p = player ?: return null
        return PlayerInfo(
            name = p.name,
            characterClass = p.characterClass,
            race = p.race,
            gender = p.gender,
            level = p.level,
            spriteUrl = PlayerRepository.pcSpriteRelativePath(p.race, p.gender, p.characterClass)
        )
    }

    suspend fun send(message: ServerMessage) {
        val text = MessageSerializer.encodeServerMessage(message)
        webSocketSession.send(Frame.Text(text))
    }

    companion object {
        const val MAX_MESSAGES_PER_SECOND = 10
        const val BURST_CAPACITY = 20
    }
}

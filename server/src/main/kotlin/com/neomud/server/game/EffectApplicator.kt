package com.neomud.server.game

import com.neomud.shared.model.Player

data class EffectResult(val newHp: Int, val newMp: Int, val message: String)

object EffectApplicator {

    fun applyEffect(type: String, magnitude: Int, customMessage: String, player: Player): EffectResult? {
        return when (type) {
            "HEAL", "HEAL_OVER_TIME" -> {
                if (player.currentHp >= player.maxHp) return null
                val healed = minOf(magnitude, player.maxHp - player.currentHp)
                val newHp = player.currentHp + healed
                val msg = customMessage.ifEmpty { "A healing aura soothes your wounds. (+$healed HP)" }
                EffectResult(newHp, player.currentMp, msg)
            }
            "POISON" -> {
                val newHp = (player.currentHp - magnitude).coerceAtLeast(1)
                val msg = customMessage.ifEmpty { "Poison courses through your veins! (-$magnitude HP)" }
                EffectResult(newHp, player.currentMp, msg)
            }
            "DAMAGE" -> {
                val newHp = (player.currentHp - magnitude).coerceAtLeast(1)
                val msg = customMessage.ifEmpty { "You take $magnitude damage! (-$magnitude HP)" }
                EffectResult(newHp, player.currentMp, msg)
            }
            "MANA_REGEN" -> {
                if (player.currentMp >= player.maxMp) return null
                val restored = minOf(magnitude, player.maxMp - player.currentMp)
                val newMp = player.currentMp + restored
                val msg = customMessage.ifEmpty { "You feel magical energy flowing into you. (+$restored MP)" }
                EffectResult(player.currentHp, newMp, msg)
            }
            "MANA_DRAIN" -> {
                if (player.currentMp <= 0) return null
                val drained = minOf(magnitude, player.currentMp)
                val newMp = (player.currentMp - drained).coerceAtLeast(0)
                val msg = customMessage.ifEmpty { "You feel magical energy draining away! (-$drained MP)" }
                EffectResult(player.currentHp, newMp, msg)
            }
            "SANCTUARY" -> null
            else -> null
        }
    }
}

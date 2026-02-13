package com.neomud.shared.protocol

import com.neomud.shared.model.CharacterClass
import com.neomud.shared.model.Direction
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class ClientMessage {
    @Serializable
    @SerialName("register")
    data class Register(
        val username: String,
        val password: String,
        val characterName: String,
        val characterClass: CharacterClass
    ) : ClientMessage()

    @Serializable
    @SerialName("login")
    data class Login(
        val username: String,
        val password: String
    ) : ClientMessage()

    @Serializable
    @SerialName("move")
    data class Move(
        val direction: Direction
    ) : ClientMessage()

    @Serializable
    @SerialName("look")
    data object Look : ClientMessage()

    @Serializable
    @SerialName("say")
    data class Say(
        val message: String
    ) : ClientMessage()

    @Serializable
    @SerialName("ping")
    data object Ping : ClientMessage()

    @Serializable
    @SerialName("attack_toggle")
    data class AttackToggle(val enabled: Boolean) : ClientMessage()

    @Serializable
    @SerialName("select_target")
    data class SelectTarget(val npcId: String?) : ClientMessage()
}

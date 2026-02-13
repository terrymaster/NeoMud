package com.neomud.shared.protocol

import kotlinx.serialization.json.Json

object MessageSerializer {
    val json = Json {
        classDiscriminator = "type"
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encodeClientMessage(message: ClientMessage): String =
        json.encodeToString(ClientMessage.serializer(), message)

    fun decodeClientMessage(text: String): ClientMessage =
        json.decodeFromString(ClientMessage.serializer(), text)

    fun encodeServerMessage(message: ServerMessage): String =
        json.encodeToString(ServerMessage.serializer(), message)

    fun decodeServerMessage(text: String): ServerMessage =
        json.decodeFromString(ServerMessage.serializer(), text)
}

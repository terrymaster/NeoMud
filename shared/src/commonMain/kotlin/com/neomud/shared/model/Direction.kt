package com.neomud.shared.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Direction {
    @SerialName("NORTH") NORTH,
    @SerialName("SOUTH") SOUTH,
    @SerialName("EAST") EAST,
    @SerialName("WEST") WEST,
    @SerialName("UP") UP,
    @SerialName("DOWN") DOWN,
    @SerialName("NORTHEAST") NORTHEAST,
    @SerialName("NORTHWEST") NORTHWEST,
    @SerialName("SOUTHEAST") SOUTHEAST,
    @SerialName("SOUTHWEST") SOUTHWEST;

    /** Human-readable phrase for locked exit messages, e.g. "the door to the north" or "the way up". */
    val lockedExitPhrase: String get() = when (this) {
        UP -> "the way up"
        DOWN -> "the way down"
        else -> "the door to the ${name.lowercase()}"
    }

    fun opposite(): Direction = when (this) {
        NORTH -> SOUTH
        SOUTH -> NORTH
        EAST -> WEST
        WEST -> EAST
        UP -> DOWN
        DOWN -> UP
        NORTHEAST -> SOUTHWEST
        SOUTHWEST -> NORTHEAST
        NORTHWEST -> SOUTHEAST
        SOUTHEAST -> NORTHWEST
    }
}

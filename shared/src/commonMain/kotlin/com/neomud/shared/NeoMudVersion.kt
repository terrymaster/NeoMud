package com.neomud.shared

object NeoMudVersion {
    const val NAME = "NeoMud"
    const val STAGE = "alpha"
    const val PROTOCOL_VERSION = 1
    // Bump when protocol changes require a client update
    const val MIN_CLIENT_VERSION = "0.1.0.0"
    val ENGINE_VERSION: String get() = VERSION_CODE
    val VERSION: String get() = "$STAGE $VERSION_CODE"
    val DISPLAY: String get() = "$NAME $VERSION"

    /**
     * Compare two version strings (e.g., "0.1.0" vs "0.2.0").
     * Handles 3-part and 4-part versions by zero-padding the shorter one.
     * Returns negative if a < b, zero if equal, positive if a > b.
     */
    fun compareVersions(a: String, b: String): Int {
        val partsA = a.split(".").map { it.toIntOrNull() ?: 0 }
        val partsB = b.split(".").map { it.toIntOrNull() ?: 0 }
        val maxLen = maxOf(partsA.size, partsB.size)
        for (i in 0 until maxLen) {
            val partA = partsA.getOrElse(i) { 0 }
            val partB = partsB.getOrElse(i) { 0 }
            if (partA != partB) return partA.compareTo(partB)
        }
        return 0
    }
}

// Updated at build time by: ./gradlew updateVersion
// To bump versions, create a git tag: git tag v0.1.0
// The build number (last digit) auto-increments from commit count since tag.
internal const val VERSION_CODE = "0.1.0.0"

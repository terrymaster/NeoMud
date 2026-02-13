package com.neomud.shared

object NeoMudVersion {
    const val NAME = "NeoMud"
    const val STAGE = "alpha"
    val VERSION: String get() = "$STAGE $VERSION_CODE"
    val DISPLAY: String get() = "$NAME $VERSION"
}

// Updated at build time by: ./gradlew updateVersion
// To bump versions, create a git tag: git tag v0.1.0
// The build number (last digit) auto-increments from commit count since tag.
internal const val VERSION_CODE = "0.0.0.1"

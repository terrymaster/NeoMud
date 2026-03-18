package com.neomud.client.platform

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.platform.Font
import org.jetbrains.skia.FontMgr
import org.jetbrains.skia.FontStyle as SkFontStyle

actual val EmojiSafeFontFamily: FontFamily = createEmojiFontFamily()

private fun createEmojiFontFamily(): FontFamily {
    return try {
        val fontMgr = FontMgr.default
        val typeface = fontMgr.matchFamilyStyle("Apple Color Emoji", SkFontStyle.NORMAL)
            ?: return FontFamily.Default
        val data = typeface.serializeToData().bytes
        FontFamily(
            Font(
                identity = "AppleColorEmoji",
                data = data,
                weight = FontWeight.Normal,
                style = FontStyle.Normal
            )
        )
    } catch (e: Exception) {
        FontFamily.Default
    }
}

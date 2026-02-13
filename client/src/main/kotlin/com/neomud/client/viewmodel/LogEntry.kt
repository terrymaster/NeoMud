package com.neomud.client.viewmodel

import androidx.compose.ui.graphics.Color

data class LogSpan(val text: String, val color: Color)

data class LogEntry(val spans: List<LogSpan>) {
    constructor(text: String, color: Color) : this(listOf(LogSpan(text, color)))
}

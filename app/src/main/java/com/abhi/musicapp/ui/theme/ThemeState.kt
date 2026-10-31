package com.abhi.musicapp.ui.theme

import androidx.compose.runtime.mutableStateOf

/** App-wide theme selection. Dark is OUR proposed variant - his Figma is light-only. */
object ThemeState {
    enum class Mode { LIGHT, DARK, SYSTEM }
    var mode = mutableStateOf(Mode.LIGHT)
    var accent = mutableStateOf(androidx.compose.ui.graphics.Color(0xFFFF6603))
}

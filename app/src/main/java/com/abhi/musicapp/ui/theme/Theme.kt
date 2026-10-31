package com.abhi.musicapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Exact values sampled from the Figma file "Music App" (qZQCBqzvSKC9gJ46TzEkz2), Page 1.
// Style names come from the file's own Styles panel: Black, Bg, White, Bg 2, Orangs, Gray-light.
val InkBlack = Color(0xFF1E1E1E)   // "Black"
val Bg = Color(0xFFEAEAE8)         // "Bg"
val SurfaceSoft = Color(0xFFF0F0F0) // "Bg 2"
val PureWhite = Color(0xFFFFFFFF)  // "White"
val Orangs = Color(0xFFFF6603)     // "Orangs" (his style name, kept)
val GrayLight = Color(0xFFD9D9D9)  // "Gray-light"
val GrayText = Color(0xFF8A8A8A)

private val MusicColors = lightColorScheme(
    primary = Orangs,
    onPrimary = PureWhite,
    background = Bg,
    onBackground = InkBlack,
    surface = Bg,
    onSurface = InkBlack,
    surfaceVariant = SurfaceSoft,
    onSurfaceVariant = InkBlack,
    secondary = GrayLight,
    onSecondary = InkBlack,
)

@Composable
fun MusicAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = MusicColors, content = content)
}

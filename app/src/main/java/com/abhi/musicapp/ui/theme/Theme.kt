package com.abhi.musicapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Exact values sampled from the Figma file "Music App" (qZQCBqzvSKC9gJ46TzEkz2), Page 1.
// Style names come from the file's own Styles panel: Black, Bg, White, Bg 2, Orangs, Gray-light.
private val LightBg = Color(0xFFEAEAE8)        // "Bg"
private val LightSurface = Color(0xFFF0F0F0)   // "Bg 2"
private val LightInk = Color(0xFF1E1E1E)       // "Black"
private val DarkBg = Color(0xFF1E1E1E)
private val DarkSurface = Color(0xFF2A2A2A)
private val DarkInk = Color(0xFFF0F0F0)
val PureWhite = Color(0xFFFFFFFF)  // "White"
val GrayLight = Color(0xFFD9D9D9)  // "Gray-light"
val GrayText = Color(0xFF8A8A8A)

// Theme-aware tokens: screens read these so Dark mode repaints every surface.
// Light values are his exact sampled styles; dark is our proposed variant.
val Bg: Color @Composable get() = MaterialTheme.colorScheme.background
val SurfaceSoft: Color @Composable get() = MaterialTheme.colorScheme.surfaceVariant
val InkBlack: Color @Composable get() = MaterialTheme.colorScheme.onBackground
val Orangs: Color @Composable get() = MaterialTheme.colorScheme.primary

private val AccentDefault = Color(0xFFFF6603) // "Orangs" (his style name, kept)

private val MusicColorsDark = darkColorScheme(
    primary = AccentDefault,
    onPrimary = PureWhite,
    background = DarkBg,
    onBackground = DarkInk,
    surface = DarkBg,
    onSurface = DarkInk,
    surfaceVariant = DarkSurface,
    onSurfaceVariant = DarkInk,
    secondary = Color(0xFF3A3A3A),
    onSecondary = DarkInk,
)

private val MusicColors = lightColorScheme(
    primary = AccentDefault,
    onPrimary = PureWhite,
    background = LightBg,
    onBackground = LightInk,
    surface = LightBg,
    onSurface = LightInk,
    surfaceVariant = LightSurface,
    onSurfaceVariant = LightInk,
    secondary = GrayLight,
    onSecondary = LightInk,
)

@Composable
fun MusicAppTheme(content: @Composable () -> Unit) {
    val dark = when (ThemeState.mode.value) {
        ThemeState.Mode.LIGHT -> false
        ThemeState.Mode.DARK -> true
        ThemeState.Mode.SYSTEM -> isSystemInDarkTheme()
    }
    val colors = (if (dark) MusicColorsDark else MusicColors).copy(primary = ThemeState.accent.value)
    MaterialTheme(colorScheme = colors, content = content)
}

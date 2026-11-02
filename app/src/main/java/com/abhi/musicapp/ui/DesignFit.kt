package com.abhi.musicapp.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

/**
 * The screens are laid out on his Figma 390dp-wide frame with absolute positions. Phones narrower than
 * 390dp (his phone measured about 360dp: 719px at 2.0 density) clipped the right edge. When the window is
 * narrower than 390dp, shrink the whole design uniformly (dp and sp together) so the 390dp frame exactly
 * fits the width. Wider windows are unchanged. Nothing else about the design changes.
 */
@Composable
fun FitDesignWidth(content: @Composable () -> Unit) {
    val widthDp = LocalConfiguration.current.screenWidthDp
    val d = LocalDensity.current
    val scale = if (widthDp in 1..389) widthDp / 390f else 1f
    if (scale == 1f) {
        content()
    } else {
        CompositionLocalProvider(LocalDensity provides Density(d.density * scale, d.fontScale)) { content() }
    }
}

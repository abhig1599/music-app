package com.abhi.musicapp

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.abhi.musicapp.library.VisibilityStore
import com.abhi.musicapp.ui.screens.SettingsScreen
import com.abhi.musicapp.ui.theme.MusicAppTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Shipped Settings (fixture = false) with controlled folder data: two shown, two hidden. Test render, not a phone screenshot. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w390dp-h1500dp-xhdpi")
class SettingsShippedRenderTest {
    @get:Rule val rule = createComposeRule()
    private val folders = listOf(
        "Music" to "/storage/emulated/0/Music",
        "Downloads" to "/storage/emulated/0/Download",
        "Call Recording" to "/storage/emulated/0/Recordings/Call",
        "Voice Notes" to "/storage/emulated/0/Recordings/Voice",
    )

    private fun render(name: String, fontScale: Float) {
        org.robolectric.RuntimeEnvironment.setFontScale(fontScale)
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val vs = VisibilityStore(ctx)
        vs.setHidden("/storage/emulated/0/Recordings/Call", true)
        vs.setHidden("/storage/emulated/0/Recordings/Voice", true)
        rule.setContent { MusicAppTheme { SettingsScreen(onBack = {}, startExpanded = true, previewFolders = folders) } }
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("build/outputs/roborazzi/$name.png")
    }

    @Test fun normalFont() = render("settings_shipped_expanded_font100", 1.0f)
    @Test fun largeFont() = render("settings_shipped_expanded_font130", 1.3f)
}

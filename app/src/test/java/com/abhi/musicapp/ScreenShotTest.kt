package com.abhi.musicapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.abhi.musicapp.ui.screens.DownloadsScreen
import com.abhi.musicapp.ui.screens.EqualiserScreen
import com.abhi.musicapp.ui.screens.HomeScreen
import com.abhi.musicapp.ui.screens.OnboardingScreen
import com.abhi.musicapp.ui.screens.PlayerScreen
import com.abhi.musicapp.ui.screens.SearchScreen
import com.abhi.musicapp.ui.screens.SettingsScreen
import com.abhi.musicapp.ui.screens.ThemesScreen
import com.abhi.musicapp.ui.theme.ThemeState
import com.abhi.musicapp.ui.theme.MusicAppTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders each screen to a real PNG (Robolectric native graphics) so layout
 * can be pixel-compared against the Figma design. Output: build/outputs/roborazzi.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], instrumentedPackages = ["androidx.loader.content"], qualifiers = "w390dp-h844dp")
class ScreenShotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun shot(name: String, content: @androidx.compose.runtime.Composable () -> Unit) {
        // 390x844 dp canvas (his Figma frame) via the qualifiers on the class;
        // paint the theme background so dark mode renders truthfully.
        composeRule.setContent {
            MusicAppTheme {
                Box(
                    modifier = Modifier
                        .size(390.dp, 844.dp)
                        .background(androidx.compose.material3.MaterialTheme.colorScheme.background)
                ) { content() }
            }
        }
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage("build/outputs/roborazzi/$name.png")
    }

    @Test fun onboarding() = shot("onboarding") { OnboardingScreen(onDone = {}) }

    @Test fun onboardingPermissions() = shot("onboarding_permissions") { OnboardingScreen(onDone = {}, initialStep = 2) }

    @Test fun onboardingReader() = shot("onboarding_reader") { OnboardingScreen(onDone = {}, initialStep = 3) }

    @Test fun home() = shot("home") {
        HomeScreen({}, {}, {}, {}, {}, {})
    }

    @Test fun player() = shot("player") { PlayerScreen(onBack = {}) }

    @Test
    fun playerPlaying() = shot("player_playing") {
        PlayerScreen(
            onBack = {},
            fixture = com.abhi.musicapp.playback.PlayerUiState(
                title = "Earring...",
                artist = "Demo Artist",
                isPlaying = true,
                positionMs = 61_000L,
                durationMs = 192_000L,
                hasTrack = true,
            ),
        )
    }

    @Test fun search() = shot("search") { SearchScreen(onBack = {}) }

    @Test fun settings() = shot("settings") { SettingsScreen(onBack = {}, onRowClick = {}) }

    @Test fun themes() = shot("themes") { ThemesScreen(onBack = {}) }

    @Test
    fun themesDark() {
        ThemeState.mode.value = ThemeState.Mode.DARK
        shot("themes_dark") { ThemesScreen(onBack = {}) }
        ThemeState.mode.value = ThemeState.Mode.LIGHT
    }

    @Test fun downloads() = shot("downloads") { DownloadsScreen(onBack = {}) }

    @Test fun equaliser() = shot("equaliser") { EqualiserScreen(onBack = {}) }
}

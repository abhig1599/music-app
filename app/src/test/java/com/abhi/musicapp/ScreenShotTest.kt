package com.abhi.musicapp

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.abhi.musicapp.ui.screens.DownloadsScreen
import com.abhi.musicapp.ui.screens.HomeScreen
import com.abhi.musicapp.ui.screens.OnboardingScreen
import com.abhi.musicapp.ui.screens.PlayerScreen
import com.abhi.musicapp.ui.screens.SearchScreen
import com.abhi.musicapp.ui.screens.SettingsScreen
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
@Config(sdk = [34])
class ScreenShotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun shot(name: String, content: @androidx.compose.runtime.Composable () -> Unit) {
        composeRule.setContent { MusicAppTheme(content = content) }
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage("build/outputs/roborazzi/$name.png")
    }

    @Test fun onboarding() = shot("onboarding") { OnboardingScreen(onDone = {}) }

    @Test fun home() = shot("home") {
        HomeScreen({}, {}, {}, {}, {}, {})
    }

    @Test fun player() = shot("player") { PlayerScreen(onBack = {}) }

    @Test fun search() = shot("search") { SearchScreen(onBack = {}) }

    @Test fun settings() = shot("settings") { SettingsScreen(onBack = {}) }

    @Test fun downloads() = shot("downloads") { DownloadsScreen(onBack = {}) }
}

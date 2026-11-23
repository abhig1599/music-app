package com.abhi.musicapp

import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.abhi.musicapp.ui.screens.SettingsScreen
import com.abhi.musicapp.ui.theme.MusicAppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// The two Song Transitions switches must stay coherent (0.1.7 allowed AutoMix on with Crossfade
// off, a state that mixes nothing): AutoMix on forces Crossfade on; Crossfade off drops AutoMix.
// Fixture mode starts both off and touches no persisted settings.
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], instrumentedPackages = ["androidx.loader.content"], qualifiers = "w390dp-h844dp")
class TransitionSwitchesCoherenceTest {
    @get:Rule val rule = createComposeRule()

    private val xOnCopy = "The next song starts before this one ends"
    private val xOffCopy = "Off. Songs play one after the other"
    private val aOffCopy = "Off. The overlap is a plain crossfade for the set time. Turning this on also turns Crossfade on."

    private fun toggle(title: String) =
        rule.onNode(hasClickAction() and hasAnyDescendant(hasText(title))).performClick()

    @Test fun autoMixOnForcesCrossfadeOn() {
        rule.setContent { MusicAppTheme { SettingsScreen(onBack = {}, fixture = true, startExpanded = true) } }
        rule.waitForIdle()
        rule.onNode(hasText(xOffCopy)).assertExists()
        rule.onNode(hasText(aOffCopy)).assertExists()
        toggle("AutoMix (beta)")
        rule.waitForIdle()
        rule.onNode(hasText(xOnCopy)).assertExists() // Crossfade came on with it
        rule.onNode(hasText(xOffCopy)).assertDoesNotExist()
    }

    @Test fun crossfadeOffDropsAutoMix() {
        rule.setContent { MusicAppTheme { SettingsScreen(onBack = {}, fixture = true, startExpanded = true) } }
        rule.waitForIdle()
        toggle("AutoMix (beta)") // both on
        rule.waitForIdle()
        toggle("Crossfade")
        rule.waitForIdle()
        rule.onNode(hasText(xOffCopy)).assertExists()
        rule.onNode(hasText(aOffCopy)).assertExists() // AutoMix fell with it
    }
}

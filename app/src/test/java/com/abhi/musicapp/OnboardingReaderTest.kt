package com.abhi.musicapp

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.abhi.musicapp.ui.screens.OnboardingScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.abhi.musicapp.ui.theme.MusicAppTheme

/** Shipped Reader step must not carry Figma sample content or a setup promise; the fixture keeps the original. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w390dp-h844dp")
class OnboardingReaderTest {
    @get:Rule val rule = createComposeRule()

    @Test fun shippedReaderStepHasNoFigmaSampleOrSetupPromise() {
        rule.setContent { MusicAppTheme { OnboardingScreen(onDone = {}, initialStep = 3) } }
        rule.onNodeWithText("API Key").assertExists()
        rule.onNodeWithText("API Key (....hmwg)").assertDoesNotExist()
        rule.onNodeWithText("Enable Reader").assertDoesNotExist()
        rule.onNodeWithText("Create Google Gemini API").assertDoesNotExist()
        rule.onNodeWithText("Reader (unavailable)").assertExists()
    }

    @Test fun fixtureKeepsFigmaOriginal() {
        rule.setContent { MusicAppTheme { OnboardingScreen(onDone = {}, initialStep = 3, figmaFixture = true) } }
        rule.onNodeWithText("Enable Reader").assertExists()
        rule.onNodeWithText("API Key (....hmwg)").assertExists()
    }
}

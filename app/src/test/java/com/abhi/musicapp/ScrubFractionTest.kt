package com.abhi.musicapp

import com.abhi.musicapp.ui.screens.scrubFraction
import org.junit.Assert.assertEquals
import org.junit.Test

class ScrubFractionTest {
    @Test fun quarterTurnClockwiseAddsQuarter() = assertEquals(0.5f, scrubFraction(0.25f, -90f, 0f), 1e-4f)
    @Test fun counterClockwiseRewinds() = assertEquals(0.0f, scrubFraction(0.25f, 0f, -90f), 1e-4f)
    @Test fun clampsAtEndNoWrap() = assertEquals(1f, scrubFraction(0.9f, 0f, 90f), 1e-4f)
    @Test fun clampsAtStart() = assertEquals(0f, scrubFraction(0.05f, 0f, -90f), 1e-4f)
    @Test fun crossingAtan2SeamIsContinuous() = assertEquals(0.525f, scrubFraction(0.5f, 175f, -176f), 1e-3f)
}

package com.abhi.musicapp

import com.abhi.musicapp.ui.components.DropletState
import com.abhi.musicapp.ui.components.dropletTargets
import org.junit.Assert.*
import org.junit.Test

class DropletStateTest {
    @Test fun silenceHasNoDeformationTarget() {
        assertTrue(dropletTargets(null, 0f).all { it == 0f })
        assertTrue(dropletTargets(FloatArray(48), 0f).all { it == 0f })
    }
    @Test fun louderBandsHaveLargerTargets() {
        val quiet = dropletTargets(FloatArray(48) { 0.1f }, 0f)
        val loud = dropletTargets(FloatArray(48) { 0.8f }, 0.8f)
        assertTrue(loud.indices.all { loud[it] > quiet[it] })
    }
    @Test fun expandsContinuouslyAndReturnsToCentre() {
        val st = DropletState()
        val loud = dropletTargets(FloatArray(48) { 0.8f }, 0.8f)
        st.step(loud, 0L)
        assertTrue(st.x.all { it == 0f })
        st.step(loud, 16_000_000L)
        assertTrue(st.x.indices.all { st.x[it] > 0f && st.x[it] < loud[it] })
        for (t in 32..1000 step 16) st.step(loud, t * 1_000_000L)
        assertTrue(st.x[0] > 0.01f)
        for (t in 1016..5000 step 16) st.step(FloatArray(4), t * 1_000_000L)
        assertTrue(st.x.all { kotlin.math.abs(it) < 0.0001f })
    }
    @Test fun stoppedClockDoesNotMove() {
        val st = DropletState(); val loud = FloatArray(4) { 0.1f }
        st.step(loud, 0); st.step(loud, 100_000_000L)
        val a = st.x.copyOf(); st.step(FloatArray(4), 100_000_000L)
        assertArrayEquals(a, st.x, 0f)
    }
}

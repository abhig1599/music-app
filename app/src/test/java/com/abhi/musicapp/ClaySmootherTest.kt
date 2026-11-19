package com.abhi.musicapp

import com.abhi.musicapp.ui.components.ClaySmoother
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClaySmootherTest {
    private fun ms(m: Int) = m * 1_000_000L
    @Test fun firstStepSnapsThenReleaseIsGradualAndReachesRest() {
        val s = ClaySmoother(2); s.step(floatArrayOf(10f, 0f), ms(0))
        assertEquals(10f, s.maxValue(), 0f)
        s.step(floatArrayOf(0f, 0f), ms(100)); val after100 = s.maxValue()
        assertTrue(after100 in 5f..9.9f) // still mostly there, moving down
        for (t in 200..3000 step 100) s.step(floatArrayOf(0f, 0f), ms(t))
        assertTrue(s.settled)
    }
    @Test fun attackIsFasterThanRelease() {
        val up = ClaySmoother(1); up.step(floatArrayOf(0f), ms(0)); up.step(floatArrayOf(10f), ms(100))
        val down = ClaySmoother(1); down.step(floatArrayOf(10f), ms(0)); down.step(floatArrayOf(0f), ms(100))
        assertTrue(up.maxValue() > 10f - down.maxValue())
    }
    @Test fun clockStandingStillOrGoingBackChangesNothing() {
        val s = ClaySmoother(1); s.step(floatArrayOf(5f), ms(1000)); s.step(floatArrayOf(0f), ms(1000)); s.step(floatArrayOf(0f), ms(500))
        assertEquals(5f, s.maxValue(), 0f)
    }
}

package com.abhi.musicapp

import com.abhi.musicapp.analysis.*
import org.junit.Assert.*
import org.junit.Test

class PhaseScheduleTest {
    private val plan=PhaseTransitionPlanner.Plan(true,26000,4000,1.04f,2f,"test")
    @Test fun speedConversionPreservesOriginalTimelines() {
        val at1=PhaseSchedule.prepare(plan,25000,30000,60000,1f,4000)!!
        assertEquals(1000L,at1.delayMs); assertEquals(4000L,at1.fadeWallMs)
        val at2=PhaseSchedule.prepare(plan,25000,30000,60000,2f,4000)!!
        assertEquals(500L,at2.delayMs); assertEquals(2000L,at2.fadeWallMs)
        assertEquals(2.08f,at2.incomingSpeed,.0001f)
    }
    @Test fun invalidatedOrShortIncomingPlansFallBackRatherThanMove() {
        assertNull(PhaseSchedule.prepare(plan,27000,30000,60000,1f,4000))
        assertNull(PhaseSchedule.prepare(plan,25000,30000,5000,1f,4000))
        assertNull(PhaseSchedule.prepare(plan,25000,30001,60000,1f,4000))
        assertNull(PhaseSchedule.prepare(plan,25000,30000,60000,.5f,4000))
        assertNull(PhaseSchedule.prepare(plan,25000,30000,60000,Float.NaN,4000))
    }
    @Test fun lateTimingCannotRetainAlignmentLabel() {
        assertTrue(PhaseSchedule.canStart(26009,26000,1f))
        assertFalse(PhaseSchedule.canStart(26040,26000,1f))
        assertFalse(PhaseSchedule.canStart(26000,26000,Float.NaN))
    }
}

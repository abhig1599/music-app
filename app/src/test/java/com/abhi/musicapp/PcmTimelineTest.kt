package com.abhi.musicapp

import com.abhi.musicapp.analysis.PcmTimeline
import org.junit.Assert.*
import org.junit.Test

class PcmTimelineTest {
    @Test fun contiguousBlocksAllowTimestampRounding() {
        assertTrue(PcmTimeline.isContinuous(240000000,44100,241000000,44100))
        assertTrue(PcmTimeline.isContinuous(240000000,44100,241001000,44100))
    }
    @Test fun gapsRepeatedBlocksAndInvalidInputsAbstain() {
        assertFalse(PcmTimeline.isContinuous(240000000,44100,241050000,44100))
        assertFalse(PcmTimeline.isContinuous(240000000,44100,240000000,44100))
        assertFalse(PcmTimeline.isContinuous(-1,0,0,44100))
        assertFalse(PcmTimeline.isContinuous(0,-1,0,44100))
        assertFalse(PcmTimeline.isContinuous(0,1,0,0))
        assertFalse(PcmTimeline.isContinuous(Long.MAX_VALUE,44100,0,44100))
    }
}

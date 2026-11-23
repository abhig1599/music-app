package com.abhi.musicapp

import com.abhi.musicapp.analysis.BeatGrid
import com.abhi.musicapp.analysis.BeatWindow
import org.junit.Assert.*
import org.junit.Test

class BeatWindowTest {
    private val grid = BeatGrid(listOf(.237f,.737f,1.237f,1.737f),.8f,120f)
    @Test fun tailOffsetAndConfidenceArePreserved() {
        val absolute = BeatWindow.absoluteGrid(grid,240000,60000)!!
        assertEquals(listOf(240237L,240737L,241237L,241737L),absolute.beatsMs)
        assertEquals(.8f,absolute.confidence,0f)
    }
    @Test fun invalidBoundsFailClosed() {
        assertNull(BeatWindow.absoluteGrid(grid,-1,60000))
        assertNull(BeatWindow.absoluteGrid(grid,Long.MAX_VALUE,60000))
        assertNull(BeatWindow.absoluteGrid(grid,0,1700))
        assertNull(BeatWindow.absoluteGrid(grid.copy(timesSeconds=listOf(0f,.5f,Float.NaN,1.5f)),0,60000))
        assertNull(BeatWindow.absoluteGrid(grid.copy(timesSeconds=listOf(0f,.5f,.5f,1.5f)),0,60000))
    }
}

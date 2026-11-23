package com.abhi.musicapp

import com.abhi.musicapp.analysis.PcmWindow
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.*
import org.junit.Test

class PcmWindowTest {
    @Test fun honorsOffsetSizeChannelsAndDoesNotMoveOriginalBuffer() {
        val b = ByteBuffer.allocate(20).order(ByteOrder.LITTLE_ENDIAN)
        b.putInt(1234); b.putShort(16384); b.putShort(0); b.putShort(-16384); b.putShort(0); b.putInt(999)
        b.position(1); b.limit(2)
        assertArrayEquals(floatArrayOf(.25f, -.25f), PcmWindow.mono(b, 4, 8, 2, 2), .00001f)
        assertEquals(1, b.position()); assertEquals(2, b.limit())
    }
    @Test fun floatAndNonfiniteAreSafe() {
        val b = ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN)
        for (v in floatArrayOf(.8f, .4f, Float.NaN, 1f, -2f, Float.POSITIVE_INFINITY)) b.putFloat(v)
        assertArrayEquals(floatArrayOf(.6f, .5f, -.5f), PcmWindow.mono(b, 0, 24, 2, 4), .00001f)
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsUnsupportedEncoding() { PcmWindow.mono(ByteBuffer.allocate(4), 0, 4, 1, 99) }
    @Test(expected = IllegalArgumentException::class) fun rejectsOutOfBoundsRange() { PcmWindow.mono(ByteBuffer.allocate(4), 2, 4, 1, 2) }
    @Test fun ignoresIncompleteTrailingFrame() { assertEquals(1, PcmWindow.mono(ByteBuffer.allocate(7), 0, 7, 2, 2).size) }
}

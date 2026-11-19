package com.abhi.musicapp

import com.abhi.musicapp.library.EmbeddedLyrics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LyricsTest {
    private fun frame(id: String, body: ByteArray): ByteArray {
        val n = body.size
        return id.toByteArray(Charsets.ISO_8859_1) + byteArrayOf((n shr 24).toByte(), (n shr 16).toByte(), (n shr 8).toByte(), n.toByte(), 0, 0) + body
    }

    @Test fun readsUsltFrame() {
        val body = byteArrayOf(0, 'e'.code.toByte(), 'n'.code.toByte(), 'g'.code.toByte(), 0) + "hello\nworld".toByteArray()
        val tag = frame("TIT2", byteArrayOf(0, 65)) + frame("USLT", body)
        assertEquals("hello\nworld", EmbeddedLyrics.parse(tag, tag.size, 3))
    }

    @Test fun noFrameGivesNull() {
        val tag = frame("TIT2", byteArrayOf(0, 65))
        assertNull(EmbeddedLyrics.parse(tag, tag.size, 3))
    }
}

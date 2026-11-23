package com.abhi.musicapp

import com.abhi.musicapp.playback.BandAnalyzer
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * During a crossfade two live decks feed one shared BandAnalyzer from two audio threads. Before the methods
 * were synchronized, the window fill could race past the buffer and throw on an audio thread - a sink error
 * that stops playback (the 0.1.8 "songs pause by themselves" class of failure). This test hammers one analyzer
 * from two threads and requires every call to come back cleanly.
 */
class BandAnalyzerConcurrencyTest {
    private fun pcm16(frames: Int, seed: Int): ByteBuffer {
        val b = ByteBuffer.allocateDirect(frames * 2).order(ByteOrder.nativeOrder())
        var x = seed
        for (i in 0 until frames) { x = (x * 1103515245 + 12345) and 0x7fffffff; b.putShort(((x shr 8) and 0x7fff).toShort()) }
        b.flip()
        return b
    }

    @Test fun twoDecksFeedingOneAnalyzerNeverThrow() {
        val a = BandAnalyzer()
        a.configure(44100, 2, androidx.media3.common.C.ENCODING_PCM_16BIT)
        val failure = AtomicReference<Throwable?>(null)
        val start = CountDownLatch(1)
        val done = CountDownLatch(2)
        val worker = { reconfigure: Boolean ->
            try {
                start.await()
                for (i in 0 until 400) {
                    a.push(pcm16(256, i))
                    if (reconfigure && i % 25 == 0) a.configure(48000, 2, androidx.media3.common.C.ENCODING_PCM_16BIT)
                }
            } catch (t: Throwable) { failure.compareAndSet(null, t) } finally { done.countDown() }
        }
        val t1 = Thread { worker(false) }; val t2 = Thread { worker(true) }
        t1.start(); t2.start(); start.countDown()
        assertTrue(done.await(30, TimeUnit.SECONDS))
        assertTrue("concurrent feed threw: ${failure.get()}", failure.get() == null)
    }
}

package com.abhi.musicapp

import com.abhi.musicapp.analysis.TempoPrefetcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/** Scheduling logic only, with fake jobs. Says nothing about real decoding. */
class TempoPrefetcherTest {
    @Test fun runsJobAndReportsResult() {
        val p = TempoPrefetcher(); val done = CountDownLatch(1); var got: Float? = null
        p.request("a", { 120f }) { got = it; done.countDown() }
        assertTrue(done.await(2, TimeUnit.SECONDS)); assertEquals(120f, got!!, 0f); p.shutdown()
    }
    @Test fun duplicateRequestForSameSongRunsOnce() {
        val p = TempoPrefetcher(); val runs = AtomicInteger(); val gate = CountDownLatch(1); val done = CountDownLatch(1)
        p.request("a", { gate.await(2, TimeUnit.SECONDS); runs.incrementAndGet(); 100f }) { done.countDown() }
        p.request("a", { runs.incrementAndGet(); 1f })
        gate.countDown(); assertTrue(done.await(2, TimeUnit.SECONDS)); assertEquals(1, runs.get()); p.shutdown()
    }
    @Test fun newSongCancelsThePreviousAndDropsItsResult() {
        val p = TempoPrefetcher(); val started = CountDownLatch(1); val sawCancel = CountDownLatch(1); val delivered = AtomicInteger(); val doneB = CountDownLatch(1)
        p.request("a", { c -> started.countDown(); var n = 0; while (!c() && n++ < 400) Thread.sleep(5); if (c()) sawCancel.countDown(); 90f }) { delivered.incrementAndGet() }
        assertTrue(started.await(2, TimeUnit.SECONDS))
        p.request("b", { 130f }) { doneB.countDown() }
        assertTrue(sawCancel.await(2, TimeUnit.SECONDS)); assertTrue(doneB.await(2, TimeUnit.SECONDS))
        assertEquals(0, delivered.get()); p.shutdown()
    }
    @Test fun failingJobDeliversNullAndKeepsWorking() {
        val p = TempoPrefetcher(); val d1 = CountDownLatch(1); var r1: Float? = 5f
        p.request("a", { throw IllegalStateException("x") }) { r1 = it; d1.countDown() }
        assertTrue(d1.await(2, TimeUnit.SECONDS)); assertEquals(null, r1)
        val d2 = CountDownLatch(1); p.request("b", { 77f }) { d2.countDown() }
        assertTrue(d2.await(2, TimeUnit.SECONDS)); p.shutdown()
    }
    @Test fun requestAfterShutdownDoesNotThrow() { val p = TempoPrefetcher(); p.shutdown(); p.request("a", { 1f }); assertFalse(false) }
}

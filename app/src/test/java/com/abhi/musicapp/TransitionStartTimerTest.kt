package com.abhi.musicapp

import com.abhi.musicapp.analysis.TransitionStartTimer
import org.junit.Assert.*
import org.junit.Test

class TransitionStartTimerTest {
    private class Clock {
        var now=1000L
        val tasks=mutableListOf<Pair<Runnable,Long>>()
        val timer=TransitionStartTimer({now},{r,t->tasks.add(r to t)},{r->tasks.removeAll{it.first===r}})
        fun fire(at: Long) { now=at; val r=tasks.removeAt(0).first; r.run() }
    }
    @Test fun oneShotStartDoesNotDependOnPollingTicks() {
        val c=Clock(); var starts=0; var fallbacks=0
        assertTrue(c.timer.arm(2237,valid={true},start={starts++},fallback={fallbacks++}))
        assertEquals(2237L,c.tasks.single().second)
        c.fire(2241); assertEquals(1,starts); assertEquals(0,fallbacks); assertTrue(c.tasks.isEmpty())
    }
    @Test fun lateAndStaleMustFallBack() {
        val c=Clock(); var starts=0; var fallbacks=0
        c.timer.arm(2237,valid={true},start={starts++},fallback={fallbacks++}); c.fire(2250)
        c.timer.arm(3000,valid={false},start={starts++},fallback={fallbacks++}); c.fire(3000)
        assertEquals(0,starts); assertEquals(2,fallbacks)
    }
    @Test fun cancelledCallbackCannotLeakAcrossReplacement() {
        val c=Clock(); var starts=0
        c.timer.arm(2000,valid={true},start={starts++},fallback={})
        val old=c.tasks.single().first
        c.timer.arm(3000,valid={true},start={starts++},fallback={})
        c.now=2000; old.run(); assertEquals(0,starts)
        c.fire(3000); assertEquals(1,starts)
    }
    @Test fun earlyDispatchRepostsOnceForTargetAndInvalidArmRejects() {
        val c=Clock(); var starts=0
        c.timer.arm(2000,valid={true},start={starts++},fallback={}); c.fire(1900)
        assertEquals(2000L,c.tasks.single().second); assertEquals(0,starts)
        c.fire(2000); assertEquals(1,starts)
        assertFalse(c.timer.arm(1000,valid={true},start={},fallback={}))
    }
    @Test fun stalePlayerReadFailureFallsBackWithoutCrashingCallback() {
        val c=Clock(); var fallbacks=0
        c.timer.arm(2000,valid={throw IllegalStateException("released")},start={fail("must not start")},fallback={fallbacks++})
        c.fire(2000); assertEquals(1,fallbacks)
    }
}

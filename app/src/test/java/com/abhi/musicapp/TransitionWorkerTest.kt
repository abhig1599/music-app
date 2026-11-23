package com.abhi.musicapp

import com.abhi.musicapp.analysis.*
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.AbstractExecutorService
import java.util.concurrent.TimeUnit

class TransitionWorkerTest {
    private class Manual : AbstractExecutorService() {
        val jobs = mutableListOf<Runnable>()
        private var stopped = false
        override fun execute(command: Runnable) { if (stopped) throw java.util.concurrent.RejectedExecutionException(); jobs.add(command) }
        override fun shutdown() { stopped=true }
        override fun shutdownNow(): MutableList<Runnable> { stopped=true; return jobs.toMutableList().also { jobs.clear() } }
        override fun isShutdown()=stopped
        override fun isTerminated()=stopped
        override fun awaitTermination(timeout: Long, unit: TimeUnit)=stopped
        fun runAll() { val pending=jobs.toList(); jobs.clear(); pending.forEach { it.run() } }
    }
    private fun result() = TransitionAnalysisResult(null,null,PhaseTransitionPlanner.plan(
        PhaseTransitionPlanner.Grid(emptyList(),null,0f),PhaseTransitionPlanner.Grid(emptyList(),null,0f),30000,4000))
    @Test fun replacementCancelsOldResultAndDuplicateJob() {
        val exec=Manual(); val worker=TransitionWorker(exec); val received=mutableListOf<String>()
        worker.request("old",{ result() },{received.add("old")})
        worker.request("old",{ result() },{received.add("duplicate")})
        worker.request("new",{ result() },{received.add("new")})
        assertEquals(2,exec.jobs.size)
        exec.runAll(); assertEquals(listOf("new"),received)
    }
    @Test fun cancellationAndShutdownSuppressDelivery() {
        val exec=Manual(); val worker=TransitionWorker(exec); var count=0
        worker.request("one",{ result() },{count++}); worker.cancel(); exec.runAll()
        assertEquals(0,count)
        worker.shutdown(); worker.request("two",{result()},{count++}); exec.runAll()
        assertEquals(0,count)
    }
}

package com.abhi.musicapp.analysis

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/** One cancellable pair-analysis job; callbacks must explicitly marshal to the playback thread. */
class TransitionWorker(private val exec: ExecutorService = Executors.newSingleThreadExecutor { r ->
    Thread(r, "transition-analysis").apply { priority = Thread.MIN_PRIORITY; isDaemon = true }
}) {
    private var token: AtomicBoolean? = null
    private var key: String? = null

    @Synchronized fun request(id: String, job: (() -> Boolean) -> TransitionAnalysisResult?, onResult: (TransitionAnalysisResult) -> Unit) {
        if (key == id && token?.get() == false) return
        token?.set(true)
        val current = AtomicBoolean(false)
        token = current; key = id
        try {
            exec.execute {
                // OOM is the one Error worth recovering from: the native decode + DSP can run out of memory
                // on long songs, and the right outcome is no plan - the pair falls back to the plain crossfade.
                // Fatal Errors (ThreadDeath, LinkageError, ...) are NOT caught: hiding them would leave the
                // daemon thread dead and every later pair silently unanalyzed.
                val value = try {
                    job { current.get() || Thread.currentThread().isInterrupted }
                } catch (e: OutOfMemoryError) {
                    e.printStackTrace()
                    null
                } catch (e: Exception) {
                    // Ordinary failures likewise mean no plan; stderr keeps the cause visible in logcat.
                    e.printStackTrace()
                    null
                }
                synchronized(this) {
                    // Serializes cancellation with delivery. UI delivery still needs its own generation check.
                    if (token === current && !current.get() && value != null) onResult(value)
                    if (token === current) { token = null; key = null }
                }
            }
        } catch (_: java.util.concurrent.RejectedExecutionException) {
            if (token === current) { token = null; key = null }
        }
    }

    @Synchronized fun cancel() { token?.set(true); token = null; key = null }
    fun shutdown() { cancel(); exec.shutdownNow() }
}

/** Immutable evidence payload, with no Android player or context reference. */
data class TransitionAnalysisResult(
    val outgoing: PhaseTransitionPlanner.Grid?,
    val incoming: PhaseTransitionPlanner.Grid?,
    val plan: PhaseTransitionPlanner.Plan,
)

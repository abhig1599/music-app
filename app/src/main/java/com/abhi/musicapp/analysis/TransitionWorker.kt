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
                // Throwable, not Exception: the native decode + DSP can fail with an Error (e.g. OOM on long songs);
                // any failure just means no plan, and the pair falls back to the plain crossfade.
                val value = try { job { current.get() || Thread.currentThread().isInterrupted } } catch (_: Throwable) { null }
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

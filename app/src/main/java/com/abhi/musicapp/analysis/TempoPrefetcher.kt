package com.abhi.musicapp.analysis

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Runs at most one background tempo analysis at a time, on its own low-priority thread. A new request for a different song cancels the
 * previous one; a repeat request for the song already being analysed is ignored. [shutdown] cancels and frees the thread.
 * Pure scheduling logic so it can be unit tested with a fake job; it knows nothing about audio.
 */
class TempoPrefetcher(private val exec: ExecutorService = Executors.newSingleThreadExecutor { r -> Thread(r, "tempo-prefetch").apply { priority = Thread.MIN_PRIORITY; isDaemon = true } }) {
    private var currentKey: String? = null
    private var flag: AtomicBoolean? = null

    @Synchronized fun request(key: String, job: (cancelled: () -> Boolean) -> Float?, onDone: (Float?) -> Unit = {}) {
        if (key == currentKey && flag?.get() == false) return
        flag?.set(true)
        val f = AtomicBoolean(false)
        flag = f; currentKey = key
        try {
            exec.execute {
                val r = try { job { f.get() } } catch (e: Exception) { null }
                if (!f.get()) onDone(r)
                synchronized(this) { if (flag === f) { currentKey = null; flag = null } }
            }
        } catch (e: java.util.concurrent.RejectedExecutionException) { currentKey = null; flag = null }
    }

    @Synchronized fun cancel() { flag?.set(true); flag = null; currentKey = null }
    fun shutdown() { cancel(); exec.shutdownNow() }
}

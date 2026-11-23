package com.abhi.musicapp.analysis

/** One-shot timing only. An on-time callback is not proof of synchronized audio output. */
class TransitionStartTimer(
    private val nowMs: () -> Long,
    private val postAt: (Runnable, Long) -> Unit,
    private val remove: (Runnable) -> Unit,
) {
    private var pending: Runnable? = null
    private var generation = 0L

    /** Must be called on the same thread as callbacks. Returns false if already too late or invalid. */
    fun arm(deadlineMs: Long, toleranceMs: Long = 10, valid: () -> Boolean, start: () -> Unit, fallback: () -> Unit): Boolean {
        cancel()
        val now = nowMs()
        if (toleranceMs !in 0L..30L || deadlineMs < now || deadlineMs - now > 120000L) return false
        val token = generation
        val task = object : Runnable {
            override fun run() {
                if (generation != token || pending !== this) return
                val actual = nowMs()
                if (actual < deadlineMs) {
                    // Protect against an early dispatcher; no spinning or repeated polling.
                    postAt(this, deadlineMs)
                    return
                }
                pending = null
                if (actual - deadlineMs <= toleranceMs && valid()) start() else fallback()
            }
        }
        pending = task
        postAt(task,deadlineMs)
        return true
    }

    fun cancel() { generation++; pending?.let(remove); pending = null }
}

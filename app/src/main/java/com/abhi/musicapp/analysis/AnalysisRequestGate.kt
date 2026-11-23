package com.abhi.musicapp.analysis

/** Playback-thread identity gate. Null means analysis is unavailable or disabled. */
class AnalysisRequestGate {
    private var key: String? = null
    var generation: Long = 0; private set
    fun update(next: String?): Boolean {
        if (next == key) return false
        key = next; generation++
        return true
    }
    fun accepts(expected: String, token: Long): Boolean = key == expected && token == generation
    fun clear() { key = null; generation++ }
}

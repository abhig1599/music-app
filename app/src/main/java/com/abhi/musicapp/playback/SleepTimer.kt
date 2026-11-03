package com.abhi.musicapp.playback

import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Sleep timer states, following his Figma frames "Sleep concept- set up / counting / Time Up / app exit in":
 * pick a time, count down, show "Time-up", then "App exit in 0:10 S x" (x cancels the exit).
 */
sealed interface SleepState {
    data object Off : SleepState
    data class Counting(val totalMs: Long, val endAtMs: Long) : SleepState
    /** Timer ran out with "Play last one to end" on: playback continues until the current song ends. */
    data class FinishingSong(val totalMs: Long) : SleepState
    data class Exiting(val totalMs: Long, val deadlineMs: Long) : SleepState
}

/** What one clock step asks the outside world to do. Pure, so it is unit tested without a device. */
data class SleepStep(val state: SleepState, val pause: Boolean = false, val exitApp: Boolean = false)

internal const val EXIT_GRACE_MS = 10_000L

internal fun sleepStep(s: SleepState, now: Long, playLastToEnd: Boolean, songEnded: Boolean, playing: Boolean): SleepStep = when (s) {
    SleepState.Off -> SleepStep(s)
    is SleepState.Counting ->
        if (now < s.endAtMs) SleepStep(s)
        else if (playLastToEnd && playing) SleepStep(SleepState.FinishingSong(s.totalMs))
        else SleepStep(SleepState.Exiting(s.totalMs, now + EXIT_GRACE_MS), pause = true)
    is SleepState.FinishingSong ->
        if (songEnded || !playing) SleepStep(SleepState.Exiting(s.totalMs, now + EXIT_GRACE_MS), pause = true) else SleepStep(s)
    is SleepState.Exiting ->
        if (now >= s.deadlineMs) SleepStep(SleepState.Off, pause = true, exitApp = true) else SleepStep(s)
}

/** Remaining seconds shown in the exit pill (rounded up, never below 0). */
internal fun exitSecondsLeft(deadlineMs: Long, now: Long): Int = (((deadlineMs - now).coerceAtLeast(0L) + 999L) / 1000L).toInt()

/** Elapsed/total as 0..1 for the orange tick position in the strip. */
internal fun sleepFraction(totalMs: Long, endAtMs: Long, now: Long): Float =
    if (totalMs <= 0) 0f else (1f - ((endAtMs - now).coerceAtLeast(0L).toFloat() / totalMs)).coerceIn(0f, 1f)

object SleepTimer {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _state = MutableStateFlow<SleepState>(SleepState.Off)
    val state: StateFlow<SleepState> = _state
    /** "Play last one to end" switch from the Figma sheet. Kept in memory only (resets when the app process ends). */
    val playLastToEnd = MutableStateFlow(false)
    /** Bumped when the exit countdown finished; MainActivity closes the app task. */
    val exitRequests = MutableStateFlow(0)
    /** Clock for the UI (same clock as the timer). */
    val nowMs: StateFlow<Long> = MutableStateFlow(SystemClock.elapsedRealtime()).also { flow ->
        scope.launch { while (true) { delay(250); flow.value = SystemClock.elapsedRealtime() } }
    }
    private var job: Job? = null
    private var songEnded = false

    fun start(totalMs: Long) {
        if (totalMs <= 0) return
        songEnded = false
        _state.value = SleepState.Counting(totalMs, SystemClock.elapsedRealtime() + totalMs)
        run()
    }

    /** Turn off, or the x on the exit pill. Playback state is left as it is. */
    fun cancel() { job?.cancel(); job = null; songEnded = false; _state.value = SleepState.Off }

    /** PlayerManager calls this on an automatic track change or end of queue. */
    fun onSongEnded() { if (_state.value is SleepState.FinishingSong) songEnded = true }

    val isFinishingSong: Boolean get() = _state.value is SleepState.FinishingSong

    private fun run() {
        job?.cancel()
        job = scope.launch {
            while (_state.value != SleepState.Off) {
                val r = sleepStep(_state.value, SystemClock.elapsedRealtime(), playLastToEnd.value, songEnded, PlayerManager.state.value.isPlaying)
                _state.value = r.state
                if (r.pause) PlayerManager.pauseNow()
                if (r.exitApp) exitRequests.value = exitRequests.value + 1
                delay(250)
            }
        }
    }
}

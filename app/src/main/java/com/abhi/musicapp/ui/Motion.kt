package com.abhi.musicapp.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

/**
 * Motion helpers (native Compose, no extra library). Motion is fluid and interruptible (springs, not fixed
 * durations) and respects the system "Remove animations" / animator-duration-scale 0 setting.
 */
@Composable
fun reducedMotion(): Boolean {
    val ctx = LocalContext.current
    return remember {
        runCatching {
            android.provider.Settings.Global.getFloat(ctx.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        }.getOrDefault(false)
    }
}

/** Soft press feedback: the element eases down to [pressedScale] while held and springs back with a slight overshoot. Does not consume touches. */
fun Modifier.pressScale(pressedScale: Float = 0.93f): Modifier = composed {
    var pressed by remember { mutableStateOf(false) }
    val reduce = reducedMotion()
    val s by animateFloatAsState(
        targetValue = if (pressed && !reduce) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow),
        label = "pressScale",
    )
    this
        .graphicsLayer { scaleX = s; scaleY = s }
        .pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                pressed = true
                waitForUpOrCancellation()
                pressed = false
            }
        }
}

/**
 * Tap runs [onTap]; holding for 350 ms starts [onHoldStart] and releasing (or the touch being cancelled, or the
 * view leaving the screen) runs [onHoldEnd]. A hold never also counts as a tap.
 */
fun Modifier.tapOrHold(onTap: () -> Unit, onHoldStart: () -> Unit, onHoldEnd: () -> Unit): Modifier = composed {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val tap = androidx.compose.runtime.rememberUpdatedState(onTap)
    val start = androidx.compose.runtime.rememberUpdatedState(onHoldStart)
    val end = androidx.compose.runtime.rememberUpdatedState(onHoldEnd)
    androidx.compose.runtime.DisposableEffect(Unit) { onDispose { end.value() } }
    pointerInput(Unit) {
        androidx.compose.foundation.gestures.detectTapGestures(onPress = {
            var holding = false
            val job = scope.launch { kotlinx.coroutines.delay(350); holding = true; start.value() }
            val released = tryAwaitRelease()
            job.cancel()
            if (holding) end.value() else if (released) tap.value()
        })
    }
}

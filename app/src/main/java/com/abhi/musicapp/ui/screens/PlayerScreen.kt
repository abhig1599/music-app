package com.abhi.musicapp.ui.screens

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import com.abhi.musicapp.ui.tapOrHold
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import com.abhi.musicapp.ui.pressScale
import com.abhi.musicapp.ui.reducedMotion
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhi.musicapp.playback.PlayerManager
import com.abhi.musicapp.playback.PlayerUiState
import com.abhi.musicapp.ui.components.TickDial
import com.abhi.musicapp.ui.theme.GrayLight
import com.abhi.musicapp.ui.theme.GrayText
import com.abhi.musicapp.ui.theme.InkBlack
import com.abhi.musicapp.ui.theme.Orangs
import com.abhi.musicapp.ui.theme.PureWhite

/** The ring only starts a seek-drag beyond this fraction of the dial width from its centre; the centre disc (164dp of 301dp, radius 0.272) sits inside it, leaving a small dead band between the two. */
internal const val RING_DRAG_MIN_FRACTION = 0.34f
internal const val CENTRE_DISC_DP = 164f
internal const val DIAL_DP = 301f

internal fun formatMs(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSec = ms / 1000
    return "%d:%02d".format(totalSec / 60, totalSec % 60)
}

private fun formatTotal(ms: Long): String {
    val totalSec = if (ms <= 0) 0 else ms / 1000
    return "%02d:%02d".format(totalSec / 60, totalSec % 60)
}

/**
 * Player, measured from his Figma "Sleep concept- app exit in" frame (390x844,
 * captured at 68% zoom, positions divided by 0.678): header centre y~132,
 * dial 296dp centre y~327 with thin ticks and the orange cluster at the top,
 * source label y~537, title y~579, controls y~712 (light 58dp circles around
 * prev/next, 60dp play), bottom row y~785 (72dp circles). Not yet verified
 * pixel-exact. Focus glyph provisional (his pending decision).
 */
/** Fraction change for a drag around the dial centre: angle delta (wrapped to +-180 deg) / 360, clamped 0..1 so it never wraps from end to start. */
internal fun scrubFraction(start: Float, prevAngleDeg: Float, curAngleDeg: Float): Float {
    var d = curAngleDeg - prevAngleDeg
    while (d > 180f) d -= 360f
    while (d < -180f) d += 360f
    return (start + d / 360f).coerceIn(0f, 1f)
}

@Composable
fun PlayerScreen(onBack: () -> Unit, fixture: PlayerUiState? = null, initialOverlay: Int = 0, sourceLabel: String? = null, progressInFixture: Boolean = false, sleepPreview: com.abhi.musicapp.playback.SleepState? = null, fixtureLevels: FloatArray? = null, fixtureFlipped: Boolean? = null, fixtureRotation: Float? = null) {
    // 0 none, 1 three-dots menu, 2 focus-mode popup, 3 output-device popup (Figma 'Song Page - menu', 'Song focus mode', 'Song Focus connected device').
    var overlay by androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(initialOverlay) }
    var menuNote by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    val live by PlayerManager.state.collectAsState()
    val state = fixture ?: live
    val rawProgress = if (state.durationMs > 0)
        state.positionMs.toFloat() / state.durationMs.toFloat() else 0f
    // Position arrives every 500ms; ease between ticks so the dial glides (instant when motion is reduced or on a fixture).
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var outKind by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(com.abhi.musicapp.playback.OutputKind.UNKNOWN) }
    var outNote by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    if (fixture == null) {
        // Re-read the routed device on connect/disconnect, when playback starts or stops, and when the track changes
        // (a route can change without a connect event, so the callback alone is not trusted).
        androidx.compose.runtime.DisposableEffect(Unit) {
            outKind = com.abhi.musicapp.playback.AudioOutput.current(ctx)
            val un = com.abhi.musicapp.playback.AudioOutput.register(ctx) { outKind = com.abhi.musicapp.playback.AudioOutput.current(ctx) }
            onDispose { un() }
        }
        androidx.compose.runtime.LaunchedEffect(state.isPlaying, state.mediaId) { outKind = com.abhi.musicapp.playback.AudioOutput.current(ctx) }
        // Bounded re-read every 2 s while this screen is shown: a route can change with no connect event.
        androidx.compose.runtime.LaunchedEffect(Unit) { while (true) { kotlinx.coroutines.delay(2000); outKind = com.abhi.musicapp.playback.AudioOutput.current(ctx) } }
    }
    val outIcon = when (outKind) {
        com.abhi.musicapp.playback.OutputKind.SPEAKER -> Icons.Filled.Speaker
        com.abhi.musicapp.playback.OutputKind.PHONE -> Icons.Filled.PhoneAndroid
        com.abhi.musicapp.playback.OutputKind.BLUETOOTH -> Icons.Filled.Bluetooth
        com.abhi.musicapp.playback.OutputKind.WIRED_HEADPHONES, com.abhi.musicapp.playback.OutputKind.USB -> Icons.Filled.Headset
        else -> Icons.Filled.VolumeUp
    }
    val outLabel = when (outKind) {
        com.abhi.musicapp.playback.OutputKind.SPEAKER -> "Audio output: phone speaker"
        com.abhi.musicapp.playback.OutputKind.PHONE -> "Audio output: phone earpiece"
        com.abhi.musicapp.playback.OutputKind.WIRED_HEADPHONES -> "Audio output: wired headphones"
        com.abhi.musicapp.playback.OutputKind.BLUETOOTH -> "Audio output: Bluetooth"
        com.abhi.musicapp.playback.OutputKind.USB -> "Audio output: USB audio"
        else -> "Audio output: unknown"
    }
    var lastAng by androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    var ringDrag by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var flipped by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    var scrub by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<Float?>(null) }
    val progress = if (scrub != null) scrub!! else if (fixture != null || reducedMotion()) rawProgress else
        androidx.compose.animation.core.animateFloatAsState(rawProgress, androidx.compose.animation.core.tween(260, easing = androidx.compose.animation.core.LinearEasing), label = "dial").value
    val circle = com.abhi.musicapp.ui.theme.SurfaceSoft
    // Audio-reactive ticks: analysis runs only while this screen is shown and a song plays; eased so ticks rise fast and fall slowly. Reduced motion keeps ticks fixed.
    val reduced = reducedMotion()
    val frame = androidx.compose.runtime.remember { androidx.compose.runtime.mutableLongStateOf(0L) }
    val shown = androidx.compose.runtime.remember { FloatArray(com.abhi.musicapp.playback.LEVEL_BANDS) }
    val live2 = fixture == null && state.isPlaying && !reduced
    androidx.compose.runtime.DisposableEffect(live2) {
        com.abhi.musicapp.playback.AudioLevels.setEnabled(live2)
        onDispose { com.abhi.musicapp.playback.AudioLevels.setEnabled(false) }
    }
    androidx.compose.runtime.LaunchedEffect(live2) {
        if (!live2) { shown.fill(0f); frame.longValue++; return@LaunchedEffect }
        while (true) {
            androidx.compose.runtime.withFrameNanos { }
            val t = com.abhi.musicapp.playback.AudioLevels.bands
            for (i in shown.indices) { val v = t[i]; shown[i] = if (v > shown[i]) shown[i] + (v - shown[i]) * 0.6f else shown[i] * 0.88f }
            frame.longValue++
        }
    }
    val levelsFn: (() -> FloatArray?)? = if (fixtureLevels != null) ({ fixtureLevels }) else if (fixture == null && !reduced) ({ frame.longValue; shown }) else null

    val mono = com.abhi.musicapp.ui.theme.GeistMono // Figma: Geist Mono (not bundled until licence check)
    // Absolute layout from exact Design-panel reads (390x844 frame, see DECISIONS.md 02:40).
    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize()) {
        // Reference viewport is 390x844. Shorter areas (system bars, small phones): the top group stays
        // anchored to the top, the title/controls/bottom group anchors to the bottom, and the dial group scales
        // down only if the two would collide. At 844dp this is identical to the Figma positions.
        val shrink = (844.dp - maxHeight).coerceAtLeast(0.dp)
        val dialScale = ((338f - shrink.value) / 301f).coerceIn(0.5f, 1f)
        val sleep by com.abhi.musicapp.playback.SleepTimer.state.collectAsState()
        val sleepNow by com.abhi.musicapp.playback.SleepTimer.nowMs.collectAsState()
        val sleepShown = sleepPreview ?: if (fixture == null) sleep else com.abhi.musicapp.playback.SleepState.Off
        val sleepOn = sleepShown != com.abhi.musicapp.playback.SleepState.Off
        if (sleepOn) Box(Modifier.offset(23.dp, 46.dp)) {
            when (val sl = sleepShown) {
                is com.abhi.musicapp.playback.SleepState.Counting -> {
                    val remain = sl.endAtMs - sleepNow
                    val frac = com.abhi.musicapp.playback.sleepFraction(sl.totalMs, sl.endAtMs, sleepNow)
                    com.abhi.musicapp.ui.components.SleepStrip(clockText(sl.totalMs - remain), clockText(sl.totalMs), orangeCluster = true, orangeIndex = (frac * 23).toInt())
                }
                is com.abhi.musicapp.playback.SleepState.FinishingSong -> com.abhi.musicapp.ui.components.SleepStrip(clockText(sl.totalMs), null, timeUp = true)
                is com.abhi.musicapp.playback.SleepState.Exiting -> com.abhi.musicapp.ui.components.AppExitPill(com.abhi.musicapp.playback.exitSecondsLeft(sl.deadlineMs, sleepNow)) { com.abhi.musicapp.playback.SleepTimer.cancel() }
                else -> {}
            }
        }
        Row(
            modifier = Modifier.offset(26.dp, if (sleepOn) 96.dp else 46.dp).size(344.dp, 37.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Playlist", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = InkBlack,
                modifier = Modifier.weight(1f).clickable(onClick = onBack))
            Icon(Icons.Filled.MoreVert, contentDescription = "Menu", tint = InkBlack,
                modifier = Modifier.clickable { overlay = if (overlay == 1) 0 else 1 })
        }
        TickDial(progress = progress, modifier = Modifier.offset(48.dp, 135.dp).size(301.dp)
            .pointerInput(fixture == null && state.durationMs > 0) {
                if (fixture != null || state.durationMs <= 0) return@pointerInput
                // DJ-style scrub: drag around the dial; time and ticks follow the finger, the seek is applied on release.
                // Relative to where the finger lands (no jump), clamped at start/end (no wrap).
                fun ang(o: androidx.compose.ui.geometry.Offset) = Math.toDegrees(kotlin.math.atan2((o.y - size.height / 2f).toDouble(), (o.x - size.width / 2f).toDouble())).toFloat()
                detectDragGestures(
                    onDragStart = { o -> ringDrag = kotlin.math.hypot((o.x - size.width / 2f).toDouble(), (o.y - size.height / 2f).toDouble()) > size.width * RING_DRAG_MIN_FRACTION; if (ringDrag) { scrub = rawProgress; lastAng = ang(o) } },
                    onDrag = { change, _ -> if (ringDrag) { change.consume(); val a = ang(change.position); scrub = scrubFraction(scrub ?: rawProgress, lastAng, a); lastAng = a } },
                    onDragEnd = { scrub?.let { PlayerManager.seekTo((it * state.durationMs).toLong()) }; scrub = null },
                    onDragCancel = { scrub = null },
                )
            }
            .semantics { if (state.durationMs > 0) { progressBarRangeInfo = androidx.compose.ui.semantics.ProgressBarRangeInfo(rawProgress, 0f..1f); setProgress("Seek") { v -> PlayerManager.seekTo((v.coerceIn(0f, 1f) * state.durationMs).toLong()); true } } }.graphicsLayer { scaleX = dialScale; scaleY = dialScale; transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0f) }, ticks = 48, pointer = true, showProgress = fixture == null || progressInFixture, levels = levelsFn) {}
        com.abhi.musicapp.ui.components.CentreDisc(
            flipped = fixtureFlipped ?: flipped, onFlip = { if (fixture == null) flipped = !flipped }, onTap = { if (fixture == null) PlayerManager.togglePlayPause() },
            time = formatMs(if (scrub != null) (scrub!! * state.durationMs).toLong() else state.positionMs), total = formatTotal(state.durationMs),
            mono = mono, levels = levelsFn, tick = if (fixtureLevels == null && fixture == null && !reduced) frame else null,
            reducedMotion = reduced, isPlaying = state.isPlaying, fixtureRotation = fixtureRotation,
            modifier = Modifier.offset(116.5.dp, 135.dp + 150.5.dp * dialScale - 82.dp).size(164.dp).graphicsLayer { scaleX = dialScale; scaleY = dialScale },
        )
        Column(
            modifier = Modifier.offset(23.5.dp, (489.dp - shrink)).width(344.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(sourceLabel ?: if (fixture != null) "[Playlist/Source]" else "Local music", fontSize = 17.sp, fontWeight = FontWeight.Medium, color = InkBlack)
            Text(
                if (state.hasTrack) state.title else "Nothing playing",
                fontSize = 32.sp, fontWeight = FontWeight.Medium, color = InkBlack,
                maxLines = 1, softWrap = false, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, letterSpacing = (-0.8).sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
            )
        }
        Row(
            modifier = Modifier.offset(89.dp, (659.dp - shrink)),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.pressScale().size(63.dp).clip(CircleShape).background(PureWhite)
                    .tapOrHold({ PlayerManager.previous() }, { PlayerManager.startSeekHold(-1) }, { PlayerManager.stopSeekHold() })
                    .semantics { onClick("Previous track") { PlayerManager.previous(); true } },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous", tint = InkBlack) }
            Box(
                modifier = Modifier.pressScale().size(63.dp).clip(CircleShape)
                    .background(if (state.isPlaying) Orangs else InkBlack)
                    .clickable { PlayerManager.togglePlayPause() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (state.isPlaying) "Pause" else "Play",
                    tint = PureWhite, modifier = Modifier.size(28.dp),
                )
            }
            Box(
                modifier = Modifier.pressScale().size(63.dp).clip(CircleShape).background(PureWhite)
                    .tapOrHold({ PlayerManager.next() }, { PlayerManager.startSeekHold(1) }, { PlayerManager.stopSeekHold() })
                    .semantics { onClick("Next track") { PlayerManager.next(); true } },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.SkipNext, contentDescription = "Next", tint = InkBlack) }
        }
        Box(Modifier.offset(19.dp, (726.dp - shrink)).pressScale().size(72.dp).clip(CircleShape).background(circle).clickable { overlay = if (overlay == 3) 0 else 3 }, contentAlignment = Alignment.Center) {
            Icon(outIcon, contentDescription = outLabel, tint = InkBlack)
        }
        Row(
            modifier = Modifier.offset(139.dp, (757.dp - shrink)).width(112.dp).heightIn(min = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null, tint = InkBlack)
            Text("Queue", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = InkBlack, maxLines = 1, softWrap = false)
        }
        Box(Modifier.offset(298.dp, (726.dp - shrink)).pressScale().size(72.dp).clip(CircleShape).background(circle).clickable { overlay = if (overlay == 2) 0 else 2 }, contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.SelfImprovement, contentDescription = "Focus mode (icon pending his decision)", tint = InkBlack)
        }

        val sheet = androidx.compose.ui.graphics.Color(0xFFF6F6F4) // estimated popup fill
        val pill = com.abhi.musicapp.ui.theme.GrayLight
        if (overlay != 0) Box(Modifier.fillMaxSize().clickable(indication = null, interactionSource = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) { overlay = 0 })
        if (overlay == 1) Column(Modifier.offset(134.dp, 94.dp).width(232.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp)).background(sheet)) {
            // Equaliser is drawn greyed out in the Figma menu: kept disabled (no EQ exists). Add to playlist is wired (shipped path, PlaylistStore); other entries only close the menu until wired.
            listOf("Sleep Time", "Add to playlist", "Share", "Speed", "Delete", "Equaliser", "Lyrics").forEach { t ->
                val off = t == "Equaliser"
                Row(Modifier.fillMaxWidth().height(51.dp).then(if (off) Modifier else Modifier.clickable {
                    overlay = if (fixture != null) 0 else when (t) {
                        "Sleep Time" -> 5
                        "Add to playlist" -> 4
                        "Speed" -> 6
                        "Share" -> { shareCurrent(ctx, state.mediaId, state.title); 0 }
                        "Delete" -> { menuNote = "Delete is not built yet. It stays off so no song is removed by accident."; 7 }
                        "Lyrics" -> { menuNote = "Lyrics are not built yet: no lyrics source has been chosen."; 7 }
                        else -> 0
                    }
                }).padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(16.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(3.dp)).background(if (off) pill else com.abhi.musicapp.ui.theme.GrayText)) // glyphs not matched
                    androidx.compose.foundation.layout.Spacer(Modifier.width(10.dp))
                    Text(t, fontSize = 13.sp, color = if (off) com.abhi.musicapp.ui.theme.GrayText.copy(alpha = 0.6f) else InkBlack)
                }
            }
        }
        if (overlay == 5) SleepTimerSheet { overlay = 0 }
        if (overlay == 6) SpeedSheet { overlay = 0 }
        if (overlay == 7) Box(Modifier.align(Alignment.Center).padding(horizontal = 40.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp)).background(sheet).padding(20.dp)) {
            Text(menuNote, fontSize = 14.sp, color = InkBlack)
        }
        if (overlay == 4) AddToPlaylistSheet(state.mediaId) { overlay = 0 } // shipped path only (menu sets 4 only when fixture == null)
        if (overlay == 2 || overlay == 3) {
            val left = overlay == 3
            val icons = if (left) listOf(outIcon) else listOf(Icons.Filled.VolumeOff, Icons.Filled.DarkMode)
            val n = icons.size
            val x = if (left) 19.dp else 298.dp
            Box(Modifier.offset(x, 726.dp - shrink - (70.dp * n) - 16.dp).size(72.dp, 70.dp * n + 88.dp).clip(CircleShape).background(sheet))
            icons.forEachIndexed { i, ic ->
                // Buttons are visual only: no device list or audio-route capability exists in the app yet.
                Box(Modifier.offset(x + 4.dp, 726.dp - shrink - (70.dp * (n - i)) + 4.dp - 6.dp).size(64.dp).clip(CircleShape).background(pill).then(if (left && fixture == null) Modifier.clickable(onClickLabel = "Choose output") { outNote = com.abhi.musicapp.playback.AudioOutput.openPicker(ctx) } else Modifier), contentAlignment = Alignment.Center) {
                    Icon(ic, contentDescription = null, tint = InkBlack, modifier = Modifier.size(22.dp))
                }
            }
            if (left) androidx.compose.material3.Text(
                (outLabel.removePrefix("Audio output: ").replaceFirstChar { it.uppercase() }) + " - tap to choose output" + (outNote?.let { "\n$it" } ?: if (outKind == com.abhi.musicapp.playback.OutputKind.UNKNOWN) "\nCurrent output unknown on this Android version" else ""),
                fontSize = 12.sp, color = InkBlack, modifier = Modifier.offset(x, 726.dp - shrink - 70.dp - 16.dp - 56.dp).width(260.dp))
            Box(Modifier.offset(x, 726.dp - shrink).size(72.dp).clip(CircleShape).background(InkBlack).clickable { overlay = 0 }, contentAlignment = Alignment.Center) {
                Icon(if (left) outIcon else Icons.Filled.Close, contentDescription = "Close", tint = androidx.compose.ui.graphics.Color.White)
            }
        }
    }
}

/** Share the current audio file through the Android share sheet. Sends only the file reference he picks a target for. */
internal fun shareCurrent(ctx: android.content.Context, mediaId: Long, title: String) {
    if (mediaId < 0) return
    val uri = android.content.ContentUris.withAppendedId(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, mediaId)
    val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "audio/*"; putExtra(android.content.Intent.EXTRA_STREAM, uri); putExtra(android.content.Intent.EXTRA_TITLE, title)
        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    ctx.startActivity(android.content.Intent.createChooser(send, "Share song").addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
}

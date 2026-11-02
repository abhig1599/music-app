package com.abhi.musicapp.ui.screens

import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
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
fun PlayerScreen(onBack: () -> Unit, fixture: PlayerUiState? = null, initialOverlay: Int = 0, sourceLabel: String? = null) {
    // 0 none, 1 three-dots menu, 2 focus-mode popup, 3 output-device popup (Figma 'Song Page - menu', 'Song focus mode', 'Song Focus connected device').
    var overlay by androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(initialOverlay) }
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
    var scrub by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<Float?>(null) }
    val progress = if (scrub != null) scrub!! else if (fixture != null || reducedMotion()) rawProgress else
        androidx.compose.animation.core.animateFloatAsState(rawProgress, androidx.compose.animation.core.tween(260, easing = androidx.compose.animation.core.LinearEasing), label = "dial").value
    val circle = com.abhi.musicapp.ui.theme.SurfaceSoft

    val mono = com.abhi.musicapp.ui.theme.GeistMono // Figma: Geist Mono (not bundled until licence check)
    // Absolute layout from exact Design-panel reads (390x844 frame, see DECISIONS.md 02:40).
    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize()) {
        // Reference viewport is 390x844. Shorter areas (system bars, small phones): the top group stays
        // anchored to the top, the title/controls/bottom group anchors to the bottom, and the dial group scales
        // down only if the two would collide. At 844dp this is identical to the Figma positions.
        val shrink = (844.dp - maxHeight).coerceAtLeast(0.dp)
        val dialScale = ((338f - shrink.value) / 301f).coerceIn(0.5f, 1f)
        Row(
            modifier = Modifier.offset(26.dp, 46.dp).size(344.dp, 37.dp),
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
                    onDragStart = { o -> scrub = rawProgress; lastAng = ang(o) },
                    onDrag = { change, _ -> change.consume(); val a = ang(change.position); scrub = scrubFraction(scrub ?: rawProgress, lastAng, a); lastAng = a },
                    onDragEnd = { scrub?.let { PlayerManager.seekTo((it * state.durationMs).toLong()) }; scrub = null },
                    onDragCancel = { scrub = null },
                )
            }
            .semantics { if (state.durationMs > 0) { progressBarRangeInfo = androidx.compose.ui.semantics.ProgressBarRangeInfo(rawProgress, 0f..1f); setProgress("Seek") { v -> PlayerManager.seekTo((v.coerceIn(0f, 1f) * state.durationMs).toLong()); true } } }.graphicsLayer { scaleX = dialScale; scaleY = dialScale; transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0f) }, ticks = 48, pointer = true, showProgress = fixture == null) {}
        Column(
            modifier = Modifier.offset(147.dp, 135.dp + (252.dp - 135.dp) * dialScale).width(104.dp).wrapContentHeight(unbounded = true, align = Alignment.Top).graphicsLayer { scaleX = dialScale; scaleY = dialScale },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Text(formatMs(if (scrub != null) (scrub!! * state.durationMs).toLong() else state.positionMs), fontSize = 35.68.sp, fontFamily = mono, color = InkBlack)
                Text("M", fontSize = 20.sp, color = InkBlack)
            }
            Box(Modifier.size(width = 104.dp, height = 1.dp).background(GrayLight))
            Text(formatTotal(state.durationMs), fontSize = 16.51.sp, fontFamily = mono, color = InkBlack)
        }
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
                modifier = Modifier.fillMaxWidth(),
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
            modifier = Modifier.offset(154.5.dp, (757.dp - shrink)).size(79.dp, 24.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null, tint = InkBlack)
            Text("Queue", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = InkBlack)
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
                Row(Modifier.fillMaxWidth().height(51.dp).then(if (off) Modifier else Modifier.clickable { overlay = if (t == "Add to playlist" && fixture == null) 4 else 0 }).padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(16.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(3.dp)).background(if (off) pill else com.abhi.musicapp.ui.theme.GrayText)) // glyphs not matched
                    androidx.compose.foundation.layout.Spacer(Modifier.width(10.dp))
                    Text(t, fontSize = 13.sp, color = if (off) com.abhi.musicapp.ui.theme.GrayText.copy(alpha = 0.6f) else InkBlack)
                }
            }
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
                fontSize = 12.sp, color = InkBlack, modifier = Modifier.offset(x + 80.dp, 726.dp - shrink - 70.dp - 6.dp).width(200.dp))
            Box(Modifier.offset(x, 726.dp - shrink).size(72.dp).clip(CircleShape).background(InkBlack).clickable { overlay = 0 }, contentAlignment = Alignment.Center) {
                Icon(if (left) outIcon else Icons.Filled.Close, contentDescription = "Close", tint = androidx.compose.ui.graphics.Color.White)
            }
        }
    }
}

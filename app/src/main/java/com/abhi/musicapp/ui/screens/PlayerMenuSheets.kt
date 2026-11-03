package com.abhi.musicapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhi.musicapp.playback.PlayerManager
import com.abhi.musicapp.playback.SleepState
import com.abhi.musicapp.playback.SleepTimer
import com.abhi.musicapp.ui.theme.GeistMono
import com.abhi.musicapp.ui.theme.GrayLight
import com.abhi.musicapp.ui.theme.GrayText
import com.abhi.musicapp.ui.theme.InkBlack
import com.abhi.musicapp.ui.theme.Orangs

private val SheetFill = Color(0xFFF6F6F4)
internal val SLEEP_PRESETS_MIN = listOf(10, 20, 30, 45, 60)
internal val SPEED_CHOICES = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)

/** "m:ss" with minutes allowed past 59 (the Figma chips show "10:01 M"). */
internal fun clockText(ms: Long): String { val t = (ms.coerceAtLeast(0) + 500) / 1000; return "%d:%02d".format(t / 60, t % 60) }

/** Custom time from the H and M boxes. Null when it is not a usable time (empty, 0, or over 24 h). */
internal fun customSleepMs(h: String, m: String): Long? {
    val hh = if (h.isEmpty()) 0 else h.toIntOrNull() ?: return null
    val mm = if (m.isEmpty()) 0 else m.toIntOrNull() ?: return null
    if (hh < 0 || mm < 0 || mm > 59) return null
    val total = (hh * 60L + mm) * 60_000L
    return if (total <= 0L || total > 24 * 3_600_000L) null else total
}

@Composable
private fun BottomSheet(heightDp: Int, onDismiss: () -> Unit, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxSize().clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) { onDismiss() }) {
        Column(
            Modifier.align(Alignment.BottomCenter).imePadding().fillMaxWidth().height(heightDp.dp).clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)).background(SheetFill)
                .clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) {}
                .padding(horizontal = 24.dp),
        ) {
            Box(Modifier.padding(top = 8.dp).align(Alignment.CenterHorizontally).size(width = 28.dp, height = 3.dp).clip(CircleShape).background(GrayLight))
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

/** Figma "Sleep concept- set up": Turn off, 10/20/30/45 min, 1 hour, + custom, and "Play last one to end". */
@Composable
fun SleepTimerSheet(onDismiss: () -> Unit) {
    val state by SleepTimer.state.collectAsState()
    val lastToEnd by SleepTimer.playLastToEnd.collectAsState()
    var custom by remember { mutableStateOf(false) }
    if (custom) { CustomSleepSheet(onDismiss = onDismiss, onBack = { custom = false }); return }
    BottomSheet(heightDp = 540, onDismiss = onDismiss) {
        Text("Sleep timer", fontSize = 17.sp, fontWeight = FontWeight.Medium, color = InkBlack)
        Spacer(Modifier.height(4.dp))
        Text("When time is up: the music pauses, then this app closes after a 10 second countdown (tap x to cancel). The timer is lost if Android closes the app.", fontSize = 11.sp, color = Orangs)
        Spacer(Modifier.height(8.dp))
        @Composable fun row(label: String, selected: Boolean, onClick: () -> Unit) {
            Row(Modifier.fillMaxWidth().height(40.dp).clickable(onClick = onClick), verticalAlignment = Alignment.CenterVertically) {
                Text(label, fontFamily = GeistMono, fontSize = 13.sp, color = if (selected) Orangs else InkBlack, modifier = Modifier.weight(1f))
                if (selected) Icon(Icons.Filled.Check, null, tint = Orangs, modifier = Modifier.size(18.dp))
            }
        }
        row("Turn off", state == SleepState.Off) { SleepTimer.cancel(); onDismiss() }
        SLEEP_PRESETS_MIN.forEach { m ->
            val label = if (m == 60) "1 hour later" else "$m min. later"
            val sel = (state as? SleepState.Counting)?.totalMs == m * 60_000L
            row(label, sel) { SleepTimer.start(m * 60_000L); onDismiss() }
        }
        Row(Modifier.fillMaxWidth().height(40.dp).clickable { custom = true }, verticalAlignment = Alignment.CenterVertically) {
            Text("+ custom", fontFamily = GeistMono, fontSize = 13.sp, color = InkBlack, modifier = Modifier.weight(1f))
            Text("set >", fontFamily = GeistMono, fontSize = 12.sp, color = GrayText)
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Play last one to end", fontFamily = GeistMono, fontSize = 13.sp, color = InkBlack)
                Text("When time is up, the current song finishes first", fontSize = 11.sp, color = GrayText)
            }
            Switch(checked = lastToEnd, onCheckedChange = { SleepTimer.playLastToEnd.value = it },
                colors = SwitchDefaults.colors(checkedTrackColor = Orangs, checkedThumbColor = Color.White))
        }
    }
}

/** Figma "Sleep concept - custom no.": hours and minutes boxes and Continue. */
@Composable
internal fun CustomSleepSheet(onDismiss: () -> Unit, onBack: () -> Unit, initialH: String = "", initialM: String = "") {
    var h by remember { mutableStateOf(initialH) }
    var m by remember { mutableStateOf(initialM) }
    val ms = customSleepMs(h, m)
    BottomSheet(heightDp = 360, onDismiss = onDismiss) {
        Text("Custom time", fontSize = 17.sp, fontWeight = FontWeight.Medium, color = InkBlack)
        Spacer(Modifier.height(4.dp))
        Text("When time is up the music pauses, then this app closes after 10 seconds (tap x to cancel).", fontSize = 11.sp, color = Orangs)
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            @Composable fun box(v: String, unit: String, set: (String) -> Unit, modifier: Modifier) {
                Row(modifier.height(56.dp).clip(RoundedCornerShape(10.dp)).background(GrayLight.copy(alpha = 0.5f)).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    BasicTextField(v, { set(it.filter(Char::isDigit).take(2)) }, singleLine = true,
                        textStyle = TextStyle(fontFamily = GeistMono, fontSize = 24.sp, color = if (v.isEmpty()) GrayText else InkBlack),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), cursorBrush = SolidColor(Orangs),
                        decorationBox = { inner -> Box { if (v.isEmpty()) Text("00", fontFamily = GeistMono, fontSize = 24.sp, color = GrayText); inner() } },
                        modifier = Modifier.weight(1f))
                    Text(unit, fontFamily = GeistMono, fontSize = 13.sp, color = GrayText)
                }
            }
            box(h, "H", { h = it }, Modifier.weight(1f))
            box(m, "M", { m = it }, Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Text(if (ms == null && (h.isNotEmpty() || m.isNotEmpty())) "Use 1 minute to 24 hours (minutes 0-59)." else "", fontSize = 11.sp, color = GrayText)
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(10.dp)).background(if (ms != null) Orangs else GrayLight)
            .then(if (ms != null) Modifier.clickable { SleepTimer.start(ms); onDismiss() } else Modifier), contentAlignment = Alignment.Center) {
            Text("Continue", fontFamily = GeistMono, fontSize = 14.sp, color = if (ms != null) Color.White else GrayText)
        }
        Spacer(Modifier.height(10.dp))
        Text("Back", fontSize = 13.sp, color = GrayText, modifier = Modifier.align(Alignment.CenterHorizontally).clickable(onClick = onBack).padding(8.dp))
    }
}

/** Figma "Speed" sheet, wired to Media3 playback speed. Applies to the current and following songs until changed. */
@Composable
fun SpeedSheet(onDismiss: () -> Unit) {
    val st by PlayerManager.state.collectAsState()
    BottomSheet(heightDp = 374, onDismiss = onDismiss) {
        Text("Speed", fontSize = 17.sp, fontWeight = FontWeight.Medium, color = InkBlack)
        Spacer(Modifier.height(10.dp))
        SPEED_CHOICES.forEach { sp ->
            val sel = kotlin.math.abs(st.speed - sp) < 0.01f
            val label = if (sp == 1.0f) "1.0x (Default)" else "${sp}x".replace("0.5x", "0.5x")
            Row(Modifier.fillMaxWidth().height(40.dp).clickable { PlayerManager.setSpeed(sp); onDismiss() }, verticalAlignment = Alignment.CenterVertically) {
                Text(label, fontFamily = GeistMono, fontSize = 13.sp, color = if (sel) Orangs else InkBlack, modifier = Modifier.weight(1f))
                if (sel) Icon(Icons.Filled.Check, null, tint = Orangs, modifier = Modifier.size(18.dp))
            }
        }
    }
}

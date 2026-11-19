package com.abhi.musicapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhi.musicapp.playback.PlayerManager
import com.abhi.musicapp.ui.components.ReorderColumn
import com.abhi.musicapp.ui.components.moveItem
import com.abhi.musicapp.ui.theme.GrayLight
import com.abhi.musicapp.ui.theme.GrayText
import com.abhi.musicapp.ui.theme.InkBlack
import com.abhi.musicapp.ui.theme.Orangs

/** One queue entry: title, artist. */
data class QueueEntry(val id: String, val title: String, val artist: String)

/**
 * Player "Queue" sheet (Figma queue frame styling: grey sheet, bordered rows, left handle glyph). Shows the real play queue;
 * the three-line handle drags a song to a new place, tapping a row plays it. The orange dot marks the song playing now.
 * Moving only changes the queue, never a playlist or a file.
 */
@Composable
fun QueueSheet(onClose: () -> Unit) {
    var snap by remember { mutableStateOf(PlayerManager.queueSnapshot()) }
    QueueSheetContent(
        items = snap.first.map { QueueEntry(it.first, it.second, it.third) }, current = snap.second,
        onMove = { from, to -> PlayerManager.moveQueueItem(from, to); snap = PlayerManager.queueSnapshot() },
        onPlay = { i -> PlayerManager.playQueueIndex(i); snap = PlayerManager.queueSnapshot() },
        onClose = onClose,
    )
}

@Composable
fun QueueSheetContent(items: List<QueueEntry>, current: Int, onMove: (Int, Int) -> Unit, onPlay: (Int) -> Unit, onClose: () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize().background(Color(0x66000000)).clickable { onClose() }) {
        // Sheet height follows the screen (about 58%, between 300 and 480 dp) so the dial stays visible and short screens still fit.
        // Rows grow with the user's font size (up to 1.6x) so larger text is not clipped.
        val fs = androidx.compose.ui.platform.LocalDensity.current.fontScale.coerceIn(1f, 1.6f)
        val sheetHeight = (maxHeight * 0.58f).coerceIn(300.dp, 480.dp)
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(sheetHeight).clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(Color(0xFFF6F6F4)).clickable(enabled = false) {}.padding(horizontal = 12.dp),
        ) {
            Box(Modifier.padding(top = 8.dp).align(Alignment.CenterHorizontally).size(width = 28.dp, height = 3.dp).clip(CircleShape).background(GrayLight))
            Spacer(Modifier.height(16.dp))
            Text("Queue", fontSize = 17.sp, fontWeight = FontWeight.Medium, color = InkBlack)
            Spacer(Modifier.height(14.dp))
            if (items.isEmpty()) Text("Nothing is queued. Play a song or open a playlist first.", fontSize = 14.sp, color = GrayText)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                ReorderColumn(items, rowHeight = (70f * fs).dp, onMove = onMove) { e, i, handle, dragging ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 3.dp).height((64f * fs).dp).clip(RoundedCornerShape(8.dp))
                            .background(if (dragging) Color(0xFFFFFFFF) else Color.Transparent)
                            .border(1.dp, if (dragging) Orangs else GrayLight, RoundedCornerShape(8.dp))
                            .clickable { onPlay(i) }.padding(start = 20.dp, end = 6.dp), // inner padding; the 3dp gap between rows is unchanged
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(28.dp).clip(RoundedCornerShape(6.dp)).background(GrayLight)) // album photo not shipped
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(e.title, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = InkBlack, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (e.artist.isNotBlank()) Text(e.artist, fontSize = 11.sp, color = GrayText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        if (i == current) Box(Modifier.padding(end = 4.dp).size(10.dp).clip(CircleShape).background(Orangs))
                        Icon(Icons.Filled.DragHandle, contentDescription = "Drag to reorder ${e.title}", tint = if (dragging) Orangs else GrayText,
                            modifier = Modifier.size(42.dp).then(handle).padding(11.dp).testTag("qhandle_$i"))
                    }
                }
            }
            Spacer(Modifier.height(12.dp).navigationBarsPadding())
        }
    }
}

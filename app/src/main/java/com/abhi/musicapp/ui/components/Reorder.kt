package com.abhi.musicapp.ui.components

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt

/** New list with the item at [from] moved to position [to] (both clamped to the list). */
internal fun <T> moveItem(list: List<T>, from: Int, to: Int): List<T> {
    if (list.isEmpty() || from !in list.indices) return list
    val t = to.coerceIn(0, list.size - 1)
    val m = list.toMutableList(); val x = m.removeAt(from); m.add(t, x); return m
}

/** Where a row dragged from [from] by [dyPx] lands, for rows [rowPx] tall in a list of [size]. */
internal fun dropIndex(from: Int, dyPx: Float, rowPx: Float, size: Int): Int =
    (from + (dyPx / rowPx).roundToInt()).coerceIn(0, (size - 1).coerceAtLeast(0))

/**
 * Column of fixed-height rows that can be re-ordered by dragging a handle (the "three lines" on the right).
 * While dragging, the other rows slide to make room and the dragged row follows the finger; the new order is
 * reported once, on release, through [onMove] (from, to). Give the row's handle the [handle] modifier it receives.
 */
@Composable
fun <T> ReorderColumn(
    items: List<T>,
    rowHeight: Dp,
    onMove: (from: Int, to: Int) -> Unit,
    modifier: Modifier = Modifier,
    row: @Composable (item: T, index: Int, handle: Modifier, dragging: Boolean) -> Unit,
) {
    val rowPx = with(LocalDensity.current) { rowHeight.toPx() }
    var dragFrom by remember { mutableIntStateOf(-1) }
    var dy by remember { mutableFloatStateOf(0f) }
    val size by rememberUpdatedState(items.size)
    val move by rememberUpdatedState(onMove)
    val target = if (dragFrom >= 0) dropIndex(dragFrom, dy, rowPx, items.size) else -1
    val order = if (dragFrom >= 0) moveItem(items.indices.toList(), dragFrom, target) else items.indices.toList()
    Column(modifier) {
        order.forEach { idx ->
            androidx.compose.runtime.key(idx) {
                val dragging = idx == dragFrom
                val handle = Modifier.pointerInput(idx) {
                    detectDragGestures(
                        onDragStart = { dragFrom = idx; dy = 0f },
                        onDrag = { change, d -> change.consume(); dy += d.y },
                        onDragEnd = { val to = dropIndex(dragFrom, dy, rowPx, size); val from = dragFrom; dragFrom = -1; dy = 0f; if (from >= 0 && to != from) move(from, to) },
                        onDragCancel = { dragFrom = -1; dy = 0f },
                    )
                }
                Box(
                    Modifier.height(rowHeight).zIndex(if (dragging) 1f else 0f)
                        .graphicsLayer { translationY = if (dragging) dy - (target - dragFrom) * rowPx else 0f },
                ) { row(items[idx], idx, handle, dragging) }
            }
        }
    }
}

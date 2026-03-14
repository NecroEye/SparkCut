package com.muratcangzm.editor.ui.reorder

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Modifier

class EditorReorderState(
    private val listState: LazyListState,
    private val onMove: (fromIndex: Int, toIndex: Int) -> Unit,
) {
    var draggingIndex by mutableStateOf<Int?>(null)
        private set

    var draggingItemOffset by mutableFloatStateOf(0f)
        private set

    private var initialDraggingItem by mutableStateOf<LazyListItemInfo?>(null)
    private var currentIndex by mutableIntStateOf(-1)

    fun onDragStart(index: Int) {
        val itemInfo = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
        draggingIndex = index
        currentIndex = index
        initialDraggingItem = itemInfo
        draggingItemOffset = 0f
    }

    fun onDragInterrupted() {
        draggingIndex = null
        currentIndex = -1
        initialDraggingItem = null
        draggingItemOffset = 0f
    }

    fun onDrag(change: PointerInputChange, dragAmountY: Float) {
        change.consume()
        draggingItemOffset += dragAmountY

        val startItem = initialDraggingItem ?: return
        val draggingCenter = startItem.offset + draggingItemOffset + (startItem.size / 2f)

        val targetItem = listState.layoutInfo.visibleItemsInfo
            .firstOrNull { item ->
                item.index != currentIndex &&
                        draggingCenter.toInt() in item.offset..(item.offset + item.size)
            }

        if (targetItem != null && currentIndex >= 0) {
            val from = currentIndex
            val to = targetItem.index
            onMove(from, to)
            currentIndex = to
            draggingIndex = to
        }
    }

    fun offsetFor(index: Int): Float {
        return if (draggingIndex == index) draggingItemOffset else 0f
    }
}

fun Modifier.reorderDragHandle(
    index: Int,
    state: EditorReorderState,
): Modifier {
    return pointerInput(index) {
        detectDragGesturesAfterLongPress(
            onDragStart = {
                state.onDragStart(index)
            },
            onDragEnd = {
                state.onDragInterrupted()
            },
            onDragCancel = {
                state.onDragInterrupted()
            },
            onDrag = { change, dragAmount ->
                state.onDrag(change, dragAmount.y)
            },
        )
    }
}
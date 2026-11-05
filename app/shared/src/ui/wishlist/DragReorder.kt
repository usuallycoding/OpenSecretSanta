package com.usuallycoding.opensecretsanta.ui.wishlist

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * An item being dragged, from the moment the handle is held until the list has the order that the drop saved.
 * [offset] is how far the row has been pulled from its resting place (including what the page scrolled meanwhile),
 * [pointerY] where the pointer is on the screen, and [startIds] the order the drag began with: the drag is over
 * for the screen as soon as the list stops having that order.
 */
@Stable
internal class ItemDrag {
    var id by mutableStateOf<Long?>(null)
    var startIds by mutableStateOf<List<Long>>(emptyList())
    var offset by mutableFloatStateOf(0f)
    var pointerY by mutableFloatStateOf(0f)

    /** Set once the item was dropped: it settles over [target] while the order is saved. */
    var settling by mutableStateOf(false)
    var target by mutableStateOf(0)

    val active: Boolean get() = id != null

    fun begin(id: Long, pointerY: Float, ids: List<Long>) {
        this.id = id
        startIds = ids
        offset = 0f
        this.pointerY = pointerY
        settling = false
    }

    fun clear() {
        id = null
        startIds = emptyList()
        offset = 0f
        settling = false
    }
}

/**
 * The slot a dragged row is over. [tops] and [heights] are those of the rows in their resting places, [from] is the
 * dragged row and [offset] how far it has been pulled from its place; it takes the place of each neighbour whose
 * middle its own middle has passed, so rows of different heights swap where the eye expects them to.
 */
internal fun dragTargetIndex(tops: List<Float>, heights: List<Float>, from: Int, offset: Float): Int {
    val middle = tops[from] + heights[from] / 2 + offset
    var target = from
    for (j in from + 1..tops.lastIndex) if (middle > tops[j] + heights[j] / 2) target = j else break
    for (j in from - 1 downTo 0) if (middle < tops[j] + heights[j] / 2) target = j else break
    return target
}

/** How far the resting row [index] moves to make room while row [from] is over slot [target]; [span] is the dragged row's height plus the gap. */
internal fun rowShift(index: Int, from: Int, target: Int, span: Float): Float = when {
    index in from + 1..target -> -span
    index in target until from -> span
    else -> 0f
}

/** The number the row at [index] shows while row [from] is over slot [target]: the dragged row the place it is over, the rows it passed the place they are making room as, the rest their own. */
internal fun shownNumber(index: Int, from: Int, target: Int): Int = when {
    index == from -> target + 1
    index in from + 1..target -> index
    index in target until from -> index + 2
    else -> index + 1
}

/** The offset at which row [from], dropped over slot [target], rests: where it sits once the rows between have made room (the gap between rows is the same before and after). */
internal fun settleOffset(tops: List<Float>, heights: List<Float>, from: Int, target: Int): Float = when {
    target > from -> tops[target] + heights[target] - heights[from] - tops[from]
    target < from -> tops[target] - tops[from]
    else -> 0f
}

/** The pixels per frame to scroll while the pointer is at [y]: none in the middle, growing to [maxSpeed] at the edge of the [top, bottom] viewport (up is negative). */
internal fun edgeScrollSpeed(y: Float, top: Float, bottom: Float, band: Float, maxSpeed: Float): Float {
    val intoTop = (top + band - y) / band
    val intoBottom = (y - (bottom - band)) / band
    return when {
        intoTop > 0f -> -maxSpeed * intoTop.coerceAtMost(1f)
        intoBottom > 0f -> maxSpeed * intoBottom.coerceAtMost(1f)
        else -> 0f
    }
}

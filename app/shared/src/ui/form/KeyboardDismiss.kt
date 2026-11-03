package com.usuallycoding.opensecretsanta.ui.form

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

/** What a finger did, once it is clear: nothing yet, a drag nothing used, or a tap nothing handled. */
internal enum class Gesture { None, Drag, Tap }

/**
 * Tells taps from drags in a stream of pointer events that have already passed the children. It holds no UI: it is fed the
 * position, whether the finger is down, whether a child used the event, and when the event happened.
 *
 * A drag is reported once, the first time the finger is past [slop] from where it went down and nothing used that move; a
 * scroll or a text selection uses its moves, so only a drag that would otherwise do nothing is reported. A tap is
 * reported on release when the finger never went past the slop and nothing used any event of the touch, which is what a
 * button, a card or a text field does with its own tap.
 */
internal class GestureTracker {
    private var origin: Offset? = null
    private var moved = false
    private var used = false
    private var dragReported = false

    /** When the current touch went down, to match it with the text field that saw the same touch; -1 between touches. */
    var downAt: Long = -1L
        private set

    fun onEvent(position: Offset, pressed: Boolean, consumed: Boolean, uptime: Long, slop: Float): Gesture {
        if (pressed) {
            val start = origin
            if (start == null) {
                origin = position
                downAt = uptime
                used = consumed
                return Gesture.None
            }
            used = used || consumed
            if (!moved && (position - start).getDistance() > slop) moved = true
            if (moved && !consumed && !dragReported) {
                dragReported = true
                return Gesture.Drag
            }
            return Gesture.None
        }
        val ended = origin != null
        val tap = ended && !moved && !(used || consumed)
        origin = null
        moved = false
        used = false
        dragReported = false
        downAt = -1L
        return if (tap) Gesture.Tap else Gesture.None
    }
}

/**
 * Where the app's text fields say that a touch started on them, by the time of the touch's first event. A touch that
 * started in a field is the field's own (moving the cursor, selecting, scrolling its text), so it never closes the keyboard.
 */
internal class TouchGuard {
    var fieldDownAt: Long = -1L
}

private val NoGuard = TouchGuard()

internal val LocalTouchGuard = staticCompositionLocalOf { NoGuard }

/** Put on a text field: records the touches that start on it, before anything above it looks at them. */
@Composable
internal fun Modifier.touchesOfAField(): Modifier {
    val guard = LocalTouchGuard.current
    return pointerInput(guard) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                event.changes.firstOrNull { it.changedToDown() }?.let { guard.fieldDownAt = it.uptimeMillis }
            }
        }
    }
}

/**
 * Reports the drags and taps that nothing below used, without consuming anything: it watches after the children. [onTap]
 * may be null when taps are not wanted.
 */
internal fun Modifier.watchGestures(tracker: GestureTracker, onDrag: () -> Unit, onTap: (() -> Unit)?): Modifier = pointerInput(tracker) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Final)
            val change = event.changes.firstOrNull() ?: continue
            val pressed = event.changes.any { it.pressed }
            val gesture = tracker.onEvent(change.position, pressed, event.changes.any { it.isConsumed }, change.uptimeMillis, viewConfiguration.touchSlop)
            when (gesture) {
                Gesture.Drag -> onDrag()
                Gesture.Tap -> onTap?.invoke()
                Gesture.None -> Unit
            }
        }
    }
}

/**
 * Calls [onScroll] when the person's own vertical scroll reaches here. With [countConsumed] a scroll the content used
 * counts as well, which is how a list that scrolls also closes the keyboard; without it only what the content could not
 * scroll counts. Horizontal scrolling is left out: it is how a single-line field scrolls its own text.
 */
internal fun scrollHook(countConsumed: Boolean, onScroll: () -> Unit): NestedScrollConnection = object : NestedScrollConnection {
    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        if (source == NestedScrollSource.UserInput && (available.y != 0f || (countConsumed && consumed.y != 0f))) onScroll()
        return Offset.Zero
    }
}

/**
 * The root of the app: a swipe or drag in any direction, a vertical scroll of a list and a tap that nothing handled close
 * the keyboard and clear the focus, unless the touch started in a text field. It does not depend on the keyboard's size,
 * which the web does not report, and with no keyboard open it only clears the focus of a field.
 */
@Composable
fun KeyboardDismissRoot(content: @Composable () -> Unit) {
    val focus by rememberUpdatedState(LocalFocusManager.current)
    val keyboard by rememberUpdatedState(LocalSoftwareKeyboardController.current)
    val guard = remember { TouchGuard() }
    val tracker = remember { GestureTracker() }
    // a touch that began in a field is the field's own, whatever it goes on to do
    fun close() {
        if (guard.fieldDownAt != -1L && guard.fieldDownAt == tracker.downAt) return
        focus.clearFocus()
        keyboard?.hide()
    }
    val connection = remember(tracker, guard) { scrollHook(countConsumed = true, onScroll = { close() }) }
    Box(Modifier.fillMaxSize().nestedScroll(connection).watchGestures(tracker, onDrag = { close() }, onTap = { close() })) {
        CompositionLocalProvider(LocalTouchGuard provides guard) { content() }
    }
}

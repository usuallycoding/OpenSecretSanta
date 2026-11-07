package com.usuallycoding.opensecretsanta.ui.layout

import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf

/**
 * The scrollers of the screens that are shown, newest last. A page keeps a readable width, so beside it there is empty space that
 * has no scroller of its own; the shell puts one there and lets it scroll the page that is shown, which is the newest registered.
 * Used from the main thread only.
 */
@Stable
class OutsideScroll {
    private val shown = mutableListOf<ScrollableState>()

    /** The scroller of the page that is shown now, or null when none has registered. */
    val target: ScrollableState? get() = shown.lastOrNull()

    fun add(state: ScrollableState) {
        shown += state
    }

    fun remove(state: ScrollableState) {
        val index = shown.indexOfLast { it === state }
        if (index >= 0) shown.removeAt(index)
    }

    /** Scrolls the page that is shown by [delta], in the direction its own scrolling takes; returns how much it used. */
    fun scrollBy(delta: Float): Float = target?.dispatchRawDelta(delta) ?: 0f
}

/** Set by the signed-in app's shell; null elsewhere, where there is nothing to forward to. */
val LocalOutsideScroll = compositionLocalOf<OutsideScroll?> { null }

/** Lets the empty space beside the page scroll [state] while the caller is shown, and gives that back when it leaves. */
@Composable
fun ForwardOutsideScroll(state: ScrollableState) {
    val host = LocalOutsideScroll.current ?: return
    DisposableEffect(host, state) {
        host.add(state)
        onDispose { host.remove(state) }
    }
}

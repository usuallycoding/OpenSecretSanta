package com.usuallycoding.opensecretsanta.ui.form

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState

/** The state of a bottom sheet that opens at full height, with no half-open stop in between: hidden or expanded. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberAppSheetState(confirmValueChange: (SheetValue) -> Boolean = { true }): SheetState = rememberBottomSheetState(
    initialValue = SheetValue.Hidden,
    enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
    confirmValueChange = confirmValueChange,
)

/** Lets the content of a sheet, which lives in the sheet's own window, hide the keyboard for code outside it. */
private class KeyboardHider {
    var hide: () -> Unit = {}
}

/**
 * The app's modal bottom sheet. While the keyboard is open it is only a keyboard that can be put away: a tap on the dimmed
 * area, a swipe down on the sheet and any drag on the sheet that nothing else used hide the keyboard and leave the sheet
 * where it is; the sheet's own dragging is off until the keyboard is gone. With the keyboard closed it is a plain
 * [ModalBottomSheet] that opens at full height, so the sheets with no text field never notice.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBottomSheet(onDismissRequest: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    // read inside the sheet's own window: the keyboard's insets go to the window that has the focus, which is the sheet's,
    // so the window around it sees none
    var keyboardOpen by remember { mutableStateOf(false) }
    val hider = remember { KeyboardHider() }
    val state = rememberAppSheetState(confirmValueChange = { target ->
        // closing is asked for by a tap on the dimmed area and by a swipe down; with the keyboard open that only hides it
        if (target == SheetValue.Hidden && keyboardOpen) {
            hider.hide()
            false
        } else {
            true
        }
    })
    ModalBottomSheet(onDismissRequest = onDismissRequest, sheetState = state, sheetGesturesEnabled = !keyboardOpen) {
        val focus = LocalFocusManager.current
        val keyboard = LocalSoftwareKeyboardController.current
        val imeShown = WindowInsets.ime.getBottom(LocalDensity.current) > 0
        SideEffect {
            hider.hide = { focus.clearFocus(); keyboard?.hide() }
            keyboardOpen = imeShown
        }
        Column(Modifier.fillMaxWidth().hideKeyboardOnLooseDrag({ keyboardOpen }, hider)) { content() }
    }
}

/**
 * Hides the keyboard when the person drags a finger, in any direction, and nothing used the drag: a field that scrolls or
 * selects its text and a button consume it, so only a drag that would otherwise do nothing counts. A form's own scrolling
 * consumes every vertical drag, even when there is nothing to scroll, so a vertical drag is judged by what is left of it:
 * when the content could not scroll that far the rest reaches the nested-scroll hook here, and the keyboard goes.
 * Both only watch; neither consumes anything.
 */
@Composable
private fun Modifier.hideKeyboardOnLooseDrag(keyboardOpen: () -> Boolean, hider: KeyboardHider): Modifier {
    val open by rememberUpdatedState(keyboardOpen)
    val connection = remember(hider) {
        object : NestedScrollConnection {
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && available.y != 0f && open()) hider.hide()
                return Offset.Zero
            }
        }
    }
    return nestedScroll(connection).watchLooseDrags(keyboardOpen, hider)
}

private fun Modifier.watchLooseDrags(keyboardOpen: () -> Boolean, hider: KeyboardHider): Modifier = pointerInput(hider) {
    awaitPointerEventScope {
        var origin: Offset? = null
        var hidden = false
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Final)
            val change = event.changes.firstOrNull() ?: continue
            if (event.changes.none { it.pressed }) {
                origin = null
                hidden = false
                continue
            }
            val start = origin ?: change.position.also { origin = it }
            if (!hidden && keyboardOpen() && event.changes.none { it.isConsumed } && (change.position - start).getDistance() > viewConfiguration.touchSlop) {
                hider.hide()
                hidden = true
            }
        }
    }
}

/**
 * Lets the system's back (the Android button or gesture) close an open sheet before it leaves the screen: while [enabled]
 * the back event is taken by [onBack] and goes no further.
 */
@Composable
fun SheetBackHandler(enabled: Boolean, onBack: () -> Unit) {
    val state = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
    NavigationBackHandler(state = state, isBackEnabled = enabled, onBackCompleted = onBack)
}

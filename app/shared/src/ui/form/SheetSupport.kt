package com.usuallycoding.opensecretsanta.ui.form

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState

/** The state of a bottom sheet that opens at full height, with no half-open stop in between: hidden or expanded. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberAppSheetState(): SheetState = rememberBottomSheetState(
    initialValue = SheetValue.Hidden,
    enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
)

/**
 * Lets the system's back (the Android button or gesture) close an open sheet before it leaves the screen: while [enabled]
 * the back event is taken by [onBack] and goes no further.
 */
@Composable
fun SheetBackHandler(enabled: Boolean, onBack: () -> Unit) {
    val state = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
    NavigationBackHandler(state = state, isBackEnabled = enabled, onBackCompleted = onBack)
}

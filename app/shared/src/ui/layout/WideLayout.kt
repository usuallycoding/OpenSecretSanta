package com.usuallycoding.opensecretsanta.ui.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** The widest the content of a screen gets: the signed-in app centers it, and the bars and buttons share it on a narrower window. */
val STANDARD_CONTENT_WIDTH: Dp = 640.dp

/** The window width from which the signed-in app shows a sidebar instead of the bottom bar. */
val WIDE_BREAKPOINT: Dp = 840.dp

/** Whether a window [width] is wide: the sidebar, the top-bar buttons and dialogs instead of sheets. */
fun isWide(width: Dp): Boolean = width >= WIDE_BREAKPOINT

/** True below the signed-in app's shell while its window is wide; screens read it to leave out what the sidebar replaces. */
val LocalWideLayout = compositionLocalOf { false }

/** One button a page asks the top bar to show on a wide window, in the place of its floating button. */
class PageAction(val icon: ImageVector, val description: String, val onClick: () -> Unit)

/** The buttons the shown pages have asked for; the top bar draws them. */
@Stable
class PageActionHost {
    val actions = mutableStateListOf<PageAction>()
}

/** Set only while the window is wide: pages hand their actions to it instead of drawing a floating button. */
val LocalPageActions = compositionLocalOf<PageActionHost?> { null }

/** Puts an action in the top bar while the caller is shown, and takes it out when the caller leaves. */
@Composable
fun PageActionEffect(host: PageActionHost, icon: ImageVector, description: String, onClick: () -> Unit) {
    val latest by rememberUpdatedState(onClick)
    DisposableEffect(host, icon, description) {
        val action = PageAction(icon, description) { latest() }
        host.actions.add(action)
        onDispose { host.actions.remove(action) }
    }
}

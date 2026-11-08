package com.usuallycoding.opensecretsanta.ui.form

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.usuallycoding.opensecretsanta.ui.layout.LocalPageActions
import com.usuallycoding.opensecretsanta.ui.layout.PageActionEffect

/** The room a list leaves at its end so that its last row can be scrolled clear of a floating button. */
val FAB_CLEARANCE: Dp = 88.dp

/** The room a list leaves for a floating button: none on a wide window, where the action is in the top bar instead. */
@Composable
fun fabClearance(): Dp = if (LocalPageActions.current == null) FAB_CLEARANCE else 0.dp

/**
 * A floating button at the bottom end of the content it is placed in, with an icon and the action's name for assistive technology.
 * On a wide window it draws nothing and asks the top bar to show the action as a button instead.
 */
@Composable
fun BoxScope.AppFab(icon: ImageVector, description: String, onClick: () -> Unit) {
    val host = LocalPageActions.current
    if (host != null) {
        PageActionEffect(host, icon, description, onClick)
        return
    }
    FloatingActionButton(onClick = onClick, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) {
        Icon(icon, contentDescription = description)
    }
}

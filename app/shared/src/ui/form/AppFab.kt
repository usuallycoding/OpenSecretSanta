package com.usuallycoding.opensecretsanta.ui.form

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** The room a list leaves at its end so that its last row can be scrolled clear of a floating button. */
val FAB_CLEARANCE: Dp = 88.dp

/** A floating button at the bottom end of the content it is placed in, with an icon and the action's name for assistive technology. */
@Composable
fun BoxScope.AppFab(icon: ImageVector, description: String, onClick: () -> Unit) {
    FloatingActionButton(onClick = onClick, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) {
        Icon(icon, contentDescription = description)
    }
}

/** The Material "help" icon (a question mark in a circle), drawn here because the core icon set has none. */
internal val QuestionIcon: ImageVector by lazy {
    ImageVector.Builder("Question", 24.dp, 24.dp, 24f, 24f).addPath(
        PathParser().parsePathString(
            "M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10S17.52,2 12,2zM13,19h-2v-2h2v2zM15.07,11.25l-0.9,0.92C13.45,12.9 13,13.5 13,15h-2v-0.5c0,-1.1 0.45,-2.1 1.17,-2.83l1.24,-1.26c0.37,-0.36 0.59,-0.86 0.59,-1.41 0,-1.1 -0.9,-2 -2,-2s-2,0.9 -2,2L8,9c0,-2.21 1.79,-4 4,-4s4,1.79 4,4c0,0.88 -0.36,1.68 -0.93,2.25z",
        ).toNodes(),
        fill = SolidColor(Color.Black),
    ).build()
}

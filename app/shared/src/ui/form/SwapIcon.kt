package com.usuallycoding.opensecretsanta.ui.form

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/** The Material "swap horizontal" icon (two arrows the other way round), drawn here because the core icon set has none. */
internal val SwapIcon: ImageVector by lazy {
    ImageVector.Builder("Swap", 24.dp, 24.dp, 24f, 24f).addPath(
        PathParser().parsePathString("M6.99,11L3,15l3.99,4v-3H14v-2H6.99v-3zM21,9l-3.99,-4v3H10v2h7.01v3L21,9z").toNodes(),
        fill = SolidColor(Color.Black),
    ).build()
}

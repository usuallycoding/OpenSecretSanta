package com.usuallycoding.opensecretsanta.ui.form

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/** The Material "vpn key" icon, drawn here because the core icon set has none. */
internal val KeyIcon: ImageVector by lazy {
    ImageVector.Builder("Key", 24.dp, 24.dp, 24f, 24f).addPath(
        PathParser().parsePathString("M12.65,10C11.83,7.67 9.61,6 7,6c-3.31,0 -6,2.69 -6,6s2.69,6 6,6c2.61,0 4.83,-1.67 5.65,-4H17v4h4v-4h2v-4H12.65zM7,14c-1.1,0 -2,-0.9 -2,-2s0.9,-2 2,-2 2,0.9 2,2 -0.9,2 -2,2z").toNodes(),
        fill = SolidColor(Color.Black),
    ).build()
}

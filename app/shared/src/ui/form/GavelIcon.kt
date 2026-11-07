package com.usuallycoding.opensecretsanta.ui.form

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/** The Material "gavel" icon, drawn here because the core icon set has none; it takes the colour of the button it is in. */
internal val GavelIcon: ImageVector by lazy {
    ImageVector.Builder("Gavel", 24.dp, 24.dp, 24f, 24f).addPath(
        PathParser().parsePathString("M1,21h12v2H1zM5.245,8.07l2.83,-2.827 14.14,14.142 -2.828,2.828zM12.317,1l5.657,5.656 -2.83,2.83 -5.654,-5.66zM3.825,9.485l5.657,5.657 -2.828,2.828 -5.657,-5.657z").toNodes(),
        fill = SolidColor(Color.Black),
    ).build()
}

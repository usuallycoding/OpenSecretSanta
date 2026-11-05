package com.usuallycoding.opensecretsanta.ui.form

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/** The Material "calendar today" icon, drawn here because the core icon set has none. */
internal val CalendarIcon: ImageVector by lazy {
    ImageVector.Builder("Calendar", 24.dp, 24.dp, 24f, 24f).addPath(
        PathParser().parsePathString("M20,3h-1L19,1h-2v2L7,3L7,1L5,1v2L4,3c-1.1,0 -2,0.9 -2,2v16c0,1.1 0.9,2 2,2h16c1.1,0 2,-0.9 2,-2L22,5c0,-1.1 -0.9,-2 -2,-2zM20,21L4,21L4,8h16v13z").toNodes(),
        fill = SolidColor(Color.Black),
    ).build()
}

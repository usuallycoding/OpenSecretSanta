package com.usuallycoding.opensecretsanta.ui.form

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/** The Material "mail (an envelope)" icon, drawn here because the core icon set has none; it takes the colour of the button it is in. */
internal val MailIcon: ImageVector by lazy {
    ImageVector.Builder("Mail", 24.dp, 24.dp, 24f, 24f).addPath(
        PathParser().parsePathString("M20,4H4c-1.1,0 -1.99,0.9 -1.99,2L2,18c0,1.1 0.9,2 2,2h16c1.1,0 2,-0.9 2,-2V6c0,-1.1 -0.9,-2 -2,-2zM20,8l-8,5 -8,-5V6l8,5 8,-5v2z").toNodes(),
        fill = SolidColor(Color.Black),
    ).build()
}

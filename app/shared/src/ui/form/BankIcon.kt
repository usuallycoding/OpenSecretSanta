package com.usuallycoding.opensecretsanta.ui.form

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/** The Material "account balance (a bank)" icon, drawn here because the core icon set has none. */
internal val BankIcon: ImageVector by lazy {
    ImageVector.Builder("Bank", 24.dp, 24.dp, 24f, 24f).addPath(
        PathParser().parsePathString("M4,10v7h3v-7L4,10zM10,10v7h3v-7h-3zM2,22h19v-3L2,19v3zM16,10v7h3v-7h-3zM11.5,1L2,6v2h19L21,6l-9.5,-5z").toNodes(),
        fill = SolidColor(Color.Black),
    ).build()
}

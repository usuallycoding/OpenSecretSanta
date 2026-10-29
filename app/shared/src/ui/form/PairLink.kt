package com.usuallycoding.opensecretsanta.ui.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * What opens the page or sheet behind a giver and a receiver: an arrow, a gift and an arrow on one tonal pill, between the
 * two names of a row. It is one button, not three icons, so that it looks like something to press: the whole pill responds,
 * it shows when it is pressed, and the area that responds is at least 48dp wide and high around the smaller pill.
 *
 * A screen reader announces it once, as a button called [label] (what it opens); the icons say nothing themselves. The arrows
 * mirror in a right-to-left language, so they still lead from the giver, at the start of the row, to the receiver.
 */
@Composable
fun PairLink(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        // laid out first, so that the 48dp area is what a press is looked for in, and the pill is drawn in the middle of it
        modifier = modifier.minimumInteractiveComponentSize().semantics {
            role = Role.Button
            contentDescription = label
        },
        shape = CircleShape,
        color = colors.secondaryContainer,
        contentColor = colors.onSecondaryContainer,
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
            Icon(GiftIcon, contentDescription = null, modifier = Modifier.size(20.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }
}

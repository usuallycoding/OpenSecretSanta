package com.usuallycoding.opensecretsanta.ui.form

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** One chip of [ParticipantChips]: the [label] it shows and, for the person's own, a [spoken] form that says so without relying on colour. */
data class ParticipantChip(val label: String, val highlighted: Boolean = false, val spoken: String? = null)

/**
 * The people of a group as chips that wrap onto as many lines as needed. They are part of the page, which scrolls as a
 * whole. The chips only show; they are not buttons. The [legend] is shown under them when there is one.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColumnScope.ParticipantChips(chips: List<ParticipantChip>, legend: String? = null) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (chip in chips) Chip(chip)
    }
    if (legend != null) {
        Text(legend, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Chip(chip: ParticipantChip) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = if (chip.spoken != null) Modifier.semantics { contentDescription = chip.spoken } else Modifier,
        shape = MaterialTheme.shapes.small,
        color = if (chip.highlighted) colors.secondaryContainer else colors.surface,
        border = if (chip.highlighted) null else BorderStroke(1.dp, colors.outlineVariant),
    ) {
        Text(
            chip.label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelLarge,
            color = if (chip.highlighted) colors.onSecondaryContainer else colors.onSurface,
        )
    }
}

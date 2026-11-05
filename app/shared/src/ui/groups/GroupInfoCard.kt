package com.usuallycoding.opensecretsanta.ui.groups

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.usuallycoding.opensecretsanta.ui.Strings
import com.usuallycoding.opensecretsanta.ui.form.ReadOnlyText

/**
 * What a group says about itself, in one card: the description, then "When? - ..." and "Where? - ..." and "Budget? - ...". A part the group does
 * not have is left out, and with none of them nothing is drawn. The group's name, state and actions are not part of it.
 */
@Composable
internal fun GroupInfoCard(description: String?, whenText: String?, location: String?, budget: String?, modifier: Modifier = Modifier) {
    val about = description?.takeIf { it.isNotBlank() }
    val schedule = whenText?.takeIf { it.isNotBlank() }
    val where = location?.takeIf { it.isNotBlank() }
    val spend = budget?.takeIf { it.isNotBlank() }
    if (about == null && schedule == null && where == null && spend == null) return
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            about?.let { ReadOnlyText(it, style = MaterialTheme.typography.bodyLarge, label = Strings.DESCRIPTION_LABEL) }
            schedule?.let { Text(labelled(Strings.WHEN_LABEL, it), style = MaterialTheme.typography.bodyLarge) }
            where?.let { Text(labelled(Strings.WHERE_LABEL, it), style = MaterialTheme.typography.bodyLarge) }
            spend?.let { Text(labelled(Strings.BUDGET_LABEL, it), style = MaterialTheme.typography.bodyLarge) }
        }
    }
}

/** One line, so it wraps and is read as one, with only the label in a heavier weight. */
private fun labelled(label: String, value: String): AnnotatedString = buildAnnotatedString {
    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(label) }
    append(labelledLine(label, value).substring(label.length))
}

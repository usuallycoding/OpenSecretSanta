package com.usuallycoding.opensecretsanta.ui.groups

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.usuallycoding.opensecretsanta.ui.Strings
import com.usuallycoding.opensecretsanta.ui.form.BankIcon
import com.usuallycoding.opensecretsanta.ui.form.CalendarIcon
import com.usuallycoding.opensecretsanta.ui.form.PlaceIcon
import com.usuallycoding.opensecretsanta.ui.form.ReadOnlyText

/**
 * What a group says about itself: its description as plain text, then one card with a line for when, where and how much,
 * each starting with an icon in place of a word. A part the group does not have is left out, and a group with none of the
 * three facts has no card. The group's name, state and actions are not part of it.
 */
@Composable
internal fun GroupInfoCard(description: String?, whenText: String?, location: String?, budget: String?, modifier: Modifier = Modifier) {
    val about = description?.takeIf { it.isNotBlank() }
    val schedule = whenText?.takeIf { it.isNotBlank() }
    val where = location?.takeIf { it.isNotBlank() }
    val spend = budget?.takeIf { it.isNotBlank() }
    if (about == null && schedule == null && where == null && spend == null) return
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        about?.let { ReadOnlyText(it, style = MaterialTheme.typography.bodyLarge, label = Strings.DESCRIPTION_LABEL) }
        if (schedule != null || where != null || spend != null) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    schedule?.let { InfoLine(CalendarIcon, Strings.WHEN_LABEL, it) }
                    where?.let { InfoLine(PlaceIcon, Strings.WHERE_LABEL, it) }
                    spend?.let { InfoLine(BankIcon, Strings.BUDGET_LABEL, it) }
                }
            }
        }
    }
}

/**
 * An icon and its value, the value wrapping under itself and not under the icon. A screen reader reads the line as one,
 * `When? - 24 Dec 2026, 18:30`, with the words that the icon stands for; the icon announces nothing.
 */
@Composable
private fun InfoLine(icon: ImageVector, label: String, value: String) {
    Row(Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = labelledLine(label, value) }) {
        Icon(icon, contentDescription = null, modifier = Modifier.padding(top = 2.dp).size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

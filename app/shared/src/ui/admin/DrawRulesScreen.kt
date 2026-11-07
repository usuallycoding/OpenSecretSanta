package com.usuallycoding.opensecretsanta.ui.admin

import com.usuallycoding.opensecretsanta.ui.layout.ForwardOutsideScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.usuallycoding.opensecretsanta.domain.group.RuleKind
import com.usuallycoding.opensecretsanta.ui.Strings
import com.usuallycoding.opensecretsanta.ui.form.SnowCard
import com.usuallycoding.opensecretsanta.ui.groups.isGone
import com.usuallycoding.opensecretsanta.ui.layout.STANDARD_CONTENT_WIDTH
import com.usuallycoding.opensecretsanta.ui.load.loadMessage

/** The rules page of a group on a narrow window: a page of its own, where the pane is not there. */
@Composable
fun DrawRulesScreen(viewModel: DrawRulesViewModel) {
    val scroll = rememberScrollState()
    ForwardOutsideScroll(scroll)
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.widthIn(max = STANDARD_CONTENT_WIDTH).fillMaxSize().verticalScroll(scroll).padding(16.dp),
        ) {
            RulesContent(viewModel)
        }
    }
}

/** The rules themselves, for the page, the pane beside the group page and the sheet. */
@Composable
internal fun RulesContent(viewModel: DrawRulesViewModel) {
    val state by viewModel.data.collectAsState()
    val ui by viewModel.ui.collectAsState()
    LaunchedEffect(viewModel) { viewModel.refresh() }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(Strings.RULES_TITLE, style = MaterialTheme.typography.titleLarge)
        if (state.isGone()) {
            Text(Strings.RULES_GONE, style = MaterialTheme.typography.bodyMedium)
            return@Column
        }
        state.error?.let { error ->
            Text(error.loadMessage(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            OutlinedButton(onClick = { viewModel.refresh() }) { Text(Strings.TRY_AGAIN) }
        }
        val data = state.value
        when {
            data != null -> RulesBody(data, ui, viewModel)
            state.loading -> CircularProgressIndicator(Modifier.padding(8.dp))
        }
    }
}

@Composable
private fun RulesBody(data: DrawRulesData, ui: DrawRulesUi, viewModel: DrawRulesViewModel) {
    if (viewModel.ownerTakesPart) Note(Strings.RULES_OWNER_TAKES_PART)
    if (data.fewDraws) Note(Strings.RULES_FEW_DRAWS, warning = true)
    if (!data.canEdit) Note(Strings.RULES_FIXED)
    if (data.canEdit) AddForm(data.participants, ui, viewModel)
    if (data.rules.isEmpty()) {
        Text(Strings.RULES_NONE, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (rule in data.rules) {
            SnowCard("rule-${rule.id}", Modifier.fillMaxWidth()) {
                Row(Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(rule.sentence, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    if (data.canEdit) {
                        IconButton(onClick = { viewModel.delete(rule) }, enabled = !ui.busy) {
                            Icon(Icons.Filled.Delete, contentDescription = Strings.RULES_DELETE, tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Note(text: String, warning: Boolean = false) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = if (warning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun AddForm(participants: List<String>, ui: DrawRulesUi, viewModel: DrawRulesViewModel) {
    val form = ui.form
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(Strings.RULES_ADD_TITLE, style = MaterialTheme.typography.titleMedium)
        PersonPicker(Strings.RULES_GIVER, form.giver, participants, viewModel::setGiver)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = form.kind == RuleKind.MUST, onClick = { viewModel.setKind(RuleKind.MUST) }, label = { Text(Strings.RULES_MUST) })
            FilterChip(selected = form.kind == RuleKind.NEVER, onClick = { viewModel.setKind(RuleKind.NEVER) }, label = { Text(Strings.RULES_NEVER) })
        }
        PersonPicker(Strings.RULES_RECEIVER, form.receiver, participants, viewModel::setReceiver)
        Row(
            Modifier.fillMaxWidth().toggleable(form.bothWays, role = Role.Switch, onValueChange = viewModel::setBothWays).padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(Strings.RULES_BOTH_WAYS, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Switch(checked = form.bothWays, onCheckedChange = null)
        }
        ui.message?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
        Button(onClick = { viewModel.add() }, enabled = form.canAdd && !ui.busy) { Text(Strings.ADMIN_ADD) }
    }
}

/** A person of the group, chosen from a menu. */
@Composable
private fun PersonPicker(label: String, selected: String?, options: List<String>, onSelect: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = { open = true }, Modifier.fillMaxWidth()) {
            Text(selected ?: label, maxLines = 1, color = if (selected == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            for (name in options) DropdownMenuItem(text = { Text(name) }, onClick = { onSelect(name); open = false })
        }
    }
}

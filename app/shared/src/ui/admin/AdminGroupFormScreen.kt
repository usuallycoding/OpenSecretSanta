package com.usuallycoding.opensecretsanta.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.usuallycoding.opensecretsanta.ui.Strings
import com.usuallycoding.opensecretsanta.ui.groups.isGone
import com.usuallycoding.opensecretsanta.ui.load.LoadContent

/**
 * The form for a group's details, for a new group or an existing one. When the group is saved the
 * screen hands its id to [onSaved] and leaves it to the caller where to go next.
 */
@Composable
fun AdminGroupFormScreen(viewModel: AdminGroupFormViewModel, onSaved: (Long) -> Unit, onBack: () -> Unit) {
    val loaded by viewModel.loaded.collectAsState()
    val form by viewModel.form.collectAsState()
    val saved by viewModel.saved.collectAsState()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }
    LaunchedEffect(saved) { saved?.let(onSaved) }

    if (loaded.isGone()) {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(Strings.GROUP_GONE, style = MaterialTheme.typography.titleMedium)
                Button(onClick = onBack) { Text(Strings.BACK) }
            }
        }
        return
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 640.dp).fillMaxSize()) {
            LoadContent(loaded, onRetry = { viewModel.refresh() }) {
                Column(
                    Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Field(Strings.GROUP_NAME, form.name, form.errors[GroupField.Name], hint = null, singleLine = true) { viewModel.edit(GroupField.Name, it) }
                    Field(Strings.GROUP_DESCRIPTION, form.description, form.errors[GroupField.Description], hint = null, singleLine = false) { viewModel.edit(GroupField.Description, it) }
                    Field(Strings.GROUP_LOCATION, form.location, form.errors[GroupField.Location], hint = null, singleLine = true) { viewModel.edit(GroupField.Location, it) }
                    Field(Strings.GROUP_DATE, form.date, form.errors[GroupField.Date], hint = Strings.GROUP_DATE_HINT, singleLine = true, numeric = true) { viewModel.edit(GroupField.Date, it) }
                    Field(Strings.GROUP_TIME, form.time, form.errors[GroupField.Time], hint = Strings.GROUP_TIME_HINT, singleLine = true, numeric = true) { viewModel.edit(GroupField.Time, it) }
                    form.general?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
                    Button(onClick = { viewModel.save() }, enabled = !form.saving) { Text(if (form.saving) Strings.SAVING else Strings.SAVE) }
                }
            }
        }
    }
}

@Composable
private fun Field(label: String, value: String, error: String?, hint: String?, singleLine: Boolean, numeric: Boolean = false, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 3,
        maxLines = if (singleLine) 1 else 6,
        isError = error != null,
        supportingText = (error ?: hint)?.let { { Text(it) } },
        keyboardOptions = if (numeric) KeyboardOptions(keyboardType = KeyboardType.Ascii) else KeyboardOptions.Default,
        modifier = Modifier.fillMaxWidth(),
    )
}

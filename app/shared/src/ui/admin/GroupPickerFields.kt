package com.usuallycoding.opensecretsanta.ui.admin

import com.usuallycoding.opensecretsanta.ui.form.appFieldColors
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import com.usuallycoding.opensecretsanta.ui.Strings
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/**
 * The group's date: a read-only field that opens a date picker, with a control that empties it. It holds the same
 * `2026-12-24` text the form always has; an empty field opens the picker on the default date but stays empty until OK.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DatePickerField(label: String, value: String, error: String?, onChange: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    PickerField(label, value, error, Strings.PICK_DATE, onClear = { onChange("") }, onOpen = { open = true })
    if (!open) return
    val initial = parseGroupDate(value) ?: defaultGroupDate(Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date)
    val state = rememberDatePickerState(initialSelectedDateMillis = initial.toPickerMillis())
    DatePickerDialog(
        onDismissRequest = { open = false },
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { onChange(dateFromPickerMillis(it).toString()) }
                open = false
            }) { Text(Strings.OK) }
        },
        dismissButton = { TextButton(onClick = { open = false }) { Text(Strings.CANCEL) } },
    ) {
        DatePicker(state, showModeToggle = false)
    }
}

/** The group's time: a read-only field that opens a 24-hour time picker, with a control that empties it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TimePickerField(label: String, value: String, error: String?, onChange: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    PickerField(label, value, error, Strings.PICK_TIME, onClear = { onChange("") }, onOpen = { open = true })
    if (!open) return
    val initial = parseGroupTime(value) ?: DEFAULT_GROUP_TIME
    val state = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute, is24Hour = true)
    AlertDialog(
        onDismissRequest = { open = false },
        confirmButton = {
            TextButton(onClick = {
                onChange(kotlinx.datetime.LocalTime(state.hour, state.minute).toString())
                open = false
            }) { Text(Strings.OK) }
        },
        dismissButton = { TextButton(onClick = { open = false }) { Text(Strings.CANCEL) } },
        text = { TimePicker(state) },
    )
}

/** A field nothing can be typed into: choosing it calls [onOpen], and while it has a value a clear control calls [onClear]. */
@Composable
private fun PickerField(label: String, value: String, error: String?, openLabel: String, onClear: () -> Unit, onOpen: () -> Unit) {
    val interactions = remember { MutableInteractionSource() }
    LaunchedEffect(interactions) {
        interactions.interactions.collect { if (it is PressInteraction.Release) onOpen() }
    }
    OutlinedTextField(
        value = value,
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        singleLine = true,
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        trailingIcon = if (value.isEmpty()) null else {
            { IconButton(onClick = onClear) { Icon(Icons.Filled.Close, contentDescription = Strings.CLEAR_FIELD) } }
        },
        interactionSource = interactions,
        colors = appFieldColors(),
        modifier = Modifier.fillMaxWidth().semantics {
            role = Role.Button
            onClick(label = openLabel) { onOpen(); true }
        },
    )
}

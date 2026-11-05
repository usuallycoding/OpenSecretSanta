package com.usuallycoding.opensecretsanta.ui.delivery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.usuallycoding.opensecretsanta.ui.Strings
import com.usuallycoding.opensecretsanta.ui.form.touchesOfAField
import com.usuallycoding.opensecretsanta.ui.form.FormColumn
import com.usuallycoding.opensecretsanta.ui.form.MultiLineTextField
import com.usuallycoding.opensecretsanta.ui.load.LoadContent

/** The page for one group's delivery info: the person's details, which a Santa uses to post their gift. */
@Composable
fun DeliveryInfoScreen(viewModel: DeliveryInfoViewModel) {
    val load by viewModel.load.collectAsState()
    val ui by viewModel.ui.collectAsState()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LoadContent(load, onRetry = { viewModel.refresh() }, modifier = Modifier.fillMaxSize()) {
            DeliveryForm(ui, viewModel)
        }
    }
}

@Composable
private fun DeliveryForm(ui: DeliveryUiState, viewModel: DeliveryInfoViewModel) {
    FormColumn(
        Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(Strings.DELIVERY_WHY, style = MaterialTheme.typography.bodyMedium)
        if (!ui.form.hasNameAndAddress) {
            Text(Strings.DELIVERY_INCOMPLETE, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        }
        Field(ui, viewModel, DeliveryField.FullName, ui.form.fullName, KeyboardType.Text, ImeAction.Next)
        Field(ui, viewModel, DeliveryField.Email, ui.form.email, KeyboardType.Email, ImeAction.Next)
        Field(ui, viewModel, DeliveryField.Phone, ui.form.phone, KeyboardType.Phone, ImeAction.Next)
        MultiLineField(ui, viewModel, DeliveryField.Address, ui.form.address)
        MultiLineField(ui, viewModel, DeliveryField.DeliveryInfo, ui.form.deliveryInfo)
        ui.errors.general?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(onClick = { viewModel.save() }, enabled = ui.canSave, modifier = Modifier.fillMaxWidth()) {
            Text(if (ui.saving) Strings.SAVING else Strings.SAVE)
        }
        if (ui.justSaved) Text(Strings.SAVED, color = MaterialTheme.colorScheme.primary)
    }
}

/** The address and the delivery information take several lines: Return starts a new line, six rows at most. */
@Composable
private fun MultiLineField(ui: DeliveryUiState, viewModel: DeliveryInfoViewModel, field: DeliveryField, value: String) {
    MultiLineTextField(
        value = value,
        onValueChange = { viewModel.edit(field, it) },
        label = field.label,
        minLines = 2,
        error = ui.errors.fields[field],
    )
}

@Composable
private fun Field(
    ui: DeliveryUiState,
    viewModel: DeliveryInfoViewModel,
    field: DeliveryField,
    value: String,
    keyboard: KeyboardType,
    ime: ImeAction,
) {
    val error = ui.errors.fields[field]
    OutlinedTextField(
        value = value,
        onValueChange = { viewModel.edit(field, it) },
        label = { Text(field.label) },
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().touchesOfAField(),
        keyboardOptions = KeyboardOptions(keyboardType = keyboard, imeAction = ime),
    )
}

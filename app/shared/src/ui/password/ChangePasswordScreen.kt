package com.usuallycoding.opensecretsanta.ui.password

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.usuallycoding.opensecretsanta.ui.Strings

@Composable
fun ChangePasswordScreen(viewModel: ChangePasswordViewModel, onSignOut: () -> Unit) {
    val state by viewModel.state.collectAsState()
    Column(
        modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(Strings.CHANGE_PASSWORD_TITLE, style = MaterialTheme.typography.headlineMedium)
        Text(Strings.CHANGE_PASSWORD_INTRO)
        PasswordField(Strings.CURRENT_PASSWORD, state.current, viewModel::onCurrentChange, state.errors.current, ImeAction.Next)
        PasswordField(Strings.NEW_PASSWORD, state.new, viewModel::onNewChange, state.errors.new, ImeAction.Next)
        PasswordField(Strings.REPEAT_PASSWORD, state.repeat, viewModel::onRepeatChange, state.errors.repeat, ImeAction.Done, onDone = viewModel::submit)
        state.errors.general?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(onClick = viewModel::submit, enabled = state.canSubmit, modifier = Modifier.fillMaxWidth()) {
            Text(if (state.inProgress) Strings.CHANGING_PASSWORD else Strings.CHANGE_PASSWORD)
        }
        OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) { Text(Strings.SIGN_OUT) }
    }
}

@Composable
private fun PasswordField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    error: String?,
    ime: ImeAction,
    onDone: () -> Unit = {},
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ime),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
    )
}

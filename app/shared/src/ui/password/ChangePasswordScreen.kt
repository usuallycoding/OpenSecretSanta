package com.usuallycoding.opensecretsanta.ui.password

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.platform.LocalAutofillManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.usuallycoding.opensecretsanta.ui.Strings
import com.usuallycoding.opensecretsanta.ui.form.PasswordField
import com.usuallycoding.opensecretsanta.ui.form.PlatformChangePasswordForm

@Composable
fun ChangePasswordScreen(viewModel: ChangePasswordViewModel, onSignOut: () -> Unit, username: String = "") {
    PlatformChangePasswordForm(username = username, viewModel = viewModel, onSignOut = onSignOut)
}

@Composable
internal fun ComposeChangePasswordForm(viewModel: ChangePasswordViewModel, onSignOut: () -> Unit) {
    val state by viewModel.state.collectAsState()
    val autofillManager = LocalAutofillManager.current
    LaunchedEffect(state.done) { if (state.done) autofillManager?.commit() }
    Column(
        modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(Strings.CHANGE_PASSWORD_TITLE, style = MaterialTheme.typography.headlineMedium)
        Text(Strings.CHANGE_PASSWORD_INTRO)
        PasswordField(Strings.CURRENT_PASSWORD, state.current, viewModel::onCurrentChange, state.errors.current, ImeAction.Next, autofillType = ContentType.Password)
        PasswordField(Strings.NEW_PASSWORD, state.new, viewModel::onNewChange, state.errors.new, ImeAction.Next, autofillType = ContentType.NewPassword)
        PasswordField(Strings.REPEAT_PASSWORD, state.repeat, viewModel::onRepeatChange, state.errors.repeat, ImeAction.Done, onDone = viewModel::submit, autofillType = ContentType.NewPassword)
        state.errors.general?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(onClick = viewModel::submit, enabled = state.canSubmit, modifier = Modifier.fillMaxWidth()) {
            Text(if (state.inProgress) Strings.CHANGING_PASSWORD else Strings.CHANGE_PASSWORD)
        }
        OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) { Text(Strings.SIGN_OUT) }
    }
}

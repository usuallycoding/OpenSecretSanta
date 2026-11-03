package com.usuallycoding.opensecretsanta.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.unit.dp
import com.usuallycoding.opensecretsanta.APP_VERSION
import com.usuallycoding.opensecretsanta.getPlatform
import androidx.compose.ui.semantics.Role
import androidx.compose.material3.Checkbox
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Row
import com.usuallycoding.opensecretsanta.ui.Strings
import com.usuallycoding.opensecretsanta.ui.form.touchesOfAField
import com.usuallycoding.opensecretsanta.ui.form.PasswordField

@Composable
fun LoginScreen(viewModel: LoginViewModel, sessionEnded: Boolean) {
    val state by viewModel.state.collectAsState()
    Column(
        modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(Strings.APP_NAME, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        if (sessionEnded) Text(Strings.SESSION_ENDED, color = MaterialTheme.colorScheme.primary)
        OutlinedTextField(
            value = state.username,
            onValueChange = viewModel::onUsernameChange,
            label = { Text(Strings.USERNAME) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().touchesOfAField(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        )
        PasswordField(Strings.PASSWORD, state.password, viewModel::onPasswordChange, null, ImeAction.Done, onDone = { viewModel.submit() })
        if (getPlatform().canChooseToStaySignedIn) {
            Row(
                Modifier.fillMaxWidth().toggleable(state.staySignedIn, role = Role.Checkbox, onValueChange = viewModel::onStaySignedInChange),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(state.staySignedIn, onCheckedChange = null)
                Spacer(Modifier.width(8.dp))
                Text(Strings.STAY_SIGNED_IN)
            }
        }
        state.problem?.let { Text(it.text(), color = MaterialTheme.colorScheme.error) }
        Button(onClick = viewModel::submit, enabled = state.canSubmit, modifier = Modifier.fillMaxWidth()) {
            Text(if (state.inProgress) Strings.SIGNING_IN else Strings.SIGN_IN)
        }
        Text(Strings.version(APP_VERSION), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun LoginProblem.text(): String = when (this) {
    LoginProblem.WrongCredentials -> Strings.WRONG_CREDENTIALS
    is LoginProblem.Paused -> Strings.signInPaused(minutes)
    LoginProblem.Unreachable -> Strings.SERVER_UNREACHABLE
    LoginProblem.ServerFailed -> Strings.SERVER_FAILED
}

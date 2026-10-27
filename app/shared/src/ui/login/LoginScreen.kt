package com.usuallycoding.opensecretsanta.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.usuallycoding.opensecretsanta.ui.Strings

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
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        )
        OutlinedTextField(
            value = state.password,
            onValueChange = viewModel::onPasswordChange,
            label = { Text(Strings.PASSWORD) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { viewModel.submit() }),
        )
        state.problem?.let { Text(it.text(), color = MaterialTheme.colorScheme.error) }
        Button(onClick = viewModel::submit, enabled = state.canSubmit, modifier = Modifier.fillMaxWidth()) {
            Text(if (state.inProgress) Strings.SIGNING_IN else Strings.SIGN_IN)
        }
    }
}

private fun LoginProblem.text(): String = when (this) {
    LoginProblem.WrongCredentials -> Strings.WRONG_CREDENTIALS
    is LoginProblem.Paused -> Strings.signInPaused(minutes)
    LoginProblem.Unreachable -> Strings.SERVER_UNREACHABLE
    LoginProblem.ServerFailed -> Strings.SERVER_FAILED
}

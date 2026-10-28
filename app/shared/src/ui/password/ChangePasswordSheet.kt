package com.usuallycoding.opensecretsanta.ui.password

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import com.usuallycoding.opensecretsanta.ui.Strings
import com.usuallycoding.opensecretsanta.ui.form.SheetFrame

/**
 * The sheet that lets a signed-in person choose a new password, over the screen they were on. Leaving it, whichever way,
 * empties the view model so that nothing that was typed stays in memory.
 */
@Composable
fun ChangePasswordSheet(viewModel: ChangePasswordViewModel, onDismiss: () -> Unit) {
    val state by viewModel.state.collectAsState()
    DisposableEffect(viewModel) { onDispose { viewModel.reset() } }

    SheetFrame(onDismiss) {
        Text(Strings.CHANGE_PASSWORD, style = MaterialTheme.typography.titleLarge)
        if (state.done) {
            Text(Strings.PASSWORD_CHANGED, style = MaterialTheme.typography.bodyLarge)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Button(onClick = onDismiss) { Text(Strings.CLOSE) }
            }
        } else {
            Text(Strings.CHANGE_PASSWORD_SHEET_INTRO, style = MaterialTheme.typography.bodyMedium)
            PasswordField(Strings.CURRENT_PASSWORD, state.current, viewModel::onCurrentChange, state.errors.current, ImeAction.Next)
            PasswordField(Strings.NEW_PASSWORD, state.new, viewModel::onNewChange, state.errors.new, ImeAction.Next)
            PasswordField(Strings.REPEAT_PASSWORD, state.repeat, viewModel::onRepeatChange, state.errors.repeat, ImeAction.Done, onDone = { viewModel.submit() })
            state.errors.general?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text(Strings.CANCEL) }
                Button(onClick = { viewModel.submit() }, enabled = state.canSubmit) {
                    Text(if (state.inProgress) Strings.CHANGING_PASSWORD else Strings.SAVE)
                }
            }
        }
    }
}

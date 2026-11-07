package com.usuallycoding.opensecretsanta.ui.login

import com.usuallycoding.opensecretsanta.ui.form.appFieldColors
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.usuallycoding.opensecretsanta.APP_VERSION
import com.usuallycoding.opensecretsanta.getPlatform
import androidx.compose.ui.semantics.Role
import androidx.compose.material3.Checkbox
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Row
import com.usuallycoding.opensecretsanta.resources.Res
import com.usuallycoding.opensecretsanta.resources.app_logo_dark
import com.usuallycoding.opensecretsanta.resources.app_logo_light
import com.usuallycoding.opensecretsanta.ui.Strings
import org.jetbrains.compose.resources.painterResource
import com.usuallycoding.opensecretsanta.ui.form.touchesOfAField
import com.usuallycoding.opensecretsanta.ui.form.PasswordField

/** How wide the logo is at most; the picture is three times as wide, so it stays sharp. */
private val LOGO_MAX_WIDTH = 280.dp

/**
 * Whether the dark version of the logo is the one to show on [background]. The app picks its own theme (its Light, Dark or System
 * setting against the device's, and dynamic colours that replace the palette), so the colour being drawn on decides, not a setting.
 */
fun logoIsDark(background: Color): Boolean = background.luminance() < 0.5f

@Composable
fun LoginScreen(viewModel: LoginViewModel, sessionEnded: Boolean) {
    val state by viewModel.state.collectAsState()
    Column(
        modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // the logo has the name in it; it comes in a light and a dark version, whichever the theme being drawn needs
        val dark = logoIsDark(MaterialTheme.colorScheme.background)
        Image(
            painterResource(if (dark) Res.drawable.app_logo_dark else Res.drawable.app_logo_light),
            contentDescription = Strings.APP_NAME,
            modifier = Modifier.widthIn(max = LOGO_MAX_WIDTH).fillMaxWidth(),
            contentScale = ContentScale.Fit,
        )
        Spacer(Modifier.height(8.dp))
        if (sessionEnded) Text(Strings.SESSION_ENDED, color = MaterialTheme.colorScheme.primary)
        OutlinedTextField(
            value = state.username,
            onValueChange = viewModel::onUsernameChange,
            label = { Text(Strings.USERNAME) },
            singleLine = true,
            colors = appFieldColors(),
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

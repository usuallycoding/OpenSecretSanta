package com.usuallycoding.opensecretsanta.ui.form

import androidx.compose.runtime.Composable
import com.usuallycoding.opensecretsanta.ui.login.ComposeLoginForm
import com.usuallycoding.opensecretsanta.ui.login.LoginViewModel
import com.usuallycoding.opensecretsanta.ui.password.ChangePasswordViewModel
import com.usuallycoding.opensecretsanta.ui.password.ComposeChangePasswordForm
import com.usuallycoding.opensecretsanta.ui.password.ComposeChangePasswordSheet

@Composable
internal actual fun PlatformLoginForm(viewModel: LoginViewModel, sessionEnded: Boolean) =
    ComposeLoginForm(viewModel = viewModel, sessionEnded = sessionEnded)

@Composable
internal actual fun PlatformChangePasswordForm(username: String, viewModel: ChangePasswordViewModel, onSignOut: () -> Unit) =
    ComposeChangePasswordForm(viewModel = viewModel, onSignOut = onSignOut)

@Composable
internal actual fun PlatformChangePasswordSheet(username: String, viewModel: ChangePasswordViewModel, onDismiss: () -> Unit) =
    ComposeChangePasswordSheet(viewModel = viewModel, onDismiss = onDismiss)

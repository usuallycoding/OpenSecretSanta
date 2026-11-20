package com.usuallycoding.opensecretsanta.ui.form

import androidx.compose.runtime.Composable
import com.usuallycoding.opensecretsanta.ui.login.LoginViewModel
import com.usuallycoding.opensecretsanta.ui.password.ChangePasswordViewModel

/** Renders the sign-in form: native Compose on Android/iOS, semantic HTML `<form>` overlay on Web. */
@Composable
internal expect fun PlatformLoginForm(viewModel: LoginViewModel, sessionEnded: Boolean)

/** Renders the mandatory password-change form: native Compose on Android/iOS, semantic HTML `<form>` overlay on Web. */
@Composable
internal expect fun PlatformChangePasswordForm(username: String, viewModel: ChangePasswordViewModel, onSignOut: () -> Unit)

/** Renders the Settings password-change sheet: native Compose on Android/iOS, semantic HTML `<form>` modal on Web. */
@Composable
internal expect fun PlatformChangePasswordSheet(username: String, viewModel: ChangePasswordViewModel, onDismiss: () -> Unit)

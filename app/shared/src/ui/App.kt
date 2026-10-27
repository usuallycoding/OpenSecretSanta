package com.usuallycoding.opensecretsanta.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.usuallycoding.opensecretsanta.data.session.SessionManager
import com.usuallycoding.opensecretsanta.data.session.SessionState
import com.usuallycoding.opensecretsanta.data.session.SignedOutReason
import com.usuallycoding.opensecretsanta.ui.login.LoginScreen
import com.usuallycoding.opensecretsanta.ui.password.ChangePasswordScreen
import com.usuallycoding.opensecretsanta.ui.shell.ShellScreen
import com.usuallycoding.opensecretsanta.ui.shell.ShellViewModel
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * The whole app: which screen shows follows the session, so a rejected token or a password gate moves
 * everything at once. A platform host may pass a [gate] that must be satisfied before the app does
 * anything (Android uses it to get the local-network permission); the session is only restored,
 * and so the server only contacted, once the gate shows its content.
 */
@Composable
fun App(gate: @Composable (content: @Composable () -> Unit) -> Unit = { it() }) {
    AppTheme {
        gate { AppContent() }
    }
}

@Composable
private fun AppContent() {
    val session = koinInject<SessionManager>()
    val state by session.state.collectAsState()
    LaunchedEffect(session) { session.restore() }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(
            Modifier.fillMaxSize().safeContentPadding().verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.Center,
        ) {
            when (val current = state) {
                SessionState.Loading -> CircularProgressIndicator()
                is SessionState.SignedOut -> LoginScreen(koinViewModel(), sessionEnded = current.reason == SignedOutReason.SessionEnded)
                is SessionState.PasswordChangeRequired -> ChangePasswordScreen(koinViewModel(), onSignOut = session::signOut)
                is SessionState.SignedIn -> ShellScreen(
                    koinViewModel<ShellViewModel>(key = current.account.username) { parametersOf(current.account) },
                )
            }
        }
    }
}

/** Material colours that follow the system's light or dark setting. */
@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme(), content = content)
}

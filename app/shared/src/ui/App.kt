package com.usuallycoding.opensecretsanta.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import com.usuallycoding.opensecretsanta.ui.update.AppCompatibility
import com.usuallycoding.opensecretsanta.ui.update.AppScreen
import com.usuallycoding.opensecretsanta.ui.update.UpdateRequiredScreen
import com.usuallycoding.opensecretsanta.ui.update.screenFor
import androidx.compose.runtime.key
import androidx.navigation.NavController
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.usuallycoding.opensecretsanta.data.session.SessionManager
import com.usuallycoding.opensecretsanta.data.session.SessionState
import com.usuallycoding.opensecretsanta.data.session.SignedOutReason
import com.usuallycoding.opensecretsanta.ui.form.KeyboardDismissRoot
import com.usuallycoding.opensecretsanta.ui.form.dismissKeyboardOnDrag
import com.usuallycoding.opensecretsanta.ui.login.LoginScreen
import com.usuallycoding.opensecretsanta.ui.password.ChangePasswordScreen
import com.usuallycoding.opensecretsanta.ui.shell.ShellScreen
import com.usuallycoding.opensecretsanta.ui.shell.ShellViewModel
import com.usuallycoding.opensecretsanta.ui.snow.SnowOverlay
import com.usuallycoding.opensecretsanta.data.settings.AppearanceSettings
import com.usuallycoding.opensecretsanta.ui.theme.AppTheme
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
fun App(
    gate: @Composable (content: @Composable () -> Unit) -> Unit = { it() },
    onNavigationReady: suspend (NavController, isAdmin: Boolean) -> Unit = { _, _ -> },
) {
    val settings = koinInject<AppearanceSettings>()
    val appearance by settings.appearance.collectAsState()
    AppTheme(appearance) {
        Box(Modifier.fillMaxSize()) {
            gate { AppContent(onNavigationReady, snow = appearance.snow) }
        }
    }
}

@Composable
private fun AppContent(onNavigationReady: suspend (NavController, isAdmin: Boolean) -> Unit, snow: Boolean) {
    val session = koinInject<SessionManager>()
    val state by session.state.collectAsState()
    val compatibility = koinInject<AppCompatibility>()
    val compatible by compatibility.state.collectAsState()
    val scope = rememberCoroutineScope()
    // the server is asked whether this app can be used with it at the same time as the session is restored, so that a server that
    // cannot be reached is waited for once, not twice; nothing else shows until both have answered
    LaunchedEffect(session) { coroutineScope { launch { compatibility.check() }; session.restore() } }
    // and again on returning to the app: the server may have moved on meanwhile, or been rolled back; the session was restored at
    // launch whatever the server said, so only the check is repeated
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { scope.launch { compatibility.check() } }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        // the snow is the first layer over the background, under every screen (the sign-in ones too): it shows in the open space and
        // passes behind whatever has a fill, and it lets every touch through
        if (snow) SnowOverlay()
        // a swipe or a tap outside a text field closes the keyboard, on the shell and on the sign-in screens alike
        KeyboardDismissRoot {
            when (val screen = screenFor(compatible)) {
                AppScreen.Loading -> CenteredOnItsOwn { CircularProgressIndicator() }
                // in place of everything else: the shell, sign-in and the password change are all out of reach, and the session is kept
                is AppScreen.Update -> CenteredOnItsOwn { UpdateRequiredScreen(screen.update) }
                AppScreen.Session -> SessionScreens(state, session, onNavigationReady)
            }
        }
    }
}

/** A screen of one thing in the middle, over the whole window. */
@Composable
private fun CenteredOnItsOwn(content: @Composable () -> Unit) {
    val scrollState = rememberScrollState()
    Box(
        Modifier.fillMaxSize().dismissKeyboardOnDrag(scrollState).safeContentPadding().verticalScroll(scrollState),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** The screens that follow the session, as they always did. */
@Composable
private fun SessionScreens(
    state: SessionState,
    session: SessionManager,
    onNavigationReady: suspend (NavController, isAdmin: Boolean) -> Unit,
) {
    when (val current = state) {
        // Signed in, the shell is its own screen with a bar at the top; a new person gets a fresh one.
        // a change of role (an administrator handing theirs over) gives a fresh shell too, with the other view's screens
        is SessionState.SignedIn -> key(current.account.username, current.account.isAdmin) {
            ShellScreen(koinViewModel<ShellViewModel>(key = "${current.account.username}-${current.account.isAdmin}") { parametersOf(current.account) }, onNavigationReady)
        }
        else -> CenteredOnItsOwn {
            when (current) {
                SessionState.Loading -> CircularProgressIndicator()
                is SessionState.SignedOut -> LoginScreen(koinViewModel(), sessionEnded = current.reason == SignedOutReason.SessionEnded)
                is SessionState.PasswordChangeRequired -> ChangePasswordScreen(koinViewModel(), onSignOut = session::signOut)
                is SessionState.SignedIn -> Unit
            }
        }
    }
}

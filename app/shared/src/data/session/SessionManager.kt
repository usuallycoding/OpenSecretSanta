package com.usuallycoding.opensecretsanta.data.session

import com.usuallycoding.opensecretsanta.contract.AccountDto
import com.usuallycoding.opensecretsanta.data.api.ApiClient
import com.usuallycoding.opensecretsanta.data.api.ApiError
import com.usuallycoding.opensecretsanta.data.api.ApiResult
import com.usuallycoding.opensecretsanta.data.api.AuthContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** Something that must happen while the person is still signed in, just before they sign out. It may take a moment but never fails. */
fun interface SignOutHook {
    suspend fun beforeSignOut()
}

/**
 * The one place that knows who is signed in. It keeps the token in memory (so a broken store never
 * ends a session) and in the [TokenStore] (so a restart can pick it up again), and it moves the
 * whole app between signed out, password change and signed in. A rejected token or a password
 * gate reported by any request lands here through [rejected], so no screen has to handle them.
 */
class SessionManager(
    private val api: Lazy<ApiClient>,
    private val store: TokenStore,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    private val beforeSignOut: SignOutHook? = null,
    private val signOutWaitMillis: Long = SIGN_OUT_WAIT_MILLIS,
) : AuthContext {
    private val _state = MutableStateFlow<SessionState>(SessionState.Loading)
    val state: StateFlow<SessionState> = _state.asStateFlow()

    private var accessToken: String? = null

    override val token: String? get() = accessToken

    /** Checks a stored token with the server. Call once at start; a missing or rejected token means signed out. */
    suspend fun restore() {
        if (_state.value != SessionState.Loading) return // already done, e.g. the window was recreated
        val stored = safely { store.read() }
        if (stored.isNullOrBlank()) {
            _state.value = SessionState.SignedOut()
            return
        }
        accessToken = stored
        when (val result = api.value.account()) {
            is ApiResult.Success -> _state.value = result.value.toState()
            is ApiResult.Failure -> when (result.error) {
                // `rejected` has already moved the state for a rejected token or a password gate.
                ApiError.Unauthorized, ApiError.PasswordChangeRequired -> Unit
                // The server could not be asked: keep the token for next time, but nobody is signed in yet.
                else -> {
                    accessToken = null
                    _state.value = SessionState.SignedOut()
                }
            }
        }
    }

    /** Signs in; on success the state has moved on by the time this returns. */
    suspend fun signIn(username: String, password: String, remember: Boolean = true): ApiResult<Unit> {
        when (val login = api.value.login(username, password)) {
            is ApiResult.Failure -> return login
            is ApiResult.Success -> {
                accessToken = login.value.token
                safely { store.write(login.value.token, remember) }
            }
        }
        return when (val account = api.value.account()) {
            is ApiResult.Success -> {
                _state.value = account.value.toState()
                ApiResult.Success(Unit)
            }
            is ApiResult.Failure -> {
                if (account.error != ApiError.Unauthorized && account.error != ApiError.PasswordChangeRequired) dropSession(reason = null)
                account
            }
        }
    }

    /** Replaces the temporary password; on success the new token replaces the old one and the person is signed in. */
    suspend fun changePassword(currentPassword: String, newPassword: String): ApiResult<Unit> {
        val change = api.value.changePassword(currentPassword, newPassword)
        if (change is ApiResult.Failure) return change
        val token = (change as ApiResult.Success).value.token
        accessToken = token
        safely { store.write(token) }
        return when (val account = api.value.account()) {
            is ApiResult.Success -> {
                _state.value = account.value.toState()
                ApiResult.Success(Unit)
            }
            is ApiResult.Failure -> account
        }
    }

    /** Signs out; [beforeSignOut] gets a short while first (while the token still works), then the session ends whatever came of it. */
    fun signOut() {
        val hook = beforeSignOut
        if (hook == null) {
            dropSession(reason = null)
            return
        }
        scope.launch {
            withTimeoutOrNull(signOutWaitMillis) { safely { hook.beforeSignOut() } }
            dropSession(reason = null)
        }
    }

    override fun rejected(error: ApiError) {
        when (error) {
            ApiError.Unauthorized -> dropSession(reason = SignedOutReason.SessionEnded)
            ApiError.PasswordChangeRequired -> {
                val account = when (val current = _state.value) {
                    is SessionState.SignedIn -> current.account
                    is SessionState.PasswordChangeRequired -> current.account
                    else -> null
                }
                if (account != null) _state.value = SessionState.PasswordChangeRequired(account)
            }
            else -> Unit
        }
    }

    /** Forgets the token and shows the sign-in screen at once; the stored copy is deleted right behind it. */
    private fun dropSession(reason: SignedOutReason?) {
        accessToken = null
        _state.value = SessionState.SignedOut(reason)
        scope.launch { safely { store.clear() } }
    }

    private fun AccountDto.toState(): SessionState {
        val account = Account(username, fullName, isAdmin)
        return if (mustChangePassword) SessionState.PasswordChangeRequired(account) else SessionState.SignedIn(account)
    }

    private suspend fun <T> safely(block: suspend () -> T): T? =
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) { // a failing store must never end a session, whatever it throws
            null
        }

    private companion object {
        /** The most a hook may hold the sign-out up, however slow the network. */
        const val SIGN_OUT_WAIT_MILLIS = 4_000L
    }
}

package com.usuallycoding.opensecretsanta.data.session

/** Who is signed in. Admin status and the password rule come from the server, never from the token. */
data class Account(
    val username: String,
    val fullName: String?,
    val isAdmin: Boolean,
)

/** Why the person is looking at the sign-in screen without having asked to be there. */
enum class SignedOutReason { SessionEnded }

sealed interface SessionState {
    /** Start-up: a stored token is being checked with the server. */
    data object Loading : SessionState

    data class SignedOut(val reason: SignedOutReason? = null) : SessionState

    /** The password is temporary: only the password-change screen may be shown. */
    data class PasswordChangeRequired(val account: Account) : SessionState

    data class SignedIn(val account: Account) : SessionState
}

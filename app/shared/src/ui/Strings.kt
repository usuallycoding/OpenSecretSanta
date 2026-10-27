package com.usuallycoding.opensecretsanta.ui

/** Every sentence the app shows on its own, in one place so a later translation has one place to start. */
object Strings {
    const val APP_NAME = "Open Secret Santa"

    const val USERNAME = "Username"
    const val PASSWORD = "Password"
    const val SIGN_IN = "Sign in"
    const val SIGN_OUT = "Sign out"
    const val SIGNING_IN = "Signing in..."

    const val SESSION_ENDED = "You have been signed out. Please sign in again."
    const val WRONG_CREDENTIALS = "Wrong username or password."
    const val SERVER_UNREACHABLE = "The server could not be reached. Check your connection and try again."
    const val SERVER_FAILED = "Something went wrong on the server. Please try again."

    fun signInPaused(minutes: Int?): String =
        if (minutes == null) {
            "Too many attempts. Sign-in is paused for a while, please try again later."
        } else {
            "Too many attempts. Sign-in is paused for about $minutes ${if (minutes == 1) "minute" else "minutes"}."
        }

    const val CHANGE_PASSWORD_TITLE = "Choose a new password"
    const val CHANGE_PASSWORD_INTRO = "Your password is temporary. Choose your own before you continue."
    const val CURRENT_PASSWORD = "Current password"
    const val NEW_PASSWORD = "New password"
    const val REPEAT_PASSWORD = "Repeat new password"
    const val CHANGE_PASSWORD = "Change password"
    const val CHANGING_PASSWORD = "Changing..."
    const val PASSWORDS_DO_NOT_MATCH = "The two passwords do not match."

    const val LOCAL_NETWORK_TITLE = "Allow access to your local network"
    const val LOCAL_NETWORK_WHY = "The server is on a private network (this computer or your home or office network). Android needs your permission before this app can connect to it."
    const val LOCAL_NETWORK_CONTINUE = "Continue"
    const val LOCAL_NETWORK_DENIED = "The server cannot be reached without access to your local network."
    const val LOCAL_NETWORK_TRY_AGAIN = "Try again"
    const val LOCAL_NETWORK_SETTINGS_HINT = "Android will not ask again. Allow \"Nearby devices\" or local network access for this app in its settings."
    const val LOCAL_NETWORK_OPEN_SETTINGS = "Open settings"

    const val PARTICIPANT_VIEW = "Participant"
    const val ADMIN_VIEW = "Admin"

    fun signedInAs(username: String) = "Signed in as $username"
    fun currentView(view: String) = "$view view"
    const val PARTICIPANT_PLACEHOLDER = "Your groups, wishlist and questions will appear here."
    const val ADMIN_PLACEHOLDER = "Group management will appear here."
}

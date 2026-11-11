package com.usuallycoding.opensecretsanta.ui.update

import com.usuallycoding.opensecretsanta.contract.ApiVersion
import com.usuallycoding.opensecretsanta.data.api.ApiClient
import com.usuallycoding.opensecretsanta.data.api.ApiResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Whether this app can be used with the server it talks to. */
sealed interface Compatibility {
    /** The server has not been heard from yet. */
    data object Checking : Compatibility

    /** Nothing says the app cannot be used. */
    data object Ok : Compatibility

    /** The server's API is newer than the one this app was built with; [androidDownload] says whether an Android app can be downloaded. */
    data class UpdateRequired(val androidDownload: Boolean) : Compatibility
}

/**
 * Compares the API version the server reports in `/health` with the one this app was built with ([ApiVersion.CURRENT]). Only a
 * successful answer with a higher number stops the app. An answer that is equal, lower or says nothing (an older server) lets it
 * go on, and so does a server that cannot be reached, when nothing has been heard yet. Once something has been heard, a failed
 * check changes nothing: a poor connection neither frees a stopped app nor stops a working one.
 */
class AppCompatibility(private val api: ApiClient) {
    private val _state = MutableStateFlow<Compatibility>(Compatibility.Checking)
    val state: StateFlow<Compatibility> = _state.asStateFlow()

    suspend fun check() {
        when (val result = api.health()) {
            is ApiResult.Success -> {
                val answer = result.value
                val serverApi = answer.apiVersion
                _state.value = if (serverApi != null && serverApi > ApiVersion.CURRENT) {
                    Compatibility.UpdateRequired(androidDownload = answer.androidDownload == true)
                } else {
                    Compatibility.Ok
                }
            }
            is ApiResult.Failure -> if (_state.value == Compatibility.Checking) _state.value = Compatibility.Ok
        }
    }
}

/** Which kind of screen the app shows, decided before the session's own screens. */
internal sealed interface AppScreen {
    /** The loading indicator: the server has not been heard from yet, or the session is being restored. */
    data object Loading : AppScreen

    /** The update screen, in place of everything else. */
    data class Update(val update: Compatibility.UpdateRequired) : AppScreen

    /** The screens that follow the session: the shell, sign-in and the password change. */
    data object Session : AppScreen
}

/**
 * The server is heard from first: until it has been, nothing else shows, so the sign-in screen and the shell never appear just
 * before a stop. A stop wins over any session. Otherwise the session decides, with its own loading indicator while it restores.
 */
internal fun screenFor(compatibility: Compatibility): AppScreen = when (compatibility) {
    Compatibility.Checking -> AppScreen.Loading
    is Compatibility.UpdateRequired -> AppScreen.Update(compatibility)
    Compatibility.Ok -> AppScreen.Session
}

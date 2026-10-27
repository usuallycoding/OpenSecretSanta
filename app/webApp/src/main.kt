package com.usuallycoding.opensecretsanta

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.usuallycoding.opensecretsanta.data.session.webTokenStore
import com.usuallycoding.opensecretsanta.data.web.resolveBaseUrl
import com.usuallycoding.opensecretsanta.di.initKoin
import com.usuallycoding.opensecretsanta.di.tokenStoreModule
import com.usuallycoding.opensecretsanta.ui.App
import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * `config.json` next to the app names the server; a missing or broken file means "the server this
 * page came from". The time in the query makes every start ask again, so a redeployed config is
 * picked up instead of a cached copy.
 */
@OptIn(ExperimentalTime::class, ExperimentalWasmJsInterop::class)
private suspend fun readConfigText(): String? = try {
    val response = window.fetch("config.json?t=${Clock.System.now().toEpochMilliseconds()}").await()
    if (response.ok) response.text().await<JsString>().toString() else null
} catch (_: Throwable) {
    null
}

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    MainScope().launch {
        initKoin(
            baseUrl = resolveBaseUrl(readConfigText(), window.location.origin),
            platformModule = tokenStoreModule { webTokenStore() },
        )
        ComposeViewport {
            App()
        }
    }
}

package com.usuallycoding.opensecretsanta

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import androidx.navigation.ExperimentalBrowserHistoryApi
import androidx.navigation.NavController
import androidx.navigation.bindToBrowserNavigation
import com.usuallycoding.opensecretsanta.data.push.WebPushDevice
import com.usuallycoding.opensecretsanta.data.session.webTokenStore
import com.usuallycoding.opensecretsanta.data.settings.BrowserSettingsStore
import com.usuallycoding.opensecretsanta.data.web.resolveBaseUrl
import com.usuallycoding.opensecretsanta.data.web.resolveFirebaseConfig
import com.usuallycoding.opensecretsanta.di.initKoin
import com.usuallycoding.opensecretsanta.di.settingsStoreModule
import com.usuallycoding.opensecretsanta.di.tokenStoreModule
import com.usuallycoding.opensecretsanta.ui.App
import com.usuallycoding.opensecretsanta.ui.navigation.WebAddresses
import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
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

@OptIn(ExperimentalWasmJsInterop::class)
private fun stateIsEmpty(event: JsAny): Boolean = js("event.state === null || event.state === undefined")

/**
 * The address the page was opened with. The browser binding rewrites the address bar as soon as it
 * starts, so it is read once, before anything else, and used when the person is signed in.
 */
private var openedWith: String? = window.location.hash

/**
 * Writes the readable address of each screen into the address bar, and shows the screen an address
 * names when the page is opened with one or one is typed. The back and forward buttons are the
 * navigation library's.
 */
@OptIn(ExperimentalBrowserHistoryApi::class, ExperimentalWasmJsInterop::class)
private suspend fun followAddresses(navController: NavController, isAdmin: Boolean) {
    coroutineScope {
        fun show(fragment: String) {
            val route = WebAddresses.routeOf(fragment, isAdmin) ?: return
            navController.navigate(route) { launchSingleTop = true }
        }
        // before the binding looks at the back stack, so that a refresh finds the history entry as it was
        openedWith?.let { show(it) }
        openedWith = null
        launch { navController.bindToBrowserNavigation { entry -> WebAddresses.addressOf(entry) ?: "" } }
        val onPopState: (JsAny) -> Unit = { event -> if (stateIsEmpty(event)) show(window.location.hash) }
        window.addEventListener("popstate", onPopState)
        try {
            awaitCancellation()
        } finally {
            window.removeEventListener("popstate", onPopState)
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    MainScope().launch {
        val configText = readConfigText()
        initKoin(
            baseUrl = resolveBaseUrl(configText, window.location.origin),
            platformModules = listOf(tokenStoreModule { webTokenStore() }, settingsStoreModule { BrowserSettingsStore() }),
            pushDevice = WebPushDevice(resolveFirebaseConfig(configText)),
        )
        ComposeViewport {
            // The back button and the address bar follow the screens the participant moves between.
            App(onNavigationReady = { navController, isAdmin -> followAddresses(navController, isAdmin) })
        }
    }
}

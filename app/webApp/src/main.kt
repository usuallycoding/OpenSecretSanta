package com.usuallycoding.opensecretsanta

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.usuallycoding.opensecretsanta.di.initKoin
import com.usuallycoding.opensecretsanta.ui.App
import kotlinx.browser.window

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    // The web app talks to the server it is served from, so the page origin is the base URL.
    initKoin(baseUrl = window.location.origin)
    ComposeViewport {
        App()
    }
}

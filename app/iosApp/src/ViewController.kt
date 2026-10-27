package com.usuallycoding.opensecretsanta

import androidx.compose.ui.window.ComposeUIViewController
import com.usuallycoding.opensecretsanta.data.session.KeychainTokenStore
import com.usuallycoding.opensecretsanta.di.initKoin
import com.usuallycoding.opensecretsanta.di.tokenStoreModule
import com.usuallycoding.opensecretsanta.ui.App

fun ViewController() = run {
    // The simulator shares the host's network, so localhost reaches the development server.
    initKoin(baseUrl = "http://localhost:8080", platformModule = tokenStoreModule { KeychainTokenStore() })
    ComposeUIViewController { App() }
}

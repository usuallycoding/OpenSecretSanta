package com.usuallycoding.opensecretsanta.ui.download

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalUriHandler
import org.koin.compose.koinInject

/**
 * What choosing "Download Android app" does, or null while it is not to be offered (on the phones, where the server has no app to
 * give, or where it could not be asked). The server is asked each time the screen appears. The address is opened by the browser,
 * and the answer is a download, so the screen stays where it is; when nothing can open it, nothing happens.
 */
@Composable
fun androidDownloadAction(): (() -> Unit)? {
    val offer = koinInject<AndroidDownloadOffer>()
    val offered by offer.offered.collectAsState()
    val uriHandler = LocalUriHandler.current
    LaunchedEffect(offer) { offer.refresh() }
    return if (offered) ({ runCatching { uriHandler.openUri(offer.url) }; Unit }) else null
}

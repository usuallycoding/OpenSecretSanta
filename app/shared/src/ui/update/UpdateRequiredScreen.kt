package com.usuallycoding.opensecretsanta.ui.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.usuallycoding.opensecretsanta.UpdateWay
import com.usuallycoding.opensecretsanta.data.api.ApiClient
import com.usuallycoding.opensecretsanta.getPlatform
import com.usuallycoding.opensecretsanta.ui.Strings
import org.koin.compose.koinInject

/**
 * Shown in place of everything else when the server's API is newer than the app understands. It says so, and offers what the
 * platform can do about it: reload the page on the web, download the Android app on Android when the server has one, and on
 * iOS only the text.
 */
@Composable
fun UpdateRequiredScreen(update: Compatibility.UpdateRequired) {
    val platform = getPlatform()
    Column(
        modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(Strings.UPDATE_REQUIRED_TITLE, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Text(
            if (platform.updateWay == UpdateWay.Reload) Strings.UPDATE_REQUIRED_RELOAD_BODY else Strings.UPDATE_REQUIRED_BODY,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        when (platform.updateWay) {
            UpdateWay.Reload -> Button(onClick = platform::reload, modifier = Modifier.fillMaxWidth()) { Text(Strings.RELOAD) }
            UpdateWay.Download -> if (update.androidDownload) {
                val api = koinInject<ApiClient>()
                val uriHandler = LocalUriHandler.current
                // nothing can open the address on some devices, which is not worth a message: the button then does nothing
                Button(onClick = { runCatching { uriHandler.openUri(api.androidDownloadUrl()) } }, modifier = Modifier.fillMaxWidth()) {
                    Text(Strings.DOWNLOAD_UPDATE)
                }
            }
            UpdateWay.None -> Unit
        }
    }
}

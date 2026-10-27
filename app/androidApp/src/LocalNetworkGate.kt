package com.usuallycoding.opensecretsanta

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.usuallycoding.opensecretsanta.data.isLocalNetworkAddress
import com.usuallycoding.opensecretsanta.ui.Strings

private const val LOCAL_NETWORK_PERMISSION = "android.permission.ACCESS_LOCAL_NETWORK"
private const val FIRST_ANDROID_WITH_LOCAL_NETWORK_PERMISSION = 37

/**
 * Android 17 refuses connections to private-network addresses (the development server on this
 * machine, a server at home or at the office) until the app holds the local-network permission, and
 * a refused connection just hangs. For such an address this explains why, asks, and says what to do
 * when the answer is no, before the app contacts the server. For every other address, and on older
 * Android versions, it shows the app straight away.
 */
@Composable
fun LocalNetworkGate(baseUrl: String, content: @Composable () -> Unit) {
    val needed = Build.VERSION.SDK_INT >= FIRST_ANDROID_WITH_LOCAL_NETWORK_PERMISSION && isLocalNetworkAddress(baseUrl)
    if (!needed) {
        content()
        return
    }

    val context = LocalContext.current
    var granted by remember { mutableStateOf(context.hasLocalNetworkPermission()) }
    var denied by remember { mutableStateOf(false) }
    // After a denial the system only shows its request again while it still considers a rationale
    // useful. Read when the answer arrives, because a second denial changes nothing else on screen.
    var canAskAgain by remember { mutableStateOf(true) }

    // The permission can also be granted in the system settings while this app is in the background.
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) granted = context.hasLocalNetworkPermission()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    val request = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { allowed ->
        granted = allowed
        denied = !allowed
        canAskAgain = allowed || context.findActivity()?.shouldShowRequestPermissionRationale(LOCAL_NETWORK_PERMISSION) == true
    }

    if (granted) {
        content()
        return
    }

    GateScreen {
        Text(Strings.LOCAL_NETWORK_TITLE, style = MaterialTheme.typography.headlineSmall)
        if (denied) Text(Strings.LOCAL_NETWORK_DENIED, color = MaterialTheme.colorScheme.error)
        Text(Strings.LOCAL_NETWORK_WHY)
        if (denied && !canAskAgain) {
            Text(Strings.LOCAL_NETWORK_SETTINGS_HINT)
            Button(onClick = { context.startActivity(appSettingsIntent(context)) }, modifier = Modifier.fillMaxWidth()) {
                Text(Strings.LOCAL_NETWORK_OPEN_SETTINGS)
            }
            OutlinedButton(onClick = { granted = context.hasLocalNetworkPermission() }, modifier = Modifier.fillMaxWidth()) {
                Text(Strings.LOCAL_NETWORK_TRY_AGAIN)
            }
        } else {
            Button(onClick = { request.launch(LOCAL_NETWORK_PERMISSION) }, modifier = Modifier.fillMaxWidth()) {
                Text(if (denied) Strings.LOCAL_NETWORK_TRY_AGAIN else Strings.LOCAL_NETWORK_CONTINUE)
            }
        }
    }
}

@Composable
private fun GateScreen(content: @Composable () -> Unit) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize().safeContentPadding(), contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) { content() }
        }
    }
}

private fun Context.hasLocalNetworkPermission(): Boolean =
    checkSelfPermission(LOCAL_NETWORK_PERMISSION) == PackageManager.PERMISSION_GRANTED

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun appSettingsIntent(context: Context) =
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))

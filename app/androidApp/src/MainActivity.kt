package com.usuallycoding.opensecretsanta

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.usuallycoding.opensecretsanta.data.BackendConfig
import com.usuallycoding.opensecretsanta.ui.App
import com.usuallycoding.opensecretsanta.ui.navigation.GroupRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import org.koin.mp.KoinPlatform

class MainActivity : ComponentActivity() {
    private val permission get() = (application as OpenSecretSantaApplication).notificationPermission

    // registered here, before the activity starts, as the result API requires
    private val askForNotifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) { permission.onResult(it) }

    /** The group a tapped notification is about, until the signed-in app has opened it. */
    private val requestedGroup = MutableStateFlow<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        permission.launcher = { askForNotifications.launch(it) }
        requestedGroup.value = groupOf(intent)
        val baseUrl = KoinPlatform.getKoin().get<BackendConfig>().baseUrl
        setContent {
            App(
                gate = { content -> LocalNetworkGate(baseUrl, content) },
                // the app is open or signed in only when the navigation exists, so a request made before that waits for it
                onNavigationReady = { navController, _ ->
                    requestedGroup.filterNotNull().collect { id ->
                        navController.navigate(GroupRoute(id)) { launchSingleTop = true }
                        requestedGroup.value = null
                        intent.removeExtra(EXTRA_GROUP_ID)
                    }
                },
            )
        }
    }

    // a tap on a notification while the app is already open
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        requestedGroup.value = groupOf(intent)
    }

    override fun onDestroy() {
        permission.launcher = null
        super.onDestroy()
    }

    private fun groupOf(intent: Intent?): Long? = intent?.getLongExtra(EXTRA_GROUP_ID, NONE)?.takeIf { it != NONE }

    private companion object {
        const val NONE = -1L
    }
}

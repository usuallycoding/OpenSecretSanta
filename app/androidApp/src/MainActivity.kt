package com.usuallycoding.opensecretsanta

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.usuallycoding.opensecretsanta.data.BackendConfig
import com.usuallycoding.opensecretsanta.ui.App
import com.usuallycoding.opensecretsanta.ui.navigation.LinkTarget
import com.usuallycoding.opensecretsanta.ui.navigation.NotificationTarget
import com.usuallycoding.opensecretsanta.ui.navigation.openNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import org.koin.mp.KoinPlatform

class MainActivity : ComponentActivity() {
    private val permission get() = (application as OpenSecretSantaApplication).notificationPermission

    // registered here, before the activity starts, as the result API requires
    private val askForNotifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        // only the activity can tell whether the system would still show its question
        permission.onResult(granted, shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS))
    }

    /** The screens a tapped notification opens, until the signed-in app has opened them. */
    private val requestedRoutes = MutableStateFlow<List<Any>?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        permission.launcher = { askForNotifications.launch(it) }
        requestedRoutes.value = routesOf(intent)
        val baseUrl = KoinPlatform.getKoin().get<BackendConfig>().baseUrl
        setContent {
            App(
                gate = { content -> LocalNetworkGate(baseUrl, content) },
                // the app is open or signed in only when the navigation exists, so a request made before that waits for it
                onNavigationReady = { navController, _ ->
                    requestedRoutes.filterNotNull().collect { routes ->
                        navController.openNotification(routes)
                        requestedRoutes.value = null
                        intent.removeExtra(EXTRA_GROUP_ID)
                        intent.removeExtra(EXTRA_CATEGORY)
                        // a link is handled once, so an activity that is recreated does not open its group again
                        intent.data = null
                    }
                },
            )
        }
    }

    // a tap on a notification while the app is already open
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        requestedRoutes.value = routesOf(intent)
    }

    override fun onDestroy() {
        permission.launcher = null
        super.onDestroy()
    }

    /**
     * What a tap on a notification opens: its group, and the page its kind is about; or what a link to the web app names, when
     * it names a group screen. Nothing for a launch that is neither.
     */
    private fun routesOf(intent: Intent?): List<Any>? {
        val groupId = intent?.getLongExtra(EXTRA_GROUP_ID, NONE)?.takeIf { it != NONE }
        if (groupId != null) return NotificationTarget.routesOf(intent.getStringExtra(EXTRA_CATEGORY), groupId)
        val link = intent?.data ?: return null
        return LinkTarget.routesOf(linkFragment(link.scheme, link.host, link.fragment))
    }

    private companion object {
        const val NONE = -1L
    }
}

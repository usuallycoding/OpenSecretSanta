package com.usuallycoding.opensecretsanta

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.usuallycoding.opensecretsanta.data.push.PushRegistration
import kotlinx.coroutines.launch
import org.koin.mp.KoinPlatform

/**
 * Receives the server's push messages, whether the app is open or not (they carry data only, so this always runs), and
 * keeps the server's copy of this device's token current.
 */
class OpenSecretSantaMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        showNotification(applicationContext, message.data)
    }

    // Firebase marks this callback as deprecated for a newer one; it is still called for every new token
    @Suppress("OVERRIDE_DEPRECATION")
    override fun onNewToken(token: String) {
        val registration = KoinPlatform.getKoinOrNull()?.get<PushRegistration>() ?: return
        // the service may stop before the call is through, so it runs on the application's scope; a start of the app does the same later
        OpenSecretSantaApplication.scope.launch { registration.resume() }
    }
}

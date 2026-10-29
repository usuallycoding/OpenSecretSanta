package com.usuallycoding.opensecretsanta

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import com.google.android.gms.tasks.Task
import com.google.firebase.messaging.FirebaseMessaging
import com.usuallycoding.opensecretsanta.data.push.PushDevice
import com.usuallycoding.opensecretsanta.data.push.PushEnable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * The notification permission: whether it is granted, and a way to ask for it. Asking needs an activity, so the activity
 * hands over a [launcher] while it exists and reports the person's answer to [onResult].
 */
class NotificationPermission(private val context: Context) {
    /** Starts the system's permission request; set by the activity while it is there, null otherwise. */
    var launcher: ((permission: String) -> Unit)? = null

    private var pending: CompletableDeferred<Boolean>? = null

    /** Covers both the runtime permission of Android 13 and later and the app's notification switch in the system settings. */
    fun isGranted(): Boolean = NotificationManagerCompat.from(context).areNotificationsEnabled()

    /** Asks if the permission is not granted yet and the system can still show the question; answers whether it is granted now. */
    suspend fun request(): Boolean {
        if (isGranted()) return true
        // before Android 13 there is no question to ask: the only way is the app's switch in the settings
        val launch = launcher
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || launch == null) return false
        val answer = CompletableDeferred<Boolean>()
        pending = answer
        launch(Manifest.permission.POST_NOTIFICATIONS)
        return answer.await() && isGranted()
    }

    fun onResult(granted: Boolean) {
        pending?.complete(granted)
        pending = null
    }
}

/**
 * Push on Android: Firebase Cloud Messaging for the token, and [permission] for the system's question.
 *
 * Firebase now marks `getToken` and `deleteToken` as deprecated in favour of `register` and a callback in the messaging
 * service. They are still the documented way to get and drop a token, so they are used as they are.
 */
@Suppress("DEPRECATION")
class AndroidPushDevice(private val permission: NotificationPermission) : PushDevice {
    override val platform: String = "android"
    override val isSupported: Boolean = true

    override suspend fun isPermitted(): Boolean = permission.isGranted()

    override suspend fun enable(): PushEnable {
        if (!permission.request()) return PushEnable.Denied
        return currentToken()?.let { PushEnable.Ready(it) } ?: PushEnable.Failed
    }

    override suspend fun currentToken(): String? {
        if (!permission.isGranted()) return null
        return try {
            FirebaseMessaging.getInstance().getToken().await()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun disable() {
        try {
            FirebaseMessaging.getInstance().deleteToken().await()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // the token stays until the server finds it dead; nothing more can be done here
        }
    }
}

/** Waits for a Google Play services task without a library for it. */
private suspend fun <T> Task<T>.await(): T = suspendCoroutine { continuation ->
    addOnCompleteListener { task ->
        val error = task.exception
        if (error != null) continuation.resumeWithException(error) else continuation.resume(task.result)
    }
}

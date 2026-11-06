package com.usuallycoding.opensecretsanta

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
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

/** How asking for the notification permission came out. */
enum class PermissionAnswer {
    Granted,

    /** Refused, and the system will ask again the next time. */
    Denied,

    /** Refused, and the system will not ask: only the app's notification settings can allow it. */
    Blocked,
}

/**
 * What the state of the permission amounts to once the person has been asked: [granted] wins. Before Android 13 there is no
 * question, so an ungranted permission can only be allowed in the settings; from Android 13 it is the same once the system
 * will not ask again ([canAskAgain] false).
 */
fun notificationAnswer(sdkInt: Int, granted: Boolean, canAskAgain: Boolean): PermissionAnswer = when {
    granted -> PermissionAnswer.Granted
    sdkInt < Build.VERSION_CODES.TIRAMISU -> PermissionAnswer.Blocked
    !canAskAgain -> PermissionAnswer.Blocked
    else -> PermissionAnswer.Denied
}

/**
 * The notification permission: whether it is granted, a way to ask for it, and a way to the settings where it can be allowed.
 * Asking needs an activity, so the activity hands over a [launcher] while it exists and reports the person's answer to [onResult].
 */
class NotificationPermission(private val context: Context) {
    /** Starts the system's permission request; set by the activity while it is there, null otherwise. */
    var launcher: ((permission: String) -> Unit)? = null

    private class Result(val granted: Boolean, val canAskAgain: Boolean)

    private var pending: CompletableDeferred<Result>? = null

    /** Covers both the runtime permission of Android 13 and later and the app's notification switch in the system settings. */
    fun isGranted(): Boolean = NotificationManagerCompat.from(context).areNotificationsEnabled()

    /** Asks if the permission is not granted yet and the system can still show the question; says how that came out. */
    suspend fun request(): PermissionAnswer {
        if (isGranted()) return PermissionAnswer.Granted
        // before Android 13 there is no question to ask: the only way is the app's switch in the settings
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return notificationAnswer(Build.VERSION.SDK_INT, granted = false, canAskAgain = false)
        // no activity to ask from: nothing was refused, so nothing is blocked either
        val launch = launcher ?: return PermissionAnswer.Denied
        val answer = CompletableDeferred<Result>()
        pending = answer
        launch(Manifest.permission.POST_NOTIFICATIONS)
        val result = answer.await()
        return notificationAnswer(Build.VERSION.SDK_INT, result.granted && isGranted(), result.canAskAgain)
    }

    /** The person's answer; [canAskAgain] is whether the system would still show its question, which only the activity can tell. */
    fun onResult(granted: Boolean, canAskAgain: Boolean) {
        pending?.complete(Result(granted, canAskAgain))
        pending = null
    }

    /** Opens the app's notification settings, or failing that its settings page; does nothing if the device has neither. */
    fun openSettings() {
        val newTask = Intent.FLAG_ACTIVITY_NEW_TASK
        val notifications = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName).addFlags(newTask)
        val details = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)).addFlags(newTask)
        // the notification settings page exists from Android 8
        val first = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) notifications else details
        try {
            context.startActivity(first)
        } catch (_: ActivityNotFoundException) {
            try {
                context.startActivity(details)
            } catch (_: ActivityNotFoundException) {
                // nowhere to go: the message stays
            }
        }
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

    override val canOpenSettings: Boolean = true

    override fun openSettings() = permission.openSettings()

    override suspend fun enable(): PushEnable {
        when (permission.request()) {
            PermissionAnswer.Denied -> return PushEnable.Denied
            PermissionAnswer.Blocked -> return PushEnable.Blocked
            PermissionAnswer.Granted -> Unit
        }
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

package com.usuallycoding.opensecretsanta

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.usuallycoding.opensecretsanta.contract.NotificationData
import com.usuallycoding.opensecretsanta.ui.Strings

/** The four kinds of notification, as Android's notification channels: the system settings list them like the app's own switches. */
private enum class Channel(val id: String, val title: String) {
    Announcements("announcements", Strings.NOTIFICATIONS_ANNOUNCEMENTS),
    Wishlist("wishlist", Strings.NOTIFICATIONS_WISHLIST),
    Questions("questions", Strings.NOTIFICATIONS_QUESTIONS),
    Answers("answers", Strings.NOTIFICATIONS_ANSWERS),
}

/** The extra that carries the group a tapped notification is about. */
const val EXTRA_GROUP_ID = "com.usuallycoding.opensecretsanta.GROUP_ID"

/** Creates the four channels (Android 8 and later); doing it again changes nothing. */
fun createNotificationChannels(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val manager = context.getSystemService(NotificationManager::class.java) ?: return
    Channel.entries.forEach { manager.createNotificationChannel(NotificationChannel(it.id, it.title, NotificationManager.IMPORTANCE_DEFAULT)) }
}

/** Shows the notification a push message asks for, if the person allows notifications. Nothing in it is private: the server sees to that. */
fun showNotification(context: Context, data: Map<String, String>) {
    val manager = NotificationManagerCompat.from(context)
    if (!manager.areNotificationsEnabled()) return
    val channel = Channel.entries.firstOrNull { it.id == data[NotificationData.CATEGORY] } ?: return
    val groupId = data[NotificationData.GROUP_ID]?.toLongOrNull()
    val title = data[NotificationData.TITLE] ?: Strings.APP_NAME
    val body = data[NotificationData.BODY] ?: return

    // a newer notification of a kind for a group replaces the older one
    val id = (groupId ?: 0L).hashCode()
    val open = Intent(context, MainActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        .apply { if (groupId != null) putExtra(EXTRA_GROUP_ID, groupId) }
    val tap = PendingIntent.getActivity(context, id * 4 + channel.ordinal, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    val notification = NotificationCompat.Builder(context, channel.id)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(title)
        .setContentText(body)
        .setStyle(NotificationCompat.BigTextStyle().bigText(body))
        .setContentIntent(tap)
        .setAutoCancel(true)
        .build()
    try {
        manager.notify(channel.id, id, notification)
    } catch (_: SecurityException) {
        // the permission was taken away a moment ago
    }
}

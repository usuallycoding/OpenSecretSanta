package com.usuallycoding.opensecretsanta

import android.app.Application
import android.content.pm.ApplicationInfo
import com.usuallycoding.opensecretsanta.data.session.AndroidTokenStore
import com.usuallycoding.opensecretsanta.data.settings.AndroidSettingsStore
import com.usuallycoding.opensecretsanta.di.initKoin
import com.usuallycoding.opensecretsanta.di.settingsStoreModule
import com.usuallycoding.opensecretsanta.di.tokenStoreModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class OpenSecretSantaApplication : Application() {
    /** Asked for by the push device; the activity gives it the launcher that can show the system's question. */
    val notificationPermission by lazy { NotificationPermission(this) }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels(this)
        val debuggable = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        initKoin(
            baseUrl = serverAddress(debuggable),
            platformModules = listOf(tokenStoreModule { AndroidTokenStore(this) }, settingsStoreModule { AndroidSettingsStore(this) }),
            pushDevice = AndroidPushDevice(notificationPermission),
        )
    }

    companion object {
        /** Work that must outlive the component that started it, such as the messaging service. */
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}

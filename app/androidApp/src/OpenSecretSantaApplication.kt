package com.usuallycoding.opensecretsanta

import android.app.Application
import com.usuallycoding.opensecretsanta.data.session.AndroidTokenStore
import com.usuallycoding.opensecretsanta.di.initKoin
import com.usuallycoding.opensecretsanta.di.tokenStoreModule

class OpenSecretSantaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // 10.0.2.2 is the emulator's alias for the development machine's localhost.
        initKoin(
            baseUrl = "http://10.0.2.2:8080",
            platformModule = tokenStoreModule { AndroidTokenStore(this) },
        )
    }
}

package com.usuallycoding.opensecretsanta.di

import com.usuallycoding.opensecretsanta.data.BackendConfig
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.mp.KoinPlatform

fun appModules(baseUrl: String): List<Module> = listOf(
    module { single { BackendConfig(baseUrl) } },
)

/**
 * Starts the shared dependency graph. Hosts call this once at launch, before the first screen
 * composes; repeated calls (for example after an Android configuration change) are ignored.
 */
fun initKoin(baseUrl: String, platformModule: Module = module { }) {
    if (KoinPlatform.getKoinOrNull() != null) return
    startKoin {
        modules(appModules(baseUrl) + platformModule)
    }
}

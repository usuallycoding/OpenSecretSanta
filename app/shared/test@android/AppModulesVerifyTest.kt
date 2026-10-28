package com.usuallycoding.opensecretsanta

import com.usuallycoding.opensecretsanta.data.session.Account
import com.usuallycoding.opensecretsanta.data.session.TokenStore
import com.usuallycoding.opensecretsanta.data.settings.SettingsStore
import com.usuallycoding.opensecretsanta.di.appModules
import io.ktor.client.engine.HttpClientEngine
import kotlinx.coroutines.CoroutineScope
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.module
import org.koin.test.verify.verify
import kotlin.test.Test

/** Koin's verify() relies on reflection, so the graph check runs on the JVM-based Android unit tests. */
class AppModulesVerifyTest {

    // Types that are supplied by something other than this module's own definitions: the platform's
    // token store and settings store (a host passes them), per-view-model scopes and the signed-in account (created where
    // the view model is requested), and the engine the HTTP client picks for itself.
    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun moduleGraphIsComplete() {
        module { includes(appModules(baseUrl = "http://localhost:8080")) }.verify(
            extraTypes = listOf(TokenStore::class, SettingsStore::class, CoroutineScope::class, Account::class, HttpClientEngine::class, Lazy::class),
        )
    }
}

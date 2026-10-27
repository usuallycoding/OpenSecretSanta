package com.usuallycoding.opensecretsanta

import com.usuallycoding.opensecretsanta.di.appModules
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.module
import org.koin.test.verify.verify
import kotlin.test.Test

/** Koin's verify() relies on reflection, so the graph check runs on the JVM-based Android unit tests. */
class AppModulesVerifyTest {

    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun moduleGraphIsComplete() {
        module { includes(appModules(baseUrl = "http://localhost:8080")) }.verify()
    }
}

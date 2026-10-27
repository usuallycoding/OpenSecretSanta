package com.usuallycoding.opensecretsanta.server

import com.usuallycoding.opensecretsanta.server.di.serverModules
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.module
import org.koin.test.verify.verify
import kotlin.test.Test

class ServerModulesTest {

    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun moduleGraphIsComplete() {
        val config = testConfig()
        module { includes(serverModules(config, migrations = emptyList())) }.verify()
    }
}

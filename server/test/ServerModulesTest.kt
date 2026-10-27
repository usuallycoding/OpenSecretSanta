package com.usuallycoding.opensecretsanta.server

import com.usuallycoding.opensecretsanta.server.config.ServerConfig
import com.usuallycoding.opensecretsanta.server.di.serverModules
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.module
import org.koin.test.verify.verify
import kotlin.test.Test

class ServerModulesTest {

    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun moduleGraphIsComplete() {
        val config = ServerConfig(jwtSecret = "s", dbPath = "unused.db", port = 8080)
        module { includes(serverModules(config, migrations = emptyList())) }.verify()
    }
}

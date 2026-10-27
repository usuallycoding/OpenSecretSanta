package com.usuallycoding.opensecretsanta.server

import com.usuallycoding.opensecretsanta.contract.ErrorResponse
import com.usuallycoding.opensecretsanta.server.config.ConfigurationException
import com.usuallycoding.opensecretsanta.server.config.ServerConfig
import com.usuallycoding.opensecretsanta.server.data.db.ClasspathMigrations
import com.usuallycoding.opensecretsanta.server.data.db.Migration
import com.usuallycoding.opensecretsanta.server.data.db.MigrationRunner
import com.usuallycoding.opensecretsanta.server.di.serverModules
import com.usuallycoding.opensecretsanta.server.presentation.HealthService
import com.usuallycoding.opensecretsanta.server.presentation.healthRoutes
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.routing
import org.koin.ktor.ext.get
import org.koin.ktor.plugin.Koin
import kotlin.system.exitProcess

fun main() {
    val config = try {
        ServerConfig.fromEnv()
    } catch (e: ConfigurationException) {
        System.err.println(e.message)
        exitProcess(1)
    }
    embeddedServer(Netty, port = config.port, host = "0.0.0.0") { module(config) }
        .start(wait = true)
}

fun Application.module(
    config: ServerConfig,
    migrations: List<Migration> = ClasspathMigrations.load(),
) {
    install(Koin) {
        modules(serverModules(config, migrations))
    }
    install(ContentNegotiation) { json() }
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.application.log.error("Unhandled exception", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse("internal_error", "An unexpected error occurred."),
            )
        }
    }

    get<MigrationRunner>().migrate()

    routing {
        healthRoutes(get<HealthService>())
    }
}

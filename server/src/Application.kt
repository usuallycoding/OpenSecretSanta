package com.usuallycoding.opensecretsanta.server

import com.usuallycoding.opensecretsanta.domain.auth.Clock
import com.usuallycoding.opensecretsanta.domain.auth.TokenService
import com.usuallycoding.opensecretsanta.domain.user.UserRepository
import com.usuallycoding.opensecretsanta.server.config.ConfigurationException
import com.usuallycoding.opensecretsanta.server.config.ServerConfig
import com.usuallycoding.opensecretsanta.server.data.db.ClasspathMigrations
import com.usuallycoding.opensecretsanta.server.data.db.Migration
import com.usuallycoding.opensecretsanta.server.data.db.MigrationRunner
import com.usuallycoding.opensecretsanta.server.di.serverModules
import com.usuallycoding.opensecretsanta.domain.admin.GetManagedUser
import com.usuallycoding.opensecretsanta.domain.admin.SeedDefaultAdmin
import com.usuallycoding.opensecretsanta.domain.error.DomainException
import com.usuallycoding.opensecretsanta.domain.admin.ListManagedUsers
import com.usuallycoding.opensecretsanta.domain.admin.PromoteUser
import com.usuallycoding.opensecretsanta.domain.admin.ResetPassword
import com.usuallycoding.opensecretsanta.domain.auth.ChangePassword
import com.usuallycoding.opensecretsanta.domain.auth.Login
import com.usuallycoding.opensecretsanta.domain.user.UpdateProfile
import com.usuallycoding.opensecretsanta.server.presentation.ACCESS_AUTH
import com.usuallycoding.opensecretsanta.server.presentation.HealthService
import com.usuallycoding.opensecretsanta.server.presentation.PasswordChangeGate
import com.usuallycoding.opensecretsanta.server.presentation.accountRoutes
import com.usuallycoding.opensecretsanta.server.presentation.adminRoutes
import com.usuallycoding.opensecretsanta.server.presentation.profileRoutes
import com.usuallycoding.opensecretsanta.server.presentation.authRoutes
import com.usuallycoding.opensecretsanta.server.presentation.registerLoginLimit
import com.usuallycoding.opensecretsanta.server.presentation.healthRoutes
import com.usuallycoding.opensecretsanta.server.presentation.installAccessTokens
import com.usuallycoding.opensecretsanta.server.presentation.installErrorHandling
import com.usuallycoding.opensecretsanta.server.security.SystemClock
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.ratelimit.RateLimit
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.routing.routing
import org.koin.ktor.ext.get
import org.koin.ktor.plugin.Koin
import kotlin.system.exitProcess

fun main() {
    try {
        val config = ServerConfig.fromEnv()
        embeddedServer(Netty, port = config.port, host = "0.0.0.0") { module(config) }
            .start(wait = true)
    } catch (e: Throwable) {
        // A bad setting is an operator mistake: report it plainly instead of with a stack trace.
        val configurationError = generateSequence(e) { it.cause }.filterIsInstance<ConfigurationException>().firstOrNull()
            ?: throw e
        System.err.println(configurationError.message)
        exitProcess(1)
    }
}

fun Application.module(
    config: ServerConfig,
    migrations: List<Migration> = ClasspathMigrations.load(),
    clock: Clock = SystemClock,
) {
    install(Koin) {
        modules(serverModules(config, migrations, clock))
    }
    install(ContentNegotiation) { json() }
    install(StatusPages) { installErrorHandling() }
    val users = get<UserRepository>()
    val tokens = get<TokenService>()
    install(Authentication) { installAccessTokens(users, tokens) }
    install(RateLimit) { registerLoginLimit() }

    get<MigrationRunner>().migrate()
    try {
        get<SeedDefaultAdmin>()(config.adminUsername, config.adminPassword)
    } catch (e: DomainException.ValidationFailed) {
        throw ConfigurationException(e.field, e.message.orEmpty())
    }

    routing {
        healthRoutes(get<HealthService>())
        authRoutes(get<Login>())

        authenticate(ACCESS_AUTH) {
            install(PasswordChangeGate)
            accountRoutes(get<ChangePassword>())
            profileRoutes(get<UpdateProfile>())
            adminRoutes(get<ListManagedUsers>(), get<GetManagedUser>(), get<ResetPassword>(), get<PromoteUser>())
        }
    }
}

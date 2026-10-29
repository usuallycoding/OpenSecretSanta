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
import com.usuallycoding.opensecretsanta.domain.email.Mailer
import com.usuallycoding.opensecretsanta.domain.notification.GetNotificationSettings
import com.usuallycoding.opensecretsanta.domain.notification.PushSender
import com.usuallycoding.opensecretsanta.domain.notification.RegisterDevice
import com.usuallycoding.opensecretsanta.domain.notification.UnregisterDevice
import com.usuallycoding.opensecretsanta.domain.notification.UpdateNotificationSettings
import com.usuallycoding.opensecretsanta.server.presentation.notificationRoutes
import com.usuallycoding.opensecretsanta.server.notification.NotificationDispatcher
import com.usuallycoding.opensecretsanta.server.notification.Scheduler
import java.util.concurrent.Executor
import com.usuallycoding.opensecretsanta.domain.email.SendDrawEmails
import com.usuallycoding.opensecretsanta.domain.error.DomainException
import com.usuallycoding.opensecretsanta.domain.admin.ListManagedUsers
import com.usuallycoding.opensecretsanta.domain.admin.ResetPassword
import com.usuallycoding.opensecretsanta.domain.auth.ChangePassword
import com.usuallycoding.opensecretsanta.domain.auth.Login
import com.usuallycoding.opensecretsanta.domain.gift.AnswerQuestion
import com.usuallycoding.opensecretsanta.domain.gift.AskQuestion
import com.usuallycoding.opensecretsanta.domain.gift.GetMyReceiver
import com.usuallycoding.opensecretsanta.domain.gift.GetMySanta
import com.usuallycoding.opensecretsanta.domain.gift.ListMyQuestions
import com.usuallycoding.opensecretsanta.domain.gift.ListQuestionThreads
import com.usuallycoding.opensecretsanta.domain.gift.ListQuestionsFromMySanta
import com.usuallycoding.opensecretsanta.domain.group.ActivateGroup
import com.usuallycoding.opensecretsanta.domain.group.ArchiveGroup
import com.usuallycoding.opensecretsanta.domain.group.CreateGroup
import com.usuallycoding.opensecretsanta.domain.group.DeleteGroup
import com.usuallycoding.opensecretsanta.domain.group.GetMyGroup
import com.usuallycoding.opensecretsanta.domain.group.GetOwnedGroup
import com.usuallycoding.opensecretsanta.domain.group.ListAssignments
import com.usuallycoding.opensecretsanta.domain.group.ListMyGroups
import com.usuallycoding.opensecretsanta.domain.group.ListOwnedGroups
import com.usuallycoding.opensecretsanta.domain.group.MoveToPending
import com.usuallycoding.opensecretsanta.domain.group.SetParticipants
import com.usuallycoding.opensecretsanta.domain.group.Shuffler
import com.usuallycoding.opensecretsanta.domain.group.UpdateGroupDetails
import com.usuallycoding.opensecretsanta.domain.user.UpdateProfile
import com.usuallycoding.opensecretsanta.domain.wishlist.AddWishlistItem
import com.usuallycoding.opensecretsanta.domain.wishlist.DeleteWishlistItem
import com.usuallycoding.opensecretsanta.domain.wishlist.GetMyWishlist
import com.usuallycoding.opensecretsanta.domain.wishlist.GetParticipantWishlist
import com.usuallycoding.opensecretsanta.domain.wishlist.GetReceiverWishlist
import com.usuallycoding.opensecretsanta.domain.wishlist.ReorderWishlist
import com.usuallycoding.opensecretsanta.domain.wishlist.UpdateWishlistItem
import com.usuallycoding.opensecretsanta.server.presentation.ACCESS_AUTH
import com.usuallycoding.opensecretsanta.server.presentation.HealthService
import com.usuallycoding.opensecretsanta.server.presentation.PasswordChangeGate
import com.usuallycoding.opensecretsanta.server.presentation.accountRoutes
import com.usuallycoding.opensecretsanta.server.presentation.allowConfiguredOrigins
import com.usuallycoding.opensecretsanta.server.presentation.adminGroupRoutes
import com.usuallycoding.opensecretsanta.server.presentation.adminOversightRoutes
import com.usuallycoding.opensecretsanta.server.presentation.adminRoutes
import com.usuallycoding.opensecretsanta.server.presentation.giftVisibilityRoutes
import com.usuallycoding.opensecretsanta.server.presentation.groupLifecycleRoutes
import com.usuallycoding.opensecretsanta.server.presentation.memberGroupRoutes
import com.usuallycoding.opensecretsanta.server.presentation.questionRoutes
import com.usuallycoding.opensecretsanta.server.presentation.receiverWishlistRoutes
import com.usuallycoding.opensecretsanta.server.presentation.wishlistRoutes
import com.usuallycoding.opensecretsanta.server.presentation.profileRoutes
import com.usuallycoding.opensecretsanta.server.presentation.authRoutes
import com.usuallycoding.opensecretsanta.server.presentation.registerLoginLimit
import com.usuallycoding.opensecretsanta.server.presentation.healthRoutes
import com.usuallycoding.opensecretsanta.server.presentation.installAccessTokens
import com.usuallycoding.opensecretsanta.server.presentation.installErrorHandling
import com.usuallycoding.opensecretsanta.server.security.SecureShuffler
import com.usuallycoding.opensecretsanta.server.security.SystemClock
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.cors.routing.CORS
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
    shuffler: Shuffler = SecureShuffler(),
    mailer: Mailer? = null,
    pushSender: PushSender? = null,
    notificationExecutor: Executor? = null,
    notificationScheduler: Scheduler? = null,
) {
    install(Koin) {
        modules(serverModules(config, migrations, clock, shuffler, mailer, pushSender, notificationExecutor, notificationScheduler))
    }
    install(ContentNegotiation) { json() }
    if (config.corsAllowedOrigins.isNotEmpty()) install(CORS) { allowConfiguredOrigins(config.corsAllowedOrigins) }
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
            adminRoutes(get<ListManagedUsers>(), get<GetManagedUser>(), get<ResetPassword>())
            adminGroupRoutes(
                get<CreateGroup>(),
                get<ListOwnedGroups>(),
                get<GetOwnedGroup>(),
                get<UpdateGroupDetails>(),
                get<SetParticipants>(),
                get<DeleteGroup>(),
                get<NotificationDispatcher>(),
            )
            groupLifecycleRoutes(get<MoveToPending>(), get<ActivateGroup>(), get<ArchiveGroup>(), get<ListAssignments>(), get<SendDrawEmails>(), get<NotificationDispatcher>())
            memberGroupRoutes(get<ListMyGroups>(), get<GetMyGroup>())
            giftVisibilityRoutes(get<GetMyReceiver>(), get<GetMySanta>())
            questionRoutes(get<AskQuestion>(), get<ListMyQuestions>(), get<ListQuestionsFromMySanta>(), get<AnswerQuestion>(), get<NotificationDispatcher>())
            wishlistRoutes(get<GetMyWishlist>(), get<AddWishlistItem>(), get<UpdateWishlistItem>(), get<DeleteWishlistItem>(), get<ReorderWishlist>(), get<NotificationDispatcher>())
            receiverWishlistRoutes(get<GetReceiverWishlist>())
            notificationRoutes(get<GetNotificationSettings>(), get<UpdateNotificationSettings>(), get<RegisterDevice>(), get<UnregisterDevice>())
            adminOversightRoutes(get<GetParticipantWishlist>(), get<ListQuestionThreads>())
        }
    }
}

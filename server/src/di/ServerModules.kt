package com.usuallycoding.opensecretsanta.server.di

import com.usuallycoding.opensecretsanta.domain.admin.AccessPolicy
import com.usuallycoding.opensecretsanta.domain.admin.CreateMissingUsers
import com.usuallycoding.opensecretsanta.domain.admin.GetManagedUser
import com.usuallycoding.opensecretsanta.domain.admin.GroupMembershipRepository
import com.usuallycoding.opensecretsanta.domain.admin.ListManagedUsers
import com.usuallycoding.opensecretsanta.domain.admin.PromoteUser
import com.usuallycoding.opensecretsanta.domain.admin.ResetPassword
import com.usuallycoding.opensecretsanta.domain.admin.SeedDefaultAdmin
import com.usuallycoding.opensecretsanta.domain.auth.ChangePassword
import com.usuallycoding.opensecretsanta.domain.auth.Clock
import com.usuallycoding.opensecretsanta.domain.auth.Login
import com.usuallycoding.opensecretsanta.domain.auth.LoginAttemptTracker
import com.usuallycoding.opensecretsanta.domain.auth.PasswordHasher
import com.usuallycoding.opensecretsanta.domain.auth.SecretBox
import com.usuallycoding.opensecretsanta.domain.auth.TempPasswordGenerator
import com.usuallycoding.opensecretsanta.domain.auth.TokenService
import com.usuallycoding.opensecretsanta.domain.user.UpdateProfile
import com.usuallycoding.opensecretsanta.domain.user.UserRepository
import com.usuallycoding.opensecretsanta.server.config.ServerConfig
import com.usuallycoding.opensecretsanta.server.data.auth.SqliteLoginAttemptTracker
import com.usuallycoding.opensecretsanta.server.data.db.ClasspathMigrations
import com.usuallycoding.opensecretsanta.server.data.db.Database
import com.usuallycoding.opensecretsanta.server.data.db.Migration
import com.usuallycoding.opensecretsanta.server.data.db.MigrationRunner
import com.usuallycoding.opensecretsanta.server.data.group.SqliteGroupMembershipRepository
import com.usuallycoding.opensecretsanta.server.data.user.SqliteUserRepository
import com.usuallycoding.opensecretsanta.server.presentation.HealthService
import com.usuallycoding.opensecretsanta.server.security.AesGcmSecretBox
import com.usuallycoding.opensecretsanta.server.security.BcryptPasswordHasher
import com.usuallycoding.opensecretsanta.server.security.JwtTokenService
import com.usuallycoding.opensecretsanta.server.security.SecureTempPasswordGenerator
import com.usuallycoding.opensecretsanta.server.security.SystemClock
import org.koin.dsl.module

fun configModule(config: ServerConfig) = module {
    single { config }
}

fun clockModule(clock: Clock = SystemClock) = module {
    single<Clock> { clock }
}

fun databaseModule(migrations: List<Migration> = ClasspathMigrations.load()) = module {
    single { Database(get<ServerConfig>().dbPath) }
    single { MigrationRunner(get(), migrations) }
}

val securityModule = module {
    single<PasswordHasher> { BcryptPasswordHasher(get<ServerConfig>().passwordHashCost) }
    single<SecretBox> { AesGcmSecretBox(get<ServerConfig>().encryptionKeyBytes()) }
    single<TokenService> { JwtTokenService(get<ServerConfig>().jwtSecret, get()) }
    single<TempPasswordGenerator> { SecureTempPasswordGenerator() }
}

val repositoryModule = module {
    single<UserRepository> { SqliteUserRepository(get()) }
    single<GroupMembershipRepository> { SqliteGroupMembershipRepository(get()) }
    single<LoginAttemptTracker> { SqliteLoginAttemptTracker(get()) }
}

val useCaseModule = module {
    single { AccessPolicy(get()) }
    single { Login(get(), get(), get(), get(), get()) }
    single { ChangePassword(get(), get(), get()) }
    single { UpdateProfile(get()) }
    single { ListManagedUsers(get(), get(), get()) }
    single { GetManagedUser(get(), get(), get()) }
    single { ResetPassword(get(), get(), get(), get(), get(), get()) }
    single { PromoteUser(get()) }
    single { CreateMissingUsers(get(), get(), get(), get()) }
    single { SeedDefaultAdmin(get(), get()) }
}

val presentationModule = module {
    single { HealthService(get()) }
}

fun serverModules(
    config: ServerConfig,
    migrations: List<Migration> = ClasspathMigrations.load(),
    clock: Clock = SystemClock,
) = listOf(
    configModule(config),
    clockModule(clock),
    databaseModule(migrations),
    securityModule,
    repositoryModule,
    useCaseModule,
    presentationModule,
)

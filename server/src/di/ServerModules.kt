package com.usuallycoding.opensecretsanta.server.di

import com.usuallycoding.opensecretsanta.domain.admin.AccessPolicy
import com.usuallycoding.opensecretsanta.domain.admin.CreateMissingUsers
import com.usuallycoding.opensecretsanta.domain.admin.GetManagedUser
import com.usuallycoding.opensecretsanta.domain.admin.GroupMembershipRepository
import com.usuallycoding.opensecretsanta.domain.admin.ListManagedUsers
import com.usuallycoding.opensecretsanta.domain.admin.NewAccountPlanner
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
import com.usuallycoding.opensecretsanta.domain.gift.AnswerQuestion
import com.usuallycoding.opensecretsanta.domain.gift.AskQuestion
import com.usuallycoding.opensecretsanta.domain.gift.GetMyReceiver
import com.usuallycoding.opensecretsanta.domain.gift.GetMySanta
import com.usuallycoding.opensecretsanta.domain.gift.ListMyQuestions
import com.usuallycoding.opensecretsanta.domain.gift.ListQuestionThreads
import com.usuallycoding.opensecretsanta.domain.gift.ListQuestionsFromMySanta
import com.usuallycoding.opensecretsanta.domain.gift.QuestionRepository
import com.usuallycoding.opensecretsanta.domain.group.ActivateGroup
import com.usuallycoding.opensecretsanta.domain.group.ArchiveGroup
import com.usuallycoding.opensecretsanta.domain.group.AssignmentPlanner
import com.usuallycoding.opensecretsanta.domain.group.CreateGroup
import com.usuallycoding.opensecretsanta.domain.group.DeleteDraftGroup
import com.usuallycoding.opensecretsanta.domain.group.GetMyGroup
import com.usuallycoding.opensecretsanta.domain.group.GetOwnedGroup
import com.usuallycoding.opensecretsanta.domain.group.GroupRepository
import com.usuallycoding.opensecretsanta.domain.group.GroupViewer
import com.usuallycoding.opensecretsanta.domain.group.ListAssignments
import com.usuallycoding.opensecretsanta.domain.group.ListMyGroups
import com.usuallycoding.opensecretsanta.domain.group.ListOwnedGroups
import com.usuallycoding.opensecretsanta.domain.group.MoveToPending
import com.usuallycoding.opensecretsanta.domain.group.OwnedGroupLoader
import com.usuallycoding.opensecretsanta.domain.group.ParticipantGroupLoader
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
import com.usuallycoding.opensecretsanta.domain.wishlist.WishlistRepository
import com.usuallycoding.opensecretsanta.domain.user.UserRepository
import com.usuallycoding.opensecretsanta.server.config.ServerConfig
import com.usuallycoding.opensecretsanta.server.data.auth.SqliteLoginAttemptTracker
import com.usuallycoding.opensecretsanta.server.data.db.ClasspathMigrations
import com.usuallycoding.opensecretsanta.server.data.db.Database
import com.usuallycoding.opensecretsanta.server.data.db.Migration
import com.usuallycoding.opensecretsanta.server.data.db.MigrationRunner
import com.usuallycoding.opensecretsanta.server.data.group.SqliteGroupMembershipRepository
import com.usuallycoding.opensecretsanta.server.data.group.SqliteGroupRepository
import com.usuallycoding.opensecretsanta.server.data.group.SqliteQuestionRepository
import com.usuallycoding.opensecretsanta.server.data.group.SqliteWishlistRepository
import com.usuallycoding.opensecretsanta.server.data.user.SqliteUserRepository
import com.usuallycoding.opensecretsanta.server.presentation.HealthService
import com.usuallycoding.opensecretsanta.server.security.AesGcmSecretBox
import com.usuallycoding.opensecretsanta.server.security.BcryptPasswordHasher
import com.usuallycoding.opensecretsanta.server.security.JwtTokenService
import com.usuallycoding.opensecretsanta.server.security.SecureShuffler
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

fun shufflerModule(shuffler: Shuffler = SecureShuffler()) = module {
    single<Shuffler> { shuffler }
}

val repositoryModule = module {
    single<UserRepository> { SqliteUserRepository(get()) }
    single<GroupMembershipRepository> { SqliteGroupMembershipRepository(get()) }
    single<LoginAttemptTracker> { SqliteLoginAttemptTracker(get()) }
    single<GroupRepository> { SqliteGroupRepository(get()) }
    single<QuestionRepository> { SqliteQuestionRepository(get()) }
    single<WishlistRepository> { SqliteWishlistRepository(get()) }
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
    single { NewAccountPlanner(get(), get(), get()) }
    single { CreateMissingUsers(get(), get()) }
    single { SeedDefaultAdmin(get(), get()) }

    single { OwnedGroupLoader(get()) }
    single { GroupViewer(get()) }
    single { AssignmentPlanner(get()) }
    single { CreateGroup(get(), get()) }
    single { UpdateGroupDetails(get(), get(), get()) }
    single { DeleteDraftGroup(get(), get()) }
    single { ListOwnedGroups(get(), get()) }
    single { GetOwnedGroup(get(), get()) }
    single { SetParticipants(get(), get(), get(), get(), get()) }
    single { MoveToPending(get(), get(), get(), get(), get()) }
    single { ActivateGroup(get(), get(), get(), get()) }
    single { ArchiveGroup(get(), get(), get()) }
    single { ListAssignments(get(), get()) }
    single { ListMyGroups(get()) }
    single { GetMyGroup(get()) }

    single { ParticipantGroupLoader(get()) }
    single { GetMyReceiver(get(), get(), get()) }
    single { GetMySanta(get(), get(), get()) }
    single { AskQuestion(get(), get(), get()) }
    single { ListMyQuestions(get(), get()) }
    single { ListQuestionsFromMySanta(get(), get()) }
    single { AnswerQuestion(get(), get(), get()) }

    single { GetMyWishlist(get(), get()) }
    single { AddWishlistItem(get(), get(), get()) }
    single { UpdateWishlistItem(get(), get(), get()) }
    single { DeleteWishlistItem(get(), get()) }
    single { ReorderWishlist(get(), get()) }
    single { GetReceiverWishlist(get(), get(), get()) }

    single { GetParticipantWishlist(get(), get()) }
    single { ListQuestionThreads(get(), get(), get()) }
}

val presentationModule = module {
    single { HealthService(get()) }
}

fun serverModules(
    config: ServerConfig,
    migrations: List<Migration> = ClasspathMigrations.load(),
    clock: Clock = SystemClock,
    shuffler: Shuffler = SecureShuffler(),
) = listOf(
    configModule(config),
    clockModule(clock),
    databaseModule(migrations),
    securityModule,
    shufflerModule(shuffler),
    repositoryModule,
    useCaseModule,
    presentationModule,
)

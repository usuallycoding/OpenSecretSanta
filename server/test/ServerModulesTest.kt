package com.usuallycoding.opensecretsanta.server

import com.usuallycoding.opensecretsanta.server.di.serverModules
import com.usuallycoding.opensecretsanta.domain.gift.AnswerQuestion
import com.usuallycoding.opensecretsanta.domain.gift.AskQuestion
import com.usuallycoding.opensecretsanta.domain.gift.GetMyReceiver
import com.usuallycoding.opensecretsanta.domain.gift.GetMySanta
import com.usuallycoding.opensecretsanta.domain.gift.ListMyQuestions
import com.usuallycoding.opensecretsanta.domain.gift.ListQuestionsFromMySanta
import com.usuallycoding.opensecretsanta.domain.gift.QuestionRepository
import com.usuallycoding.opensecretsanta.domain.group.ActivateGroup
import com.usuallycoding.opensecretsanta.domain.group.ArchiveGroup
import com.usuallycoding.opensecretsanta.domain.group.AssignmentPlanner
import com.usuallycoding.opensecretsanta.domain.group.CreateGroup
import com.usuallycoding.opensecretsanta.domain.group.DeleteGroup
import com.usuallycoding.opensecretsanta.domain.group.GetMyGroup
import com.usuallycoding.opensecretsanta.domain.group.GetOwnedGroup
import com.usuallycoding.opensecretsanta.domain.group.GroupRepository
import com.usuallycoding.opensecretsanta.domain.group.ListAssignments
import com.usuallycoding.opensecretsanta.domain.group.ListMyGroups
import com.usuallycoding.opensecretsanta.domain.group.ListOwnedGroups
import com.usuallycoding.opensecretsanta.domain.group.MoveToPending
import com.usuallycoding.opensecretsanta.domain.group.ParticipantGroupLoader
import com.usuallycoding.opensecretsanta.domain.group.SetParticipants
import com.usuallycoding.opensecretsanta.domain.group.Shuffler
import com.usuallycoding.opensecretsanta.domain.group.UpdateGroupDetails
import com.usuallycoding.opensecretsanta.domain.wishlist.AddWishlistItem
import com.usuallycoding.opensecretsanta.domain.wishlist.DeleteWishlistItem
import com.usuallycoding.opensecretsanta.domain.wishlist.GetMyWishlist
import com.usuallycoding.opensecretsanta.domain.wishlist.GetReceiverWishlist
import com.usuallycoding.opensecretsanta.domain.wishlist.ReorderWishlist
import com.usuallycoding.opensecretsanta.domain.wishlist.UpdateWishlistItem
import com.usuallycoding.opensecretsanta.domain.wishlist.WishlistRepository
import com.usuallycoding.opensecretsanta.domain.notification.DisabledPushSender
import com.usuallycoding.opensecretsanta.domain.notification.PushSender
import com.usuallycoding.opensecretsanta.server.config.FcmCredentials
import com.usuallycoding.opensecretsanta.server.data.push.FcmPushSender
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.koin.test.verify.verify
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ServerModulesTest {

    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun moduleGraphIsComplete() {
        val config = testConfig()
        module { includes(serverModules(config, migrations = emptyList())) }.verify()
    }

    @Test
    fun everyGroupComponentCanBeResolvedFromAStartedContainer() {
        val app = koinApplication { modules(serverModules(testConfig(), migrations = emptyList())) }
        try {
            val koin = app.koin
            val resolved = listOf(
                koin.get<GroupRepository>(), koin.get<Shuffler>(), koin.get<AssignmentPlanner>(),
                koin.get<CreateGroup>(), koin.get<UpdateGroupDetails>(), koin.get<DeleteGroup>(),
                koin.get<ListOwnedGroups>(), koin.get<GetOwnedGroup>(), koin.get<SetParticipants>(),
                koin.get<MoveToPending>(), koin.get<ActivateGroup>(), koin.get<ArchiveGroup>(),
                koin.get<ListAssignments>(), koin.get<ListMyGroups>(), koin.get<GetMyGroup>(),
                koin.get<ParticipantGroupLoader>(), koin.get<QuestionRepository>(), koin.get<GetMyReceiver>(),
                koin.get<GetMySanta>(), koin.get<AskQuestion>(), koin.get<ListMyQuestions>(),
                koin.get<ListQuestionsFromMySanta>(), koin.get<AnswerQuestion>(),
                koin.get<WishlistRepository>(), koin.get<GetMyWishlist>(), koin.get<AddWishlistItem>(),
                koin.get<UpdateWishlistItem>(), koin.get<DeleteWishlistItem>(), koin.get<ReorderWishlist>(),
                koin.get<GetReceiverWishlist>(),
            )

            assertEquals(30, resolved.size)
            assertTrue(resolved.all { it != null })
        } finally {
            app.close()
        }
    }
    @Test
    fun theSenderIsFcmOnlyWithAServiceAccountKey() {
        val without = koinApplication { modules(serverModules(testConfig(), migrations = emptyList())) }
        val with = koinApplication {
            modules(serverModules(testConfig().copy(fcm = FcmCredentials.load(writeServiceAccountKey().first.toString())), migrations = emptyList()))
        }
        try {
            assertEquals(DisabledPushSender, without.koin.get<PushSender>())
            assertTrue(with.koin.get<PushSender>() is FcmPushSender)
        } finally {
            without.close()
            with.close()
        }
    }

}

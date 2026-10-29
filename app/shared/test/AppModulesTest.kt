package com.usuallycoding.opensecretsanta

import com.usuallycoding.opensecretsanta.data.BackendConfig
import com.usuallycoding.opensecretsanta.di.initKoin
import com.usuallycoding.opensecretsanta.ui.admin.AdminGroupFormViewModel
import com.usuallycoding.opensecretsanta.ui.admin.AdminGroupViewModel
import com.usuallycoding.opensecretsanta.ui.admin.AdminPairThreadViewModel
import com.usuallycoding.opensecretsanta.ui.admin.AdminParticipantWishlistViewModel
import com.usuallycoding.opensecretsanta.ui.admin.AdminGroupsViewModel
import com.usuallycoding.opensecretsanta.ui.admin.AdminUserViewModel
import com.usuallycoding.opensecretsanta.ui.admin.AdminUsersViewModel
import com.usuallycoding.opensecretsanta.ui.gift.ReceiverViewModel
import com.usuallycoding.opensecretsanta.ui.gift.SantaQuestionsViewModel
import com.usuallycoding.opensecretsanta.ui.gift.SantaRevealViewModel
import com.usuallycoding.opensecretsanta.ui.notifications.NotificationsViewModel
import com.usuallycoding.opensecretsanta.ui.wishlist.ReceiverWishlistViewModel
import com.usuallycoding.opensecretsanta.ui.wishlist.WishlistViewModel
import org.koin.core.parameter.parametersOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.koin.core.context.stopKoin
import org.koin.mp.KoinPlatform
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame

class AppModulesTest {

    @AfterTest
    fun tearDown() = stopKoin()

    @Test
    fun backendUrlSuppliedByHostIsResolvable() {
        initKoin(baseUrl = "http://example.test:8080")

        val config = KoinPlatform.getKoin().get<BackendConfig>()

        assertEquals("http://example.test:8080", config.baseUrl)
    }

    @Test
    fun secondInitIsIgnoredAndDoesNotFail() {
        assertNull(KoinPlatform.getKoinOrNull())
        initKoin(baseUrl = "http://first.test")
        val firstKoin = KoinPlatform.getKoin()

        initKoin(baseUrl = "http://second.test")

        assertNotNull(KoinPlatform.getKoinOrNull())
        assertSame(firstKoin, KoinPlatform.getKoin())
        assertEquals("http://first.test", firstKoin.get<BackendConfig>().baseUrl)
    }

    // View models run on the main dispatcher, which a plain JVM test does not have.
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun theGroupViewModelsAreResolvableForAGroup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        try {
            initKoin(baseUrl = "http://example.test:8080")
            val koin = KoinPlatform.getKoin()

            assertEquals(7L, koin.get<WishlistViewModel> { parametersOf(7L) }.groupId)
            assertEquals(7L, koin.get<ReceiverWishlistViewModel> { parametersOf(7L) }.groupId)
            assertEquals(7L, koin.get<ReceiverViewModel> { parametersOf(7L) }.groupId)
            assertEquals(7L, koin.get<SantaQuestionsViewModel> { parametersOf(7L) }.groupId)
            assertEquals(7L, koin.get<SantaRevealViewModel> { parametersOf(7L) }.groupId)
            assertEquals(7L, koin.get<AdminGroupViewModel> { parametersOf(7L, "admin") }.groupId)
            assertEquals("bob", koin.get<AdminParticipantWishlistViewModel> { parametersOf(7L, "bob") }.username)
            assertEquals("carol", koin.get<AdminPairThreadViewModel> { parametersOf(7L, "bob", "carol") }.receiver)
            assertEquals(7L, koin.get<AdminGroupFormViewModel> { parametersOf(7L) }.groupId)
            assertNull(koin.get<AdminGroupFormViewModel> { parametersOf(AdminGroupFormViewModel.NEW_GROUP) }.groupId)
            assertNotNull(koin.get<AdminGroupsViewModel>())
            assertNotNull(koin.get<AdminUsersViewModel>())
            assertNotNull(koin.get<NotificationsViewModel>())
            assertEquals("alice", koin.get<AdminUserViewModel> { parametersOf("alice", "carol") }.username)
        } finally {
            Dispatchers.resetMain()
        }
    }
    @Test
    fun theHostsPushDeviceIsTheOneTheRegistrationUsesAndSignOutHasItsHook() {
        initKoin(baseUrl = "http://example.test:8080", pushDevice = com.usuallycoding.opensecretsanta.data.push.UnsupportedPushDevice)
        val koin = KoinPlatform.getKoin()

        assertSame(com.usuallycoding.opensecretsanta.data.push.UnsupportedPushDevice, koin.get<com.usuallycoding.opensecretsanta.data.push.PushDevice>())
        assertEquals(false, koin.get<com.usuallycoding.opensecretsanta.data.push.PushRegistration>().isSupported)
        assertNotNull(koin.get<com.usuallycoding.opensecretsanta.data.session.SessionManager>())
    }

}

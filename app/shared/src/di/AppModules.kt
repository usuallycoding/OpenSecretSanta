package com.usuallycoding.opensecretsanta.di

import com.usuallycoding.opensecretsanta.data.BackendConfig
import com.usuallycoding.opensecretsanta.data.api.ApiClient
import com.usuallycoding.opensecretsanta.data.api.AuthContext
import com.usuallycoding.opensecretsanta.data.api.createHttpClient
import com.usuallycoding.opensecretsanta.data.session.MemoryTokenStore
import com.usuallycoding.opensecretsanta.data.session.SessionManager
import com.usuallycoding.opensecretsanta.data.session.TokenStore
import com.usuallycoding.opensecretsanta.ui.groups.GroupDetailViewModel
import com.usuallycoding.opensecretsanta.ui.groups.GroupsViewModel
import com.usuallycoding.opensecretsanta.ui.login.LoginViewModel
import com.usuallycoding.opensecretsanta.ui.password.ChangePasswordViewModel
import com.usuallycoding.opensecretsanta.ui.profile.ProfileViewModel
import com.usuallycoding.opensecretsanta.ui.wishlist.ReceiverWishlistViewModel
import com.usuallycoding.opensecretsanta.ui.wishlist.WishlistViewModel
import com.usuallycoding.opensecretsanta.ui.shell.ShellViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.mp.KoinPlatform

fun appModules(baseUrl: String): List<Module> = listOf(
    module {
        single { BackendConfig(baseUrl) }
        single { createHttpClient() }
        // The session and the client need each other (one holds the token, the other sends it), so the
        // session only looks the client up when it first needs it.
        single { SessionManager(api = lazy { get<ApiClient>() }, store = get()) } bind AuthContext::class
        single { ApiClient(http = get(), baseUrl = get<BackendConfig>().baseUrl, auth = get()) }

        // Each view model owns its scope, which the lifecycle library cancels when the view model is cleared.
        viewModel { LoginViewModel(get(), viewModelScope()) }
        viewModel { ChangePasswordViewModel(get(), viewModelScope()) }
        viewModel { (account: com.usuallycoding.opensecretsanta.data.session.Account) -> ShellViewModel(account, get(), viewModelScope()) }
        viewModel { GroupsViewModel(get(), viewModelScope()) }
        viewModel { ProfileViewModel(get(), viewModelScope()) }
        viewModel { (groupId: Long) -> WishlistViewModel(groupId, get(), viewModelScope()) }
        viewModel { (groupId: Long) -> ReceiverWishlistViewModel(groupId, get(), viewModelScope()) }
        viewModel { (groupId: Long, me: String) -> GroupDetailViewModel(groupId, me, get(), viewModelScope()) }
    },
)

private fun viewModelScope() = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

/** What a platform host supplies: where a sign-in is kept between runs. Memory only, unless a host says otherwise. */
fun tokenStoreModule(store: () -> TokenStore): Module = module { single<TokenStore> { store() } }

/**
 * Starts the shared dependency graph. Hosts call this once at launch, before the first screen
 * composes; repeated calls (for example after an Android configuration change) are ignored.
 */
fun initKoin(baseUrl: String, platformModule: Module = tokenStoreModule { MemoryTokenStore() }) {
    if (KoinPlatform.getKoinOrNull() != null) return
    startKoin {
        modules(appModules(baseUrl) + platformModule)
    }
}

package com.usuallycoding.opensecretsanta.ui.groups

import androidx.lifecycle.ViewModel
import com.usuallycoding.opensecretsanta.contract.AccountDto
import com.usuallycoding.opensecretsanta.data.api.ApiClient
import com.usuallycoding.opensecretsanta.data.api.map
import com.usuallycoding.opensecretsanta.ui.gift.ReceiverInfo
import com.usuallycoding.opensecretsanta.ui.gift.toInfo
import com.usuallycoding.opensecretsanta.ui.load.LoadState
import com.usuallycoding.opensecretsanta.ui.load.Loader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow

/**
 * The receiver the person gives to, for the first pair row of an Active or Archived group, and what the receiver's
 * details sheet shows. It is created only once the names are drawn, because the server has no receiver before that.
 */
class PairViewModel(
    val groupId: Long,
    api: ApiClient,
    scope: CoroutineScope,
) : ViewModel(scope) {
    private val loader = Loader(scope) { api.receiver(groupId).map { it.toInfo() } }

    val receiver: StateFlow<LoadState<ReceiverInfo>> = loader.state

    fun refresh(): Job = loader.refresh()
}

/** The signed-in person's own details, as their sheet shows them: whatever they have set, blank values left out. */
data class MyDetails(
    val username: String,
    val fullName: String?,
    val email: String?,
    val phone: String?,
    val address: String?,
    val deliveryInfo: String?,
) {
    val hasProfile: Boolean get() = listOf(fullName, email, phone, address, deliveryInfo).any { it != null }
}

private fun String?.clean(): String? = this?.trim()?.ifEmpty { null }

internal fun AccountDto.toDetails() = MyDetails(username, fullName.clean(), email.clean(), phone.clean(), address.clean(), deliveryInfo.clean())

/** Loads the person's own details when their sheet is opened. */
class MyDetailsViewModel(
    api: ApiClient,
    scope: CoroutineScope,
) : ViewModel(scope) {
    private val loader = Loader(scope) { api.account().map { it.toDetails() } }

    val details: StateFlow<LoadState<MyDetails>> = loader.state

    fun refresh(): Job = loader.refresh()
}

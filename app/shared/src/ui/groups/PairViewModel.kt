package com.usuallycoding.opensecretsanta.ui.groups

import androidx.lifecycle.ViewModel
import com.usuallycoding.opensecretsanta.contract.DeliveryInfoDto
import com.usuallycoding.opensecretsanta.data.api.ApiClient
import com.usuallycoding.opensecretsanta.data.api.map
import com.usuallycoding.opensecretsanta.domain.delivery.DeliveryInfo
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

    /** The full name, email, phone and address are all set: the one rule of `DeliveryInfo`, so the app has a single definition. */
    val isComplete: Boolean get() = DeliveryInfo(fullName, email, phone, address, deliveryInfo).isComplete
}

/**
 * Whether the "Your stuff:" card asks the person to fill in their delivery info: only while they can still edit it (a Pending or an
 * Active group), once it has loaded, and while it lacks a required field. Loading, a failed load and a failed reload say nothing, as
 * what is known is then not reliable.
 */
fun showsDeliveryNudge(phase: GroupPhase?, details: LoadState<MyDetails>?): Boolean {
    if (phase != GroupPhase.Pending && phase != GroupPhase.Active) return false
    if (details == null || details.error != null) return false
    val loaded = details.value ?: return false
    return !loaded.isComplete
}

private fun String?.clean(): String? = this?.trim()?.ifEmpty { null }

internal fun DeliveryInfoDto.toDetails(username: String) = MyDetails(username, fullName.clean(), email.clean(), phone.clean(), address.clean(), deliveryInfo.clean())

/** Loads the person's own delivery info for the group when their sheet is opened. */
class MyDetailsViewModel(
    groupId: Long,
    me: String,
    api: ApiClient,
    scope: CoroutineScope,
) : ViewModel(scope) {
    private val loader = Loader(scope) { api.deliveryInfo(groupId).map { it.toDetails(me) } }

    val details: StateFlow<LoadState<MyDetails>> = loader.state

    fun refresh(): Job = loader.refresh()
}

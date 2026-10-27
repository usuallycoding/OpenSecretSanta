package com.usuallycoding.opensecretsanta.server.presentation

import com.usuallycoding.opensecretsanta.contract.ApiRoutes
import com.usuallycoding.opensecretsanta.contract.ReceiverDto
import com.usuallycoding.opensecretsanta.contract.SantaRevealDto
import com.usuallycoding.opensecretsanta.domain.gift.GetMyReceiver
import com.usuallycoding.opensecretsanta.domain.gift.GetMySanta
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

/**
 * What a participant may learn about the exchange. Both routes answer for the caller only: the
 * receiver is "my receiver" and the Santa is "my Santa", never looked up by name.
 */
fun Route.giftVisibilityRoutes(getReceiver: GetMyReceiver, getSanta: GetMySanta) {
    get(ApiRoutes.GROUP_RECEIVER) {
        val user = call.currentUser()
        val details = blocking { getReceiver(user, call.groupId()) }
        call.respond(ReceiverDto(details.username.value, details.fullName, details.address, details.phone, details.deliveryInfo))
    }
    get(ApiRoutes.GROUP_SANTA) {
        val user = call.currentUser()
        val reveal = blocking { getSanta(user, call.groupId()) }
        call.respond(SantaRevealDto(reveal.username.value, reveal.fullName))
    }
}

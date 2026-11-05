package com.usuallycoding.opensecretsanta.server.presentation

import com.usuallycoding.opensecretsanta.contract.ApiRoutes
import com.usuallycoding.opensecretsanta.contract.DeliveryInfoDto
import com.usuallycoding.opensecretsanta.domain.delivery.DeliveryInfo
import com.usuallycoding.opensecretsanta.domain.delivery.GetMyDeliveryInfo
import com.usuallycoding.opensecretsanta.domain.delivery.UpdateMyDeliveryInfo
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.put

private fun DeliveryInfo.toDto() = DeliveryInfoDto(fullName, email, phone, address, deliveryInfo)

/** A participant's own delivery info for one group: read it, and replace it while the group is Pending or Active. */
fun Route.deliveryInfoRoutes(getInfo: GetMyDeliveryInfo, updateInfo: UpdateMyDeliveryInfo) {
    get(ApiRoutes.GROUP_DELIVERY_INFO) {
        val user = call.currentUser()
        call.respond(blocking { getInfo(user, call.groupId()) }.toDto())
    }
    put(ApiRoutes.GROUP_DELIVERY_INFO) {
        val user = call.currentUser()
        val groupId = call.groupId()
        val request = call.receive<DeliveryInfoDto>()
        val info = DeliveryInfo.of(request.fullName, request.email, request.phone, request.address, request.deliveryInfo)
        call.respond(blocking { updateInfo(user, groupId, info) }.toDto())
    }
}

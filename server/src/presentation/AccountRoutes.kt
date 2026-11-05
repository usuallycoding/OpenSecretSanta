package com.usuallycoding.opensecretsanta.server.presentation

import com.usuallycoding.opensecretsanta.contract.ApiRoutes
import com.usuallycoding.opensecretsanta.contract.ChangePasswordRequest
import com.usuallycoding.opensecretsanta.contract.ChangePasswordResponse
import com.usuallycoding.opensecretsanta.domain.announcement.Channels
import com.usuallycoding.opensecretsanta.domain.auth.ChangePassword
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.put

/** Reading the account and changing the password: the routes open while the password is temporary. */
fun Route.accountRoutes(changePassword: ChangePassword, channels: Channels) {
    get(ApiRoutes.ME) {
        call.respond(call.currentUser().toDto(channels))
    }
    put(ApiRoutes.ME_PASSWORD) {
        val user = call.currentUser()
        val request = call.receive<ChangePasswordRequest>()
        val result = blocking { changePassword(user.username, request.currentPassword, request.newPassword) }
        call.respond(ChangePasswordResponse(result.token))
    }
}

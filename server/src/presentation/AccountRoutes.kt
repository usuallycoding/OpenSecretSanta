package com.usuallycoding.opensecretsanta.server.presentation

import com.usuallycoding.opensecretsanta.contract.ApiRoutes
import com.usuallycoding.opensecretsanta.contract.ChangePasswordRequest
import com.usuallycoding.opensecretsanta.contract.ChangePasswordResponse
import com.usuallycoding.opensecretsanta.contract.ProfileUpdateRequest
import com.usuallycoding.opensecretsanta.domain.auth.ChangePassword
import com.usuallycoding.opensecretsanta.domain.user.Profile
import com.usuallycoding.opensecretsanta.domain.user.UpdateProfile
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.put

/** Reading the account and changing the password: the routes open while the password is temporary. */
fun Route.accountRoutes(changePassword: ChangePassword) {
    get(ApiRoutes.ME) {
        call.respond(call.currentUser().toDto())
    }
    put(ApiRoutes.ME_PASSWORD) {
        val user = call.currentUser()
        val request = call.receive<ChangePasswordRequest>()
        val result = blocking { changePassword(user.username, request.currentPassword, request.newPassword) }
        call.respond(ChangePasswordResponse(result.token))
    }
}

/** Editing the profile, which needs the temporary password to be replaced first. */
fun Route.profileRoutes(updateProfile: UpdateProfile) {
    put(ApiRoutes.ME) {
        val user = call.currentUser()
        val request = call.receive<ProfileUpdateRequest>()
        val profile = Profile.of(request.fullName, request.email, request.phone, request.address, request.deliveryInfo)
        val updated = blocking { updateProfile(user.username, request.username, profile) }
        call.respond(updated.toDto())
    }
}

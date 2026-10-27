package com.usuallycoding.opensecretsanta.server.presentation

import com.usuallycoding.opensecretsanta.contract.ApiRoutes
import com.usuallycoding.opensecretsanta.contract.ResetPasswordResponse
import com.usuallycoding.opensecretsanta.domain.admin.GetManagedUser
import com.usuallycoding.opensecretsanta.domain.admin.ListManagedUsers
import com.usuallycoding.opensecretsanta.domain.admin.PromoteUser
import com.usuallycoding.opensecretsanta.domain.admin.ResetPassword
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post

private fun ApplicationCall.targetUsername(): String = parameters["username"].orEmpty()

/** Admin-only routes; the use cases themselves refuse non-admins and out-of-scope targets. */
fun Route.adminRoutes(
    listUsers: ListManagedUsers,
    getUser: GetManagedUser,
    resetPassword: ResetPassword,
    promoteUser: PromoteUser,
) {
    get(ApiRoutes.ADMIN_USERS) {
        val actor = call.currentUser()
        call.respond(blocking { listUsers(actor) }.map { it.toDto() })
    }
    get(ApiRoutes.ADMIN_USER) {
        val actor = call.currentUser()
        call.respond(blocking { getUser(actor, call.targetUsername()) }.toDto())
    }
    post(ApiRoutes.ADMIN_USER_RESET_PASSWORD) {
        val actor = call.currentUser()
        val credentials = blocking { resetPassword(actor, call.targetUsername()) }
        call.respond(ResetPasswordResponse(credentials.username.value, credentials.temporaryPassword))
    }
    post(ApiRoutes.ADMIN_USER_PROMOTE) {
        val actor = call.currentUser()
        call.respond(blocking { promoteUser(actor, call.targetUsername()) }.toDto())
    }
}

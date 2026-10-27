package com.usuallycoding.opensecretsanta.domain.user

import com.usuallycoding.opensecretsanta.domain.error.DomainException

/** Replaces the editable fields of the caller's own account. The username can never change. */
class UpdateProfile(private val users: UserRepository) {

    operator fun invoke(actor: Username, requestedUsername: String?, profile: Profile): User {
        if (requestedUsername != null && Username.parseOrNull(requestedUsername) != actor) {
            throw DomainException.ValidationFailed("username", "The username cannot be changed.")
        }
        return users.updateProfile(actor, profile) ?: throw DomainException.NotFound()
    }
}

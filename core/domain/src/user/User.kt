package com.usuallycoding.opensecretsanta.domain.user

/** An account as the rest of the system sees it; credentials are kept out of this type. */
data class User(
    val username: Username,
    val profile: Profile,
    val isAdmin: Boolean,
    val mustChangePassword: Boolean,
    val tokenVersion: Int,
)

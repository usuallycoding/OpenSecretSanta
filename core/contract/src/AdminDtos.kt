package com.usuallycoding.opensecretsanta.contract

import kotlinx.serialization.Serializable

@Serializable
data class ManagedUserDto(
    val user: AccountDto,
    /** Present only while the user's password is still the temporary one. */
    val temporaryPassword: String? = null,
    /** True when the caller may delete this user: an account they created that is in no group. */
    val canDelete: Boolean = false,
)

@Serializable
data class ResetPasswordResponse(
    val username: String,
    val temporaryPassword: String,
)

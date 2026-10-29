package com.usuallycoding.opensecretsanta.contract

import kotlinx.serialization.Serializable

@Serializable
data class AccountDto(
    val username: String,
    val fullName: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val address: String? = null,
    val deliveryInfo: String? = null,
    val isAdmin: Boolean,
    val mustChangePassword: Boolean,
    /** Whether the server can send email, and whether it can send push notifications; false when it is not set up. */
    val emailAvailable: Boolean = false,
    val pushAvailable: Boolean = false,
)

/** Replaces all editable fields. [username] may be sent back unchanged but must never differ. */
@Serializable
data class ProfileUpdateRequest(
    val username: String? = null,
    val fullName: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val address: String? = null,
    val deliveryInfo: String? = null,
)

package com.usuallycoding.opensecretsanta.contract

import kotlinx.serialization.Serializable

@Serializable
data class AccountDto(
    val username: String,
    val isAdmin: Boolean,
    val mustChangePassword: Boolean,
    /** Whether the server can send email, and whether it can send push notifications; false when it is not set up. */
    val emailAvailable: Boolean = false,
    val pushAvailable: Boolean = false,
)

/** What a participant told one group about where and how a gift can reach them; every field is optional. */
@Serializable
data class DeliveryInfoDto(
    val fullName: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val address: String? = null,
    val deliveryInfo: String? = null,
)

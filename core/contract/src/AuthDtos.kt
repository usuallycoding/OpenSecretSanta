package com.usuallycoding.opensecretsanta.contract

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val username: String,
    val password: String,
)

@Serializable
data class LoginResponse(
    val token: String,
    val mustChangePassword: Boolean,
)

@Serializable
data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String,
)

/** A fresh token: the password change invalidated all earlier ones. */
@Serializable
data class ChangePasswordResponse(
    val token: String,
)

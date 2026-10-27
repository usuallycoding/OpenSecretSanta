package com.usuallycoding.opensecretsanta.server.presentation

import com.usuallycoding.opensecretsanta.contract.AccountDto
import com.usuallycoding.opensecretsanta.contract.ManagedUserDto
import com.usuallycoding.opensecretsanta.domain.admin.ManagedUser
import com.usuallycoding.opensecretsanta.domain.user.User

fun User.toDto() = AccountDto(
    username = username.value,
    fullName = profile.fullName,
    email = profile.email,
    phone = profile.phone,
    address = profile.address,
    deliveryInfo = profile.deliveryInfo,
    isAdmin = isAdmin,
    mustChangePassword = mustChangePassword,
)

fun ManagedUser.toDto() = ManagedUserDto(user = user.toDto(), temporaryPassword = temporaryPassword)

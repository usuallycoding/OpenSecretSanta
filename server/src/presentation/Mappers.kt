package com.usuallycoding.opensecretsanta.server.presentation

import com.usuallycoding.opensecretsanta.domain.announcement.Channels
import com.usuallycoding.opensecretsanta.contract.AccountDto
import com.usuallycoding.opensecretsanta.contract.ManagedUserDto
import com.usuallycoding.opensecretsanta.domain.admin.ManagedUser
import com.usuallycoding.opensecretsanta.domain.user.User

/** [channels] says what the server can send; the places that show someone else's account leave it out. */
fun User.toDto(channels: Channels? = null) = AccountDto(
    username = username.value,
    fullName = profile.fullName,
    email = profile.email,
    phone = profile.phone,
    address = profile.address,
    deliveryInfo = profile.deliveryInfo,
    isAdmin = isAdmin,
    mustChangePassword = mustChangePassword,
    emailAvailable = channels?.emailAvailable ?: false,
    pushAvailable = channels?.pushAvailable ?: false,
)

fun ManagedUser.toDto() = ManagedUserDto(user = user.toDto(), temporaryPassword = temporaryPassword, canDelete = canDelete)

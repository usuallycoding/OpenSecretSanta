package com.usuallycoding.opensecretsanta.domain.user

/** Shape of generated temporary passwords: easy to read out or paste into a chat message. */
object TempPassword {
    const val LENGTH = 12

    /** Letters and digits without the look-alikes 0, O, 1, l and I. */
    const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789"
}

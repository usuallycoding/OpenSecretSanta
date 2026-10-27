package com.usuallycoding.opensecretsanta.server.security

import com.usuallycoding.opensecretsanta.domain.group.Shuffler
import java.security.SecureRandom
import java.util.Collections

/** Shuffles with a cryptographically secure source, so a draw cannot be predicted from the input order. */
class SecureShuffler(private val random: SecureRandom = SecureRandom()) : Shuffler {
    override fun <T> shuffled(items: List<T>): List<T> = items.toMutableList().also { Collections.shuffle(it, random) }
}

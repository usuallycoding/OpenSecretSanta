package com.usuallycoding.opensecretsanta.server.security

import com.usuallycoding.opensecretsanta.domain.auth.TempPasswordGenerator
import com.usuallycoding.opensecretsanta.domain.user.TempPassword
import java.security.SecureRandom

class SecureTempPasswordGenerator(private val random: SecureRandom = SecureRandom()) : TempPasswordGenerator {
    override fun generate(): String = buildString(TempPassword.LENGTH) {
        repeat(TempPassword.LENGTH) { append(TempPassword.ALPHABET[random.nextInt(TempPassword.ALPHABET.length)]) }
    }
}

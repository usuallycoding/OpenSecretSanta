package com.usuallycoding.opensecretsanta.server.security

import com.usuallycoding.opensecretsanta.domain.auth.SecretBox
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/** AES-256-GCM; the output is a random 12-byte nonce followed by the ciphertext and its auth tag. */
class AesGcmSecretBox(key: ByteArray, private val random: SecureRandom = SecureRandom()) : SecretBox {
    private val keySpec: SecretKeySpec

    init {
        require(key.size == KEY_BYTES) { "AES-256 needs a $KEY_BYTES-byte key but got ${key.size}" }
        keySpec = SecretKeySpec(key, "AES")
    }

    override fun encrypt(plain: String): ByteArray {
        val nonce = ByteArray(NONCE_BYTES).also(random::nextBytes)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, GCMParameterSpec(TAG_BITS, nonce))
        return nonce + cipher.doFinal(plain.encodeToByteArray())
    }

    /** Throws if the data was altered or was encrypted with a different key. */
    override fun decrypt(encrypted: ByteArray): String {
        require(encrypted.size > NONCE_BYTES) { "Encrypted value is too short" }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, GCMParameterSpec(TAG_BITS, encrypted.copyOfRange(0, NONCE_BYTES)))
        return cipher.doFinal(encrypted, NONCE_BYTES, encrypted.size - NONCE_BYTES).decodeToString()
    }

    private companion object {
        const val KEY_BYTES = 32
        const val NONCE_BYTES = 12
        const val TAG_BITS = 128
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}

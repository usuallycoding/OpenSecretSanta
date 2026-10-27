package com.usuallycoding.opensecretsanta.server

import com.usuallycoding.opensecretsanta.server.security.AesGcmSecretBox
import com.usuallycoding.opensecretsanta.server.security.BcryptPasswordHasher
import java.security.GeneralSecurityException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class BcryptPasswordHasherTest {
    private val hasher = BcryptPasswordHasher(cost = 4)

    @Test
    fun correctPasswordVerifies() {
        assertTrue(hasher.verify("correct horse", hasher.hash("correct horse")))
    }

    @Test
    fun wrongPasswordDoesNotVerify() {
        assertFalse(hasher.verify("wrong horse", hasher.hash("correct horse")))
    }

    @Test
    fun hashesAreSaltedAndDoNotContainThePassword() {
        val first = hasher.hash("same-password")
        val second = hasher.hash("same-password")
        assertNotEquals(first, second)
        assertFalse(first.contains("same-password"))
        assertTrue(first.startsWith("\$2a\$04\$"))
    }

    @Test
    fun malformedHashDoesNotVerify() {
        assertFalse(hasher.verify("anything", "not-a-bcrypt-hash"))
    }

    @Test
    fun passwordsBeyondBcryptsLimitAreNotTruncated() {
        val long = "x".repeat(100)
        val hash = hasher.hash(long)
        assertTrue(hasher.verify(long, hash))
        assertFalse(hasher.verify("x".repeat(99) + "y", hash))
        assertFalse(hasher.verify("x".repeat(72), hash))
    }

    @Test
    fun unicodePasswordsWork() {
        assertTrue(hasher.verify("pässwörd-密码", hasher.hash("pässwörd-密码")))
    }
}

class AesGcmSecretBoxTest {
    private val key = ByteArray(32) { it.toByte() }
    private val box = AesGcmSecretBox(key)

    @Test
    fun roundTrips() {
        assertEquals("TempPass0001", box.decrypt(box.encrypt("TempPass0001")))
        assertEquals("", box.decrypt(box.encrypt("")))
        assertEquals("päss-密码", box.decrypt(box.encrypt("päss-密码")))
    }

    @Test
    fun equalPlaintextsGiveDifferentCiphertexts() {
        assertFalse(box.encrypt("same").contentEquals(box.encrypt("same")))
    }

    @Test
    fun ciphertextDoesNotContainThePlaintext() {
        assertFalse(box.encrypt("TempPass0001").decodeToString().contains("TempPass0001"))
    }

    @Test
    fun modifiedCiphertextIsRejected() {
        val encrypted = box.encrypt("TempPass0001")
        encrypted[encrypted.size - 1] = (encrypted.last() + 1).toByte()
        assertFailsWith<GeneralSecurityException> { box.decrypt(encrypted) }
    }

    @Test
    fun modifiedNonceIsRejected() {
        val encrypted = box.encrypt("TempPass0001")
        encrypted[0] = (encrypted[0] + 1).toByte()
        assertFailsWith<GeneralSecurityException> { box.decrypt(encrypted) }
    }

    @Test
    fun wrongKeyIsRejected() {
        val encrypted = box.encrypt("TempPass0001")
        val other = AesGcmSecretBox(ByteArray(32) { (it + 1).toByte() })
        assertFailsWith<GeneralSecurityException> { other.decrypt(encrypted) }
    }

    @Test
    fun truncatedInputIsRejected() {
        assertFailsWith<IllegalArgumentException> { box.decrypt(ByteArray(5)) }
    }

    @Test
    fun keyMustBe32Bytes() {
        assertFailsWith<IllegalArgumentException> { AesGcmSecretBox(ByteArray(16)) }
    }
}

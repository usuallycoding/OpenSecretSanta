package com.usuallycoding.opensecretsanta.server

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.usuallycoding.opensecretsanta.domain.auth.Clock
import com.usuallycoding.opensecretsanta.domain.user.TempPassword
import com.usuallycoding.opensecretsanta.domain.user.Username
import com.usuallycoding.opensecretsanta.server.security.JwtTokenService
import com.usuallycoding.opensecretsanta.server.security.SecureTempPasswordGenerator
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SecureTempPasswordGeneratorTest {
    private val generator = SecureTempPasswordGenerator()

    @Test
    fun hasTwelveCharactersFromTheLookAlikeFreeAlphabet() {
        repeat(500) {
            val password = generator.generate()
            assertEquals(TempPassword.LENGTH, password.length)
            assertTrue(password.all { it in TempPassword.ALPHABET }, password)
            assertTrue(password.none { it in "0O1lI" }, password)
        }
    }

    @Test
    fun passwordsAreNotRepeated() {
        assertEquals(1000, List(1000) { generator.generate() }.toSet().size)
    }
}

class JwtTokenServiceTest {
    private val systemClock = Clock { System.currentTimeMillis() }
    private val secret = "a-test-secret-with-enough-length"
    private val service = JwtTokenService(secret, systemClock)
    private val alice = Username.parse("alice")

    @Test
    fun validTokenIsAcceptedWithItsClaims() {
        val claims = assertNotNull(service.verify(service.issue(alice, 7)))
        assertEquals(alice, claims.username)
        assertEquals(7, claims.tokenVersion)
    }

    @Test
    fun tokenLivesSevenDaysByDefault() {
        val decoded = JWT.decode(service.issue(alice, 1))
        assertEquals(7L * 24 * 60 * 60 * 1000, decoded.expiresAt.time - decoded.issuedAt.time)
    }

    @Test
    fun expiredTokenIsRejected() {
        val expired = JwtTokenService(secret, systemClock, lifetimeMillis = -1_000)
        assertNull(service.verify(expired.issue(alice, 1)))
    }

    @Test
    fun tokenIssuedWellInThePastExpiresByTheClockItWasIssuedWith() {
        val eightDaysAgo = Clock { System.currentTimeMillis() - 8L * 24 * 60 * 60 * 1000 }
        assertNull(service.verify(JwtTokenService(secret, eightDaysAgo).issue(alice, 1)))
    }

    @Test
    fun tamperedPayloadIsRejected() {
        val (header, _, signature) = service.issue(alice, 1).split(".")
        val forgedPayload = Base64.getUrlEncoder().withoutPadding()
            .encodeToString("""{"sub":"mallory","tv":1,"exp":99999999999}""".toByteArray())
        assertNull(service.verify("$header.$forgedPayload.$signature"))
    }

    @Test
    fun tamperedSignatureIsRejected() {
        val token = service.issue(alice, 1)
        val flipped = token.dropLast(2) + if (token.takeLast(2) == "AA") "BB" else "AA"
        assertNull(service.verify(flipped))
    }

    @Test
    fun tokenSignedWithAnotherSecretIsRejected() {
        assertNull(service.verify(JwtTokenService("another-secret-of-similar-length!", systemClock).issue(alice, 1)))
    }

    @Test
    fun garbageAndUnsignedTokensAreRejected() {
        assertNull(service.verify(""))
        assertNull(service.verify("not.a.jwt"))
        assertNull(service.verify("garbage"))
        val unsigned = JWT.create().withSubject("alice").withClaim("tv", 1).sign(Algorithm.none())
        assertNull(service.verify(unsigned))
    }

    @Test
    fun tokenWithoutVersionOrWithInvalidSubjectIsRejected() {
        val algorithm = Algorithm.HMAC256(secret)
        assertNull(service.verify(JWT.create().withSubject("alice").sign(algorithm)))
        assertNull(service.verify(JWT.create().withSubject("no").withClaim("tv", 1).sign(algorithm)))
        assertNull(service.verify(JWT.create().withClaim("tv", 1).sign(algorithm)))
    }

    @Test
    fun tokenDoesNotContainSecretsOrPasswords() {
        val payload = String(Base64.getUrlDecoder().decode(service.issue(alice, 1).split(".")[1]))
        assertFalse(payload.contains(secret))
        assertTrue(payload.contains("alice"))
    }
}

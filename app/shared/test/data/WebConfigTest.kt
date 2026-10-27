package com.usuallycoding.opensecretsanta.data

import com.usuallycoding.opensecretsanta.data.web.resolveBaseUrl
import kotlin.test.Test
import kotlin.test.assertEquals

class WebConfigTest {
    private val origin = "https://app.example.org"

    @Test
    fun theConfiguredAddressWins() {
        assertEquals("https://api.example.org", resolveBaseUrl("""{"baseUrl": "https://api.example.org"}""", origin))
    }

    @Test
    fun aTrailingSlashAndSurroundingSpacesAreTidied() {
        assertEquals("https://api.example.org", resolveBaseUrl("""{"baseUrl": "  https://api.example.org/ "}""", origin))
    }

    @Test
    fun aMissingFileFallsBackToTheOrigin() {
        assertEquals(origin, resolveBaseUrl(null, origin))
    }

    @Test
    fun everyUnusableFileFallsBackToTheOrigin() {
        for (text in listOf("", "not json", "[]", "{}", """{"baseUrl": ""}""", """{"baseUrl": "   "}""", """{"baseUrl": null}""", """{"baseUrl": 8080}""", """{"other": "x"}""", """"just a string"""")) {
            assertEquals(origin, resolveBaseUrl(text, origin), text)
        }
    }

    @Test
    fun otherKeysAreIgnored() {
        assertEquals("http://localhost:8080", resolveBaseUrl("""{"baseUrl": "http://localhost:8080", "theme": "dark"}""", origin))
    }
}

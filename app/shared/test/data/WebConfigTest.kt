package com.usuallycoding.opensecretsanta.data

import com.usuallycoding.opensecretsanta.data.web.FirebaseWebConfig
import com.usuallycoding.opensecretsanta.data.web.resolveBaseUrl
import com.usuallycoding.opensecretsanta.data.web.resolveFirebaseConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

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
    // --- firebase ---

    private val firebase = """{"apiKey":"AIza","projectId":"p","messagingSenderId":"123","appId":"1:123:web:abc","vapidKey":"BDi_x"}"""

    @Test
    fun theFirebaseSettingsAreReadWhenAllFiveAreThere() {
        val config = resolveFirebaseConfig("""{"baseUrl": "http://localhost:8081", "firebase": $firebase}""")

        assertEquals(FirebaseWebConfig("AIza", "p", "123", "1:123:web:abc", "BDi_x"), config)
    }

    @Test
    fun spacesAroundASettingAreTidied() {
        val config = resolveFirebaseConfig("""{"firebase": {"apiKey":" AIza ","projectId":"p","messagingSenderId":"123","appId":"a","vapidKey":"k"}}""")

        assertEquals("AIza", config?.apiKey)
    }

    @Test
    fun noFirebaseSettingsMeansNoPush() {
        for (text in listOf(null, "", "not json", "[]", "{}", """{"baseUrl":"x"}""", """{"firebase": null}""", """{"firebase": []}""", """{"firebase": "x"}""")) {
            assertNull(resolveFirebaseConfig(text), text.toString())
        }
    }

    @Test
    fun aMissingOrEmptyOrNonTextSettingMeansNoPush() {
        for (name in listOf("apiKey", "projectId", "messagingSenderId", "appId", "vapidKey")) {
            val without = firebase.replace(Regex("\"$name\":\"[^\"]*\",?"), "").replace(",}", "}")
            assertNull(resolveFirebaseConfig("""{"firebase": $without}"""), "missing $name")
            assertNull(resolveFirebaseConfig("""{"firebase": ${firebase.replace(Regex("\"$name\":\"[^\"]*\""), "\"$name\":\"  \"")}}"""), "blank $name")
            assertNull(resolveFirebaseConfig("""{"firebase": ${firebase.replace(Regex("\"$name\":\"[^\"]*\""), "\"$name\":12")}}"""), "number $name")
        }
    }

    @Test
    fun theFirebaseSettingsDoNotChangeTheServerAddress() {
        assertEquals("http://localhost:8081", resolveBaseUrl("""{"baseUrl": "http://localhost:8081", "firebase": $firebase}""", origin))
    }

}

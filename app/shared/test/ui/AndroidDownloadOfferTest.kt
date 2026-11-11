package com.usuallycoding.opensecretsanta.ui

import com.usuallycoding.opensecretsanta.ui.download.AndroidDownloadOffer
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AndroidDownloadOfferTest {
    private val server = ScriptedServer().also { it.session() }

    private fun offer(enabled: Boolean = true) = AndroidDownloadOffer(server.api, enabled, immediateScope())

    private fun healthSays(androidDownload: String) {
        server.healthAnswer = HttpStatusCode.OK to """{"status":"ok","schemaVersion":16$androidDownload}"""
    }

    private fun healthCalls() = server.calls.count { it == "GET /health" }

    @Test
    fun nothingIsOfferedBeforeTheServerHasBeenAsked() {
        assertFalse(offer().offered.value)
        assertEquals(0, healthCalls())
    }

    @Test
    fun anAppThatCanBeDownloadedIsOffered() = runTest {
        healthSays(""","androidDownload":true""")
        val offer = offer()

        offer.refresh()?.join()

        assertTrue(offer.offered.value)
    }

    @Test
    fun noAppMeansNoOffer() = runTest {
        healthSays(""","androidDownload":false""")
        val offer = offer()

        offer.refresh()?.join()

        assertFalse(offer.offered.value)
    }

    @Test
    fun aServerThatDoesNotSayMeansNoOffer() = runTest {
        healthSays("")
        val offer = offer()

        offer.refresh()?.join()

        assertFalse(offer.offered.value)
    }

    @Test
    fun aServerThatCannotBeReachedMeansNoOfferAndNoFailure() = runTest {
        server.unreachable = true
        val offer = offer()

        offer.refresh()?.join()

        assertFalse(offer.offered.value)
    }

    @Test
    fun aServerThatIsNotHealthyMeansNoOffer() = runTest {
        server.healthAnswer = HttpStatusCode.ServiceUnavailable to """{"status":"error"}"""
        val offer = offer()

        offer.refresh()?.join()

        assertFalse(offer.offered.value)
    }

    @Test
    fun theOfferGoesAwayWhenTheAppIsTakenDown() = runTest {
        val offer = offer()
        healthSays(""","androidDownload":true""")
        offer.refresh()?.join()
        assertTrue(offer.offered.value)

        healthSays(""","androidDownload":false""")
        offer.refresh()?.join()

        assertFalse(offer.offered.value)
    }

    @Test
    fun thePhonesNeverAskTheServer() = runTest {
        healthSays(""","androidDownload":true""")
        val offer = offer(enabled = false)

        assertNull(offer.refresh())

        assertFalse(offer.offered.value)
        assertEquals(0, healthCalls())
    }

    @Test
    fun theDownloadAddressIsTheServersLatestRoute() {
        assertEquals("https://api.example.org/download_latest", offer().url)
    }
}

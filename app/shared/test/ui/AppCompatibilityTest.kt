package com.usuallycoding.opensecretsanta.ui

import com.usuallycoding.opensecretsanta.contract.ApiVersion
import com.usuallycoding.opensecretsanta.ui.update.AppCompatibility
import com.usuallycoding.opensecretsanta.ui.update.AppScreen
import com.usuallycoding.opensecretsanta.ui.update.Compatibility
import com.usuallycoding.opensecretsanta.ui.update.screenFor
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AppCompatibilityTest {
    private val server = ScriptedServer().also { it.session() }
    private val compatibility = AppCompatibility(server.api)

    private fun healthSays(apiVersion: Int?, androidDownload: Boolean? = null) {
        val fields = listOfNotNull(
            apiVersion?.let { """"apiVersion":$it""" },
            androidDownload?.let { """"androidDownload":$it""" },
        ).joinToString(",").let { if (it.isEmpty()) "" else ",$it" }
        server.healthAnswer = HttpStatusCode.OK to """{"status":"ok","schemaVersion":16$fields}"""
    }

    private val state get() = compatibility.state.value

    @Test
    fun nothingIsKnownBeforeTheFirstCheck() {
        assertEquals(Compatibility.Checking, state)
    }

    @Test
    fun aServerWithANewerApiRequiresAnUpdate() = runTest {
        healthSays(ApiVersion.CURRENT + 1, androidDownload = false)

        compatibility.check()

        assertEquals(Compatibility.UpdateRequired(androidDownload = false), state)
    }

    @Test
    fun theAndroidDownloadComesFromTheAnswerThatStoppedTheApp() = runTest {
        healthSays(ApiVersion.CURRENT + 1, androidDownload = true)

        compatibility.check()

        assertEquals(Compatibility.UpdateRequired(androidDownload = true), state)
    }

    @Test
    fun aMissingDownloadFlagMeansNothingToDownload() = runTest {
        healthSays(ApiVersion.CURRENT + 1)

        compatibility.check()

        assertEquals(Compatibility.UpdateRequired(androidDownload = false), state)
    }

    @Test
    fun theSameApiIsFine() = runTest {
        healthSays(ApiVersion.CURRENT, androidDownload = true)

        compatibility.check()

        assertEquals(Compatibility.Ok, state)
    }

    @Test
    fun anOlderApiIsFine() = runTest {
        healthSays(ApiVersion.CURRENT - 1)

        compatibility.check()

        assertEquals(Compatibility.Ok, state)
    }

    @Test
    fun anOlderServerThatSaysNothingIsFine() = runTest {
        healthSays(null)

        compatibility.check()

        assertEquals(Compatibility.Ok, state)
    }

    @Test
    fun aServerThatCannotBeReachedIsFineWhenNothingWasHeard() = runTest {
        server.unreachable = true

        compatibility.check()

        assertEquals(Compatibility.Ok, state)
    }

    @Test
    fun anErrorAnswerIsFine() = runTest {
        server.healthAnswer = HttpStatusCode.ServiceUnavailable to """{"status":"error"}"""

        compatibility.check()

        assertEquals(Compatibility.Ok, state)
    }

    @Test
    fun aFailedCheckKeepsAStoppedAppStopped() = runTest {
        healthSays(ApiVersion.CURRENT + 1, androidDownload = true)
        compatibility.check()

        server.unreachable = true
        compatibility.check()

        assertEquals(Compatibility.UpdateRequired(androidDownload = true), state)
    }

    @Test
    fun aFailedCheckKeepsAWorkingAppWorking() = runTest {
        healthSays(ApiVersion.CURRENT)
        compatibility.check()

        server.unreachable = true
        compatibility.check()

        assertEquals(Compatibility.Ok, state)
    }

    @Test
    fun anErrorAnswerKeepsAStoppedAppStopped() = runTest {
        healthSays(ApiVersion.CURRENT + 1)
        compatibility.check()

        server.healthAnswer = HttpStatusCode.ServiceUnavailable to """{"status":"error"}"""
        compatibility.check()

        assertEquals(Compatibility.UpdateRequired(androidDownload = false), state)
    }

    @Test
    fun aRolledBackServerEndsTheStop() = runTest {
        healthSays(ApiVersion.CURRENT + 1)
        compatibility.check()

        healthSays(ApiVersion.CURRENT)
        compatibility.check()

        assertEquals(Compatibility.Ok, state)
    }

    @Test
    fun aServerThatBecomesNewerStopsAWorkingApp() = runTest {
        healthSays(ApiVersion.CURRENT)
        compatibility.check()

        healthSays(ApiVersion.CURRENT + 1)
        compatibility.check()

        assertEquals(Compatibility.UpdateRequired(androidDownload = false), state)
    }

    @Test
    fun theAnswerNeedsNoSignIn() = runTest {
        healthSays(ApiVersion.CURRENT)

        compatibility.check()

        assertEquals(listOf("GET /health"), server.calls)
    }
}

class ScreenForTest {
    private val stop = Compatibility.UpdateRequired(androidDownload = true)

    @Test
    fun nothingShowsUntilTheServerHasBeenHeardFrom() {
        assertEquals(AppScreen.Loading, screenFor(Compatibility.Checking))
    }

    @Test
    fun aStopReplacesEverythingElse() {
        assertEquals(AppScreen.Update(stop), screenFor(stop))
    }

    @Test
    fun otherwiseTheSessionDecides() {
        assertEquals(AppScreen.Session, screenFor(Compatibility.Ok))
    }
}

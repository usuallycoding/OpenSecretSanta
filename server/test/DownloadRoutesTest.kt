package com.usuallycoding.opensecretsanta.server

import com.usuallycoding.opensecretsanta.contract.ApiRoutes
import com.usuallycoding.opensecretsanta.contract.ErrorResponse
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DownloadRoutesTest {
    private val folder: Path = Files.createTempDirectory("santa-downloads")

    private fun ApplicationTestBuilder.startApp(directory: Path = folder) = application { module(testConfig(downloadsDir = directory)) }

    private fun apk(name: String, bytes: ByteArray) = Files.write(folder.resolve(name), bytes)

    @Test
    fun theNewestAppIsSentAsAnAttachmentWithNoSignIn() = testApplication {
        startApp()
        val content = ByteArray(5_000) { (it % 251).toByte() }
        apk("opensecretsanta-1.1.4-100.apk", byteArrayOf(9, 9))
        apk("opensecretsanta-1.1.5-28900000.apk", content)

        val response = client.get(ApiRoutes.DOWNLOAD_LATEST)

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("application/vnd.android.package-archive", response.headers[HttpHeaders.ContentType])
        val disposition = assertNotNull(response.headers[HttpHeaders.ContentDisposition])
        assertTrue(disposition.startsWith("attachment"), disposition)
        assertTrue("opensecretsanta-1.1.5-28900000.apk" in disposition, disposition)
        assertContentEquals(content, response.bodyAsBytes())
    }

    @Test
    fun aNewerFileCopiedWhileTheServerRunsIsServedAtOnce() = testApplication {
        startApp()
        apk("opensecretsanta-1.1.5-9000.apk", byteArrayOf(1))
        assertContentEquals(byteArrayOf(1), client.get(ApiRoutes.DOWNLOAD_LATEST).bodyAsBytes())

        apk("opensecretsanta-1.1.6-9100.apk", byteArrayOf(2))

        assertContentEquals(byteArrayOf(2), client.get(ApiRoutes.DOWNLOAD_LATEST).bodyAsBytes())
    }

    @Test
    fun aFileStillBeingCopiedIsNeverServed() = testApplication {
        startApp()
        apk("opensecretsanta-1.1.5-9000.apk", byteArrayOf(1))
        apk("opensecretsanta-1.1.6-9100.apk.part", byteArrayOf(2, 2))

        assertContentEquals(byteArrayOf(1), client.get(ApiRoutes.DOWNLOAD_LATEST).bodyAsBytes())

        Files.move(folder.resolve("opensecretsanta-1.1.6-9100.apk.part"), folder.resolve("opensecretsanta-1.1.6-9100.apk"))

        assertContentEquals(byteArrayOf(2, 2), client.get(ApiRoutes.DOWNLOAD_LATEST).bodyAsBytes())
    }

    @Test
    fun anEmptyFolderAnswers404WithAMessage() = testApplication {
        startApp()

        val response = client.get(ApiRoutes.DOWNLOAD_LATEST)

        assertEquals(HttpStatusCode.NotFound, response.status)
        assertTrue(Json.decodeFromString<ErrorResponse>(response.bodyAsText()).message.isNotBlank())
        assertEquals(null, response.headers[HttpHeaders.ContentDisposition])
    }

    @Test
    fun aMissingFolderAnswers404AndTheServerKeepsRunning() = testApplication {
        startApp(folder.resolve("nothing-here"))

        assertEquals(HttpStatusCode.NotFound, client.get(ApiRoutes.DOWNLOAD_LATEST).status)
        assertEquals(HttpStatusCode.OK, client.get(ApiRoutes.HEALTH).status)
    }

    @Test
    fun noOtherFileOfTheFolderCanBeReachedOrListed() = testApplication {
        startApp()
        apk("opensecretsanta-1.1.5-9000.apk", byteArrayOf(1))
        apk("secret.txt", "do not serve".toByteArray())

        for (path in listOf(
            "${ApiRoutes.DOWNLOAD_LATEST}/",
            "${ApiRoutes.DOWNLOAD_LATEST}/secret.txt",
            "${ApiRoutes.DOWNLOAD_LATEST}/opensecretsanta-1.1.5-9000.apk",
            "${ApiRoutes.DOWNLOAD_LATEST}/..%2Fsecret.txt",
            "${ApiRoutes.DOWNLOAD_LATEST}/../secret.txt",
            "/secret.txt",
            "/downloads",
        )) {
            val response = client.get(path)
            assertTrue(response.status.value >= 400, "$path answered ${response.status}")
            assertTrue("do not serve" !in response.bodyAsText(), path)
        }
    }
}

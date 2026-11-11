package com.usuallycoding.opensecretsanta.contract

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HealthResponseTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun roundTrips() {
        for (value in listOf(
            HealthResponse("ok", schemaVersion = 16, androidDownload = true),
            HealthResponse("ok", schemaVersion = 16, androidDownload = false),
            HealthResponse("error"),
        )) {
            assertEquals(value, json.decodeFromString<HealthResponse>(json.encodeToString(value)))
        }
    }

    @Test
    fun anAnswerWithoutTheDownloadFlagStillReads() {
        val answer = json.decodeFromString<HealthResponse>("""{"status":"ok","schemaVersion":15}""")

        assertNull(answer.androidDownload)
    }

    @Test
    fun anOlderReaderIgnoresTheDownloadFlag() {
        val strictOld = Json { ignoreUnknownKeys = true }

        assertEquals("ok", strictOld.decodeFromString<HealthResponse>("""{"status":"ok","schemaVersion":16,"androidDownload":true,"other":1}""").status)
    }

    @Test
    fun theDownloadRouteIsFixed() {
        assertEquals("/download_latest", ApiRoutes.DOWNLOAD_LATEST)
    }
}

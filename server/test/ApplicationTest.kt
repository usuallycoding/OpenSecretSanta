package com.usuallycoding.opensecretsanta.server

import com.usuallycoding.opensecretsanta.contract.ApiRoutes
import com.usuallycoding.opensecretsanta.contract.ErrorResponse
import com.usuallycoding.opensecretsanta.contract.HealthResponse
import com.usuallycoding.opensecretsanta.server.data.db.Migration
import com.usuallycoding.opensecretsanta.server.data.db.MigrationException
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

class ApplicationTest {

    private fun tempDbPath(): Path = newTempDbPath()

    private fun configFor(path: Path) = testConfig(path)

    @Test
    fun healthReportsOkWithSchemaVersion() = testApplication {
        application { module(configFor(tempDbPath())) }

        val response = client.get(ApiRoutes.HEALTH)

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(HealthResponse("ok", schemaVersion = 6), Json.decodeFromString(response.bodyAsText()))
    }

    @Test
    fun healthReports503WhenDatabaseCannotBeQueried() = testApplication {
        val path = tempDbPath()
        application { module(configFor(path)) }
        client.get(ApiRoutes.HEALTH) // boots the application and runs migrations
        Files.delete(path) // the next connection opens an empty database without schema_version

        val response = client.get(ApiRoutes.HEALTH)

        assertEquals(HttpStatusCode.ServiceUnavailable, response.status)
        assertEquals(HealthResponse("error"), Json.decodeFromString(response.bodyAsText()))
    }

    @Test
    fun unhandledExceptionBecomesGeneric500WithoutDetails() = testApplication {
        application {
            module(configFor(tempDbPath()))
            routing { get("/boom") { error("secret internal detail") } }
        }

        val response = client.get("/boom")

        assertEquals(HttpStatusCode.InternalServerError, response.status)
        val body = response.bodyAsText()
        assertNotNull(Json.decodeFromString<ErrorResponse>(body))
        assertFalse(body.contains("secret internal detail"))
        assertFalse(body.contains("Exception"))
        assertFalse(body.contains(" at "))
    }

    @Test
    fun oldHelloRouteIsGone() = testApplication {
        application { module(configFor(tempDbPath())) }

        assertEquals(HttpStatusCode.NotFound, client.get("/").status)
    }

    @Test
    fun failingMigrationAbortsStartup() {
        val broken = Migration(1, "broken", "INSERT INTO missing_table VALUES (1);")
        val failure = assertFailsWith<Throwable> {
            testApplication {
                application { module(configFor(tempDbPath()), listOf(broken)) }
                client.get(ApiRoutes.HEALTH)
            }
        }
        generateSequence(failure) { it.cause }.any { it is MigrationException }.also {
            assertEquals(true, it, "Expected a MigrationException in the cause chain of $failure")
        }
    }
}

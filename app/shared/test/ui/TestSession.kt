package com.usuallycoding.opensecretsanta.ui

import com.usuallycoding.opensecretsanta.contract.ApiRoutes
import com.usuallycoding.opensecretsanta.data.api.ApiClient
import com.usuallycoding.opensecretsanta.data.api.ApiError
import com.usuallycoding.opensecretsanta.data.api.AuthContext
import com.usuallycoding.opensecretsanta.data.api.createHttpClient
import com.usuallycoding.opensecretsanta.data.session.MemoryTokenStore
import com.usuallycoding.opensecretsanta.data.session.SessionManager
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

/** A real session over a scripted server, so view-model tests exercise the same code the app runs. */
class ScriptedServer {
    var loginAnswer: Pair<HttpStatusCode, Pair<String, Map<String, String>>> =
        HttpStatusCode.OK to ("""{"token":"t1","mustChangePassword":false}""" to emptyMap())
    var passwordAnswer: Pair<HttpStatusCode, String> = HttpStatusCode.OK to """{"token":"t2"}"""
    var accountBody: (String?) -> String = { """{"username":"alice","isAdmin":false,"mustChangePassword":false}""" }
    var unreachable = false
    val calls = mutableListOf<String>()
    val bodies = mutableListOf<String>()

    val engine = MockEngine { request ->
        calls += "${request.method.value} ${request.url.encodedPath}"
        bodies += (request.body as? io.ktor.http.content.TextContent)?.text.orEmpty()
        if (unreachable) throw RuntimeException("down")
        fun json(status: HttpStatusCode, body: String, extra: Map<String, String> = emptyMap()) = respond(
            body, status,
            Headers.build {
                append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                extra.forEach { (k, v) -> append(k, v) }
            },
        )
        when (request.url.encodedPath) {
            ApiRoutes.LOGIN -> json(loginAnswer.first, loginAnswer.second.first, loginAnswer.second.second)
            ApiRoutes.ME_PASSWORD -> json(passwordAnswer.first, passwordAnswer.second)
            ApiRoutes.ME -> json(HttpStatusCode.OK, accountBody(request.headers[HttpHeaders.Authorization]?.removePrefix("Bearer ")))
            else -> respond("", HttpStatusCode.NotFound)
        }
    }

    fun session(): SessionManager {
        lateinit var created: SessionManager
        val api = ApiClient(createHttpClient(engine), "https://api.example.org", object : AuthContext {
            override val token get() = created.token
            override fun rejected(error: ApiError) = created.rejected(error)
        })
        created = SessionManager(lazyOf(api), MemoryTokenStore(), CoroutineScope(Dispatchers.Unconfined))
        return created
    }
}

fun immediateScope() = CoroutineScope(Dispatchers.Unconfined)

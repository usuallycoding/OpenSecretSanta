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
import io.ktor.http.HttpMethod
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.usuallycoding.opensecretsanta.contract.ReorderWishlistRequest
import com.usuallycoding.opensecretsanta.contract.SharedWishlistItemDto
import com.usuallycoding.opensecretsanta.contract.WishlistItemDto
import com.usuallycoding.opensecretsanta.contract.WishlistItemRequest
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

/** A real session over a scripted server, so view-model tests exercise the same code the app runs. */
class ScriptedServer {
    var loginAnswer: Pair<HttpStatusCode, Pair<String, Map<String, String>>> =
        HttpStatusCode.OK to ("""{"token":"t1","mustChangePassword":false}""" to emptyMap())
    var passwordAnswer: Pair<HttpStatusCode, String> = HttpStatusCode.OK to """{"token":"t2"}"""
    var accountBody: (String?) -> String = { """{"username":"alice","isAdmin":false,"mustChangePassword":false}""" }
    var groupsAnswer: Pair<HttpStatusCode, String> = HttpStatusCode.OK to "[]"
    var groupAnswer: (Long) -> Pair<HttpStatusCode, String> =
        { HttpStatusCode.NotFound to """{"code":"not_found","message":"Group not found."}""" }
    var profileAnswer: Pair<HttpStatusCode, String> =
        HttpStatusCode.OK to """{"username":"alice","isAdmin":false,"mustChangePassword":false}"""
    /** The person's own wishlist as the server keeps it: it really adds, edits, deletes and reorders. */
    val wishlist = FakeWishlist()

    /** What the person's receiver keeps, as the receiver's list endpoint returns it (no ids). */
    var receiverWishlist: List<SharedWishlistItemDto> = emptyList()
    var receiverWishlistAnswer: Pair<HttpStatusCode, String>? = null

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
            ApiRoutes.ME ->
                if (request.method == HttpMethod.Put) {
                    json(profileAnswer.first, profileAnswer.second)
                } else {
                    json(HttpStatusCode.OK, accountBody(request.headers[HttpHeaders.Authorization]?.removePrefix("Bearer ")))
                }
            ApiRoutes.GROUPS -> json(groupsAnswer.first, groupsAnswer.second)
            else -> {
                val path = request.url.encodedPath
                val wishlistAnswer = wishlist.answer(request.method.value, path, bodies.last())
                if (wishlistAnswer != null) return@MockEngine json(wishlistAnswer.first, wishlistAnswer.second)
                if (path.endsWith("/receiver/wishlist")) {
                    return@MockEngine receiverWishlistAnswer?.let { json(it.first, it.second) }
                        ?: json(HttpStatusCode.OK, Json.encodeToString(receiverWishlist))
                }
                val id = request.url.encodedPath.removePrefix("${ApiRoutes.GROUPS}/").toLongOrNull()
                if (request.url.encodedPath.startsWith("${ApiRoutes.GROUPS}/") && id != null) {
                    groupAnswer(id).let { json(it.first, it.second) }
                } else {
                    respond("", HttpStatusCode.NotFound)
                }
            }
        }
    }

    /** The client of the session built last, for view models that call the API directly. */
    lateinit var api: ApiClient
        private set

    fun session(): SessionManager {
        lateinit var created: SessionManager
        val api = ApiClient(createHttpClient(engine), "https://api.example.org", object : AuthContext {
            override val token get() = created.token
            override fun rejected(error: ApiError) = created.rejected(error)
        })
        this.api = api
        created = SessionManager(lazyOf(api), MemoryTokenStore(), CoroutineScope(Dispatchers.Unconfined))
        return created
    }
}

fun immediateScope() = CoroutineScope(Dispatchers.Unconfined)

/**
 * A wishlist with the server's behavior: items get ids, a list holds at most 50, an empty title is
 * refused, a reorder must name exactly the current ids, and a group that is not editable answers 409.
 * A test can also queue a failure that the next change (add, edit, delete or reorder) answers with.
 */
class FakeWishlist {
    private class Row(val id: Long, var title: String, var note: String?, var url: String?)

    private val rows = mutableListOf<Row>()
    private var nextId = 1L

    /** False answers every change with 409, like a group that was archived. */
    var editable = true

    private val failures = ArrayDeque<Pair<HttpStatusCode, String>>()

    val ids: List<Long> get() = rows.map { it.id }
    val titles: List<String> get() = rows.map { it.title }

    fun add(title: String, note: String? = null, url: String? = null): Long {
        val row = Row(nextId++, title, note, url)
        rows += row
        return row.id
    }

    /** Another device changed the list: remove an item without the screen knowing. */
    fun removeBehindTheScenes(id: Long) {
        rows.removeAll { it.id == id }
    }

    fun failNextChangeWith(status: HttpStatusCode, code: String, message: String = "refused", field: String? = null) {
        val extra = if (field != null) ""","field":"$field"""" else ""
        failures += status to """{"code":"$code","message":"$message"$extra}"""
    }

    private fun dto(row: Row) = WishlistItemDto(row.id, row.title, row.note, row.url, "2026-12-01T10:00:00Z", "2026-12-01T10:00:00Z")

    private fun error(status: HttpStatusCode, code: String, message: String, field: String? = null) =
        status to """{"code":"$code","message":"$message"${if (field != null) ""","field":"$field"""" else ""}}"""

    /** The answer to a request under `/groups/{id}/wishlist`, or null when the request is about something else. */
    fun answer(method: String, path: String, body: String): Pair<HttpStatusCode, String>? {
        val match = Regex("/groups/\\d+/wishlist(/items(/(\\d+))?|/order)?").matchEntire(path) ?: return null
        val items = match.groupValues[1] == "/items"
        val item = match.groupValues[3].toLongOrNull()
        val order = match.groupValues[1] == "/order"
        if (method == "GET" && match.groupValues[1].isEmpty()) return HttpStatusCode.OK to Json.encodeToString(rows.map(::dto))
        // everything below changes the list
        failures.removeFirstOrNull()?.let { return it }
        if (!editable) return error(HttpStatusCode.Conflict, "invalid_state", "The wishlist can no longer be changed.")
        return when {
            method == "POST" && items && item == null -> {
                val request = Json.decodeFromString<WishlistItemRequest>(body)
                when {
                    request.title.isBlank() -> error(HttpStatusCode.BadRequest, "validation_failed", "Title must be 1 to 200 characters long.", "title")
                    rows.size >= 50 -> error(HttpStatusCode.Conflict, "invalid_state", "The wishlist is full.")
                    else -> {
                        val row = Row(nextId++, request.title.trim(), request.note?.trim()?.ifEmpty { null }, request.url?.trim()?.ifEmpty { null })
                        rows += row
                        HttpStatusCode.Created to Json.encodeToString(dto(row))
                    }
                }
            }
            method == "PUT" && item != null -> {
                val row = rows.firstOrNull { it.id == item } ?: return error(HttpStatusCode.NotFound, "not_found", "Item not found.")
                val request = Json.decodeFromString<WishlistItemRequest>(body)
                if (request.title.isBlank()) return error(HttpStatusCode.BadRequest, "validation_failed", "Title must be 1 to 200 characters long.", "title")
                row.title = request.title.trim()
                row.note = request.note?.trim()?.ifEmpty { null }
                row.url = request.url?.trim()?.ifEmpty { null }
                HttpStatusCode.OK to Json.encodeToString(dto(row))
            }
            method == "DELETE" && item != null ->
                if (rows.removeAll { it.id == item }) HttpStatusCode.NoContent to "" else error(HttpStatusCode.NotFound, "not_found", "Item not found.")
            method == "PUT" && order -> {
                val wanted = Json.decodeFromString<ReorderWishlistRequest>(body).itemIds
                if (wanted.size != rows.size || wanted.toSet() != ids.toSet()) {
                    error(HttpStatusCode.BadRequest, "validation_failed", "The ids must be exactly your current items.", "itemIds")
                } else {
                    val byId = rows.associateBy { it.id }
                    rows.clear()
                    rows += wanted.map { byId.getValue(it) }
                    HttpStatusCode.OK to Json.encodeToString(rows.map(::dto))
                }
            }
            else -> null
        }
    }
}

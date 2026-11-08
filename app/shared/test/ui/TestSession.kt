package com.usuallycoding.opensecretsanta.ui

import com.usuallycoding.opensecretsanta.contract.AddDrawRuleRequest
import com.usuallycoding.opensecretsanta.contract.HandOverAdminRequest
import com.usuallycoding.opensecretsanta.contract.AnnouncementDto
import com.usuallycoding.opensecretsanta.contract.AnnouncementRequest
import com.usuallycoding.opensecretsanta.contract.DrawRuleDto
import com.usuallycoding.opensecretsanta.contract.DrawRulesDto
import com.usuallycoding.opensecretsanta.contract.AnnouncementResponse
import com.usuallycoding.opensecretsanta.contract.ApiRoutes
import com.usuallycoding.opensecretsanta.contract.ReadMarksDto
import com.usuallycoding.opensecretsanta.contract.ReadMarkDto
import com.usuallycoding.opensecretsanta.contract.MarkReadRequest
import com.usuallycoding.opensecretsanta.contract.SendLetterRequest
import com.usuallycoding.opensecretsanta.contract.LetterDto
import com.usuallycoding.opensecretsanta.contract.PushSummaryDto
import com.usuallycoding.opensecretsanta.contract.EmailSummaryDto
import com.usuallycoding.opensecretsanta.contract.ErrorResponse
import com.usuallycoding.opensecretsanta.contract.DeletionImpactDto
import com.usuallycoding.opensecretsanta.contract.DeliveryInfoDto
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
import com.usuallycoding.opensecretsanta.contract.AccountDto
import com.usuallycoding.opensecretsanta.contract.AdminGroupDto
import com.usuallycoding.opensecretsanta.contract.AdminLetterBoxDto
import com.usuallycoding.opensecretsanta.contract.AdminQuestionThreadDto
import com.usuallycoding.opensecretsanta.contract.AssignmentDto
import com.usuallycoding.opensecretsanta.contract.ManagedUserDto
import com.usuallycoding.opensecretsanta.contract.ResetPasswordResponse
import com.usuallycoding.opensecretsanta.contract.AnswerRequest
import com.usuallycoding.opensecretsanta.contract.CreateGroupRequest
import com.usuallycoding.opensecretsanta.contract.CreatedAccountDto
import com.usuallycoding.opensecretsanta.contract.GroupChangeResponse
import com.usuallycoding.opensecretsanta.contract.GroupDetailsRequest
import com.usuallycoding.opensecretsanta.contract.ParticipantDto
import com.usuallycoding.opensecretsanta.contract.ParticipantsRequest
import com.usuallycoding.opensecretsanta.contract.AskQuestionRequest
import com.usuallycoding.opensecretsanta.contract.NotificationSettingsDto
import com.usuallycoding.opensecretsanta.contract.NotificationSettingsRequest
import com.usuallycoding.opensecretsanta.contract.RegisterDeviceRequest
import com.usuallycoding.opensecretsanta.contract.RemoveDeviceRequest
import com.usuallycoding.opensecretsanta.contract.QuestionDto
import com.usuallycoding.opensecretsanta.contract.ReceiverDto
import com.usuallycoding.opensecretsanta.contract.ReorderWishlistRequest
import com.usuallycoding.opensecretsanta.contract.SantaRevealDto
import com.usuallycoding.opensecretsanta.contract.SharedWishlistItemDto
import com.usuallycoding.opensecretsanta.contract.WishlistItemDto
import com.usuallycoding.opensecretsanta.contract.WishlistItemRequest
import com.usuallycoding.opensecretsanta.domain.gift.LetterLimits
import com.usuallycoding.opensecretsanta.domain.gift.QuestionLimits
import com.usuallycoding.opensecretsanta.domain.wishlist.WishlistLimits
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
    /** What the person told the group, as `GET /groups/{id}/delivery-info` answers, and how a `PUT` there is answered. */
    var deliveryInfoBody: String = "{}"
    var deliveryInfoAnswer: Pair<HttpStatusCode, String>? = null
    /** The person's own wishlist as the server keeps it: it really adds, edits, deletes and reorders. */
    val wishlist = FakeWishlist()

    /** The users the administrator manages, as the admin user routes keep and answer them. */
    val adminUsers = FakeAdminUsers()

    /** The administrator's groups, as the admin routes keep and answer them. */
    val adminGroups = FakeAdminGroups()

    /** What the owner reads inside those groups: the draw, participants' wishlists and question threads. */
    val oversight = FakeAdminOversight(adminGroups)

    /** The receiver, the two question threads and the reveal as the server keeps and answers them. */
    val gift = FakeGift()

    /** The letters between a receiver and their Santa, as the server keeps and answers them. */
    val letters = FakeLetters(gift)

    /** What the person has seen of their receiver's things, as the server keeps and answers it. */
    val readMarks = FakeReadMarks()

    /** The notification switches and registered devices as the server keeps and answers them. */
    val notifications = FakeNotifications()

    /** Announcements to a group, as the server answers them. */
    val announcements = FakeAnnouncements()

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
            ApiRoutes.ME -> json(HttpStatusCode.OK, accountBody(request.headers[HttpHeaders.Authorization]?.removePrefix("Bearer ")))
            ApiRoutes.GROUPS -> json(groupsAnswer.first, groupsAnswer.second)
            else -> {
                val path = request.url.encodedPath
                if (path.endsWith("/delivery-info") && !path.startsWith("/admin/")) {
                    return@MockEngine if (request.method == HttpMethod.Put) {
                        deliveryInfoAnswer?.let { json(it.first, it.second) } ?: json(HttpStatusCode.OK, bodies.last())
                    } else {
                        json(HttpStatusCode.OK, deliveryInfoBody)
                    }
                }
                adminUsers.answer(request.method.value, path, bodies.last())?.let { return@MockEngine json(it.first, it.second) }
                oversight.answer(request.method.value, path)?.let { return@MockEngine json(it.first, it.second) }
                adminGroups.rulesAnswer(request.method.value, path, bodies.last())?.let { return@MockEngine json(it.first, it.second) }
                adminGroups.confirmed = request.url.parameters["confirm"] == "true"
                adminGroups.answer(request.method.value, path, bodies.last())?.let { return@MockEngine json(it.first, it.second) }
                announcements.answer(request.method.value, path, bodies.last())?.let { return@MockEngine json(it.first, it.second) }
                gift.answer(request.method.value, path, bodies.last())?.let { return@MockEngine json(it.first, it.second) }
                letters.answer(request.method.value, path, bodies.last())?.let { return@MockEngine json(it.first, it.second) }
                readMarks.answer(request.method.value, path, bodies.last())?.let { return@MockEngine json(it.first, it.second) }
                notifications.answer(request.method.value, path, bodies.last())?.let { return@MockEngine json(it.first, it.second) }
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

    fun session(beforeSignOut: com.usuallycoding.opensecretsanta.data.session.SignOutHook? = null): SessionManager {
        lateinit var created: SessionManager
        val api = ApiClient(createHttpClient(engine), "https://api.example.org", object : AuthContext {
            override val token get() = created.token
            override fun rejected(error: ApiError) = created.rejected(error)
        })
        this.api = api
        created = SessionManager(lazyOf(api), MemoryTokenStore(), CoroutineScope(Dispatchers.Unconfined), beforeSignOut)
        return created
    }
}

fun immediateScope() = CoroutineScope(Dispatchers.Unconfined)

/**
 * A wishlist with the server's behavior: items get ids, a list holds at most [WishlistLimits.MAX_ITEMS], an empty title is
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
                    rows.size >= WishlistLimits.MAX_ITEMS -> error(HttpStatusCode.Conflict, "invalid_state", "The wishlist is full.")
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

/**
 * The gift routes with the server's behavior: details and threads exist once the group is Active or
 * Archived, the reveal only once it is Archived, a question is 1 to 1000 characters and at most [QuestionLimits.MAX_QUESTIONS_PER_SANTA],
 * an answer 1 to 2000, and anything that writes needs an Active group. The Santa's questions carry
 * no sign of who asked. A test can queue a failure that the next write answers with.
 */
class FakeGift {
    /** `pending`, `active` or `archived`, as the group's state. */
    var state = "active"
    var receiver = ReceiverDto("bob", "Bob Builder", "1 Main Street", "555-0100", "Ring twice", "bob@example.com")
    var santa = SantaRevealDto("carol", "Carol Santos")

    private val asked = mutableListOf<QuestionDto>()
    private val received = mutableListOf<QuestionDto>()
    private val failures = ArrayDeque<Pair<HttpStatusCode, String>>()

    /** The questions the person asked their receiver. */
    val myQuestions: List<QuestionDto> get() = asked

    /** The questions the person's Santa asked them. */
    val santaQuestions: List<QuestionDto> get() = received

    fun santaAsks(text: String) {
        received += QuestionDto(received.size + 1, text, "2026-12-02T09:00:00Z")
    }

    fun receiverAnswers(number: Int, text: String) {
        val index = asked.indexOfFirst { it.number == number }
        asked[index] = asked[index].copy(answer = text, answeredAt = "2026-12-03T09:00:00Z", answerUpdatedAt = "2026-12-03T09:00:00Z")
    }

    fun askedBehindTheScenes(count: Int) {
        repeat(count) { asked += QuestionDto(asked.size + 1, "Question ${asked.size + 1}", "2026-12-02T09:00:00Z") }
    }

    fun failNextWriteWith(status: HttpStatusCode, code: String, message: String = "refused", field: String? = null) {
        val extra = if (field != null) ""","field":"$field"""" else ""
        failures += status to """{"code":"$code","message":"$message"$extra}"""
    }

    private fun error(status: HttpStatusCode, code: String, message: String, field: String? = null) =
        status to """{"code":"$code","message":"$message"${if (field != null) ""","field":"$field"""" else ""}}"""

    private val started get() = state == "active" || state == "archived"

    /** The answer to a request under `/groups/{id}` about the receiver, the Santa or the questions, or null for anything else. */
    fun answer(method: String, path: String, body: String): Pair<HttpStatusCode, String>? {
        val match = Regex("/groups/\\d+/(receiver|santa)(/questions(/(\\d+)/answer)?)?").matchEntire(path) ?: return null
        val side = match.groupValues[1]
        val questions = match.groupValues[2].isNotEmpty()
        val number = match.groupValues[4].toIntOrNull()
        val notStarted = error(HttpStatusCode.Conflict, "invalid_state", "Questions start once the group is active.")
        if (method == "GET") {
            return when {
                !questions && side == "receiver" -> if (started) HttpStatusCode.OK to Json.encodeToString(receiver) else notStarted
                !questions -> if (state == "archived") HttpStatusCode.OK to Json.encodeToString(santa) else error(HttpStatusCode.Conflict, "invalid_state", "Your Santa is revealed once the group is archived.")
                !started -> notStarted
                side == "receiver" -> HttpStatusCode.OK to Json.encodeToString(asked.toList())
                else -> HttpStatusCode.OK to Json.encodeToString(received.toList())
            }
        }
        // everything below writes
        failures.removeFirstOrNull()?.let { return it }
        if (state != "active") return error(HttpStatusCode.Conflict, "invalid_state", "Questions and answers can only be written while the group is active.")
        val tooLong = { text: String, max: Int -> text.isBlank() || text.trim().length > max }
        return when {
            method == "POST" && side == "receiver" && questions -> {
                val text = Json.decodeFromString<AskQuestionRequest>(body).text
                when {
                    tooLong(text, 1000) -> error(HttpStatusCode.BadRequest, "validation_failed", "Text must be 1 to 1000 characters long.", "text")
                    asked.size >= QuestionLimits.MAX_QUESTIONS_PER_SANTA -> error(HttpStatusCode.Conflict, "invalid_state", "You have asked the most questions allowed.")
                    else -> {
                        val question = QuestionDto(asked.size + 1, text.trim(), "2026-12-02T10:00:00Z")
                        asked += question
                        HttpStatusCode.Created to Json.encodeToString(question)
                    }
                }
            }
            method == "PUT" && side == "santa" && number != null -> {
                val index = received.indexOfFirst { it.number == number }
                if (index < 0) return error(HttpStatusCode.NotFound, "not_found", "Question not found.")
                val text = Json.decodeFromString<AnswerRequest>(body).text
                if (tooLong(text, 2000)) return error(HttpStatusCode.BadRequest, "validation_failed", "Text must be 1 to 2000 characters long.", "text")
                val old = received[index]
                val updated = old.copy(answer = text.trim(), answeredAt = old.answeredAt ?: "2026-12-03T10:00:00Z", answerUpdatedAt = "2026-12-03T10:00:00Z")
                received[index] = updated
                HttpStatusCode.OK to Json.encodeToString(updated)
            }
            else -> null
        }
    }
}

/**
 * The letters between a receiver and their Santa as the server keeps and answers them. The group's state is the [gift] fake's, as for
 * the questions. A test can queue a failure for the next send.
 */
class FakeLetters(private val gift: FakeGift) {
    private val sent = mutableListOf<LetterDto>()
    private val inbox = mutableListOf<LetterDto>()
    private val failures = ArrayDeque<Pair<HttpStatusCode, String>>()

    /** The letters the person has sent their Santa. */
    val mySent: List<LetterDto> get() = sent

    /** The letters the person's receiver has sent them. */
    val received: List<LetterDto> get() = inbox

    fun receiverSends(title: String, text: String) {
        inbox += LetterDto(inbox.size + 1, title, text, "2026-12-04T09:00:00Z")
    }

    fun sentBehindTheScenes(count: Int) {
        repeat(count) { sent += LetterDto(sent.size + 1, "Letter ${sent.size + 1}", "Text", "2026-12-04T09:00:00Z") }
    }

    fun failNextSendWith(status: HttpStatusCode, code: String, message: String = "refused", field: String? = null) {
        val extra = if (field != null) ""","field":"$field"""" else ""
        failures += status to """{"code":"$code","message":"$message"$extra}"""
    }

    private fun error(status: HttpStatusCode, code: String, message: String, field: String? = null) =
        status to """{"code":"$code","message":"$message"${if (field != null) ""","field":"$field"""" else ""}}"""

    private val started get() = gift.state == "active" || gift.state == "archived"

    /** The answer to a request under `/groups/{id}` about letters, or null for anything else. */
    fun answer(method: String, path: String, body: String): Pair<HttpStatusCode, String>? {
        val match = Regex("/groups/\\d+/(receiver|santa)/letters").matchEntire(path) ?: return null
        val side = match.groupValues[1]
        if (method == "GET") {
            if (!started) return error(HttpStatusCode.Conflict, "invalid_state", "Letters start once the group is active.")
            return HttpStatusCode.OK to Json.encodeToString(if (side == "santa") sent.toList() else inbox.toList())
        }
        if (method != "POST" || side != "santa") return null
        failures.removeFirstOrNull()?.let { return it }
        if (gift.state != "active") return error(HttpStatusCode.Conflict, "invalid_state", "Letters can only be sent while the group is active.")
        val request = Json.decodeFromString<SendLetterRequest>(body)
        return when {
            request.title.isBlank() || request.title.trim().length > 100 -> error(HttpStatusCode.BadRequest, "validation_failed", "Title must be 1 to 100 characters long.", "title")
            request.text.isBlank() || request.text.trim().length > 2000 -> error(HttpStatusCode.BadRequest, "validation_failed", "Text must be 1 to 2000 characters long.", "text")
            sent.size >= LetterLimits.MAX_LETTERS_PER_RECEIVER -> error(HttpStatusCode.Conflict, "invalid_state", "The letter limit has been reached.")
            else -> {
                val letter = LetterDto(sent.size + 1, request.title.trim(), request.text.trim(), "2026-12-04T10:00:00Z")
                sent += letter
                HttpStatusCode.Created to Json.encodeToString(letter)
            }
        }
    }
}

/**
 * What a Santa has seen of their receiver's things, as the server keeps and answers it: the marks of each kind, the marks the app
 * sent, and a switch that makes the server fail. The times are the ones a test puts in.
 */
class FakeReadMarks {
    var wishlist = ReadMarkDto(0, null)
    var answers = ReadMarkDto(0, null)
    var letters = ReadMarkDto(0, null)

    /** Every mark the app sent, as kind to time, in order. */
    val sent = mutableListOf<Pair<String, String>>()

    /** Makes the reads route answer 500, and the marking route too. */
    var failing = false

    private fun dto() = ReadMarksDto(wishlist, answers, letters)

    fun answer(method: String, path: String, body: String): Pair<HttpStatusCode, String>? {
        val match = Regex("/groups/\\d+/receiver/reads(/(wishlist|answers|letters))?").matchEntire(path) ?: return null
        if (failing) return HttpStatusCode.InternalServerError to """{"code":"internal_error","message":"refused"}"""
        val kind = match.groupValues[2]
        return when {
            method == "GET" && kind.isEmpty() -> HttpStatusCode.OK to Json.encodeToString(dto())
            method == "PUT" && kind.isNotEmpty() -> {
                val time = Json.decodeFromString<MarkReadRequest>(body).seenUpTo.orEmpty()
                sent += kind to time
                val marked = ReadMarkDto(0, time)
                when (kind) {
                    "wishlist" -> wishlist = marked
                    "answers" -> answers = marked
                    else -> letters = marked
                }
                HttpStatusCode.OK to Json.encodeToString(marked)
            }
            else -> null
        }
    }
}

/**
 * The administrator's group routes with the server's behavior: a group has a state that only moves
 * forward, the participants can change in a Draft and a Pending group (a Pending group creates the
 * accounts it lacks), the names can be drawn with at least 4 people, and only a Draft is deleted.
 * A group owned by someone else answers 404. A test can queue a failure for the next write.
 */
class FakeAdminGroups {
    class Group(
        val id: Long,
        var name: String,
        var description: String? = null,
        var location: String? = null,
        var date: String? = null,
        var time: String? = null,
        var state: String = "draft",
        var participants: List<String> = emptyList(),
        var budget: Int? = null,
        var currency: String? = null,
    )

    private val groups = mutableListOf<Group>()
    private val foreign = mutableSetOf<Long>()
    private var nextId = 1L
    private val failures = ArrayDeque<Pair<HttpStatusCode, String>>()

    /** The usernames that have an account, and which of them have a name and an address. */
    val accounts = mutableSetOf("alice", "bob")
    val completeProfiles = mutableSetOf("alice")

    /** The accounts that still have a temporary password. */
    val temporaryPasswords = mutableSetOf<String>()

    val ids: List<Long> get() = groups.map { it.id }

    /** Whether each `DELETE` of a group carried `confirm=true`, in the order they came. */
    val deleteRequests = mutableListOf<Boolean>()

    /** Set by the engine from the request's query before [answer] is called. */
    var confirmed = false

    /** What deleting a group takes with it, when a test says so; a drawn group always has at least its draw. */
    val impacts = mutableMapOf<Long, DeletionImpactDto>()

    private fun impactOf(group: Group): DeletionImpactDto {
        val set = impacts[group.id] ?: DeletionImpactDto(0, 0, 0, draw = false)
        return set.copy(draw = set.draw || group.state == "active" || group.state == "archived")
    }

    private fun DeletionImpactDto.isEmpty() = wishlistItems == 0 && questions == 0 && answers == 0 && !draw
    fun group(id: Long): Group = groups.first { it.id == id }

    /** The group as this administrator may see it: null when it does not exist or belongs to someone else. */
    fun visible(id: Long): Group? = groups.firstOrNull { it.id == id && it.id !in foreign }

    fun add(name: String, state: String = "draft", participants: List<String> = emptyList()): Long {
        val group = Group(nextId++, name, state = state, participants = participants)
        groups += group
        return group.id
    }

    /** A group of another administrator, which this one must never see. */
    fun addForeign(name: String): Long = add(name).also { foreign += it }

    /** Another device changed the group behind the screen. */
    fun moveBehindTheScenes(id: Long, state: String) {
        group(id).state = state
    }

    fun removeBehindTheScenes(id: Long) {
        groups.removeAll { it.id == id }
    }

    fun failNextWriteWith(status: HttpStatusCode, code: String, message: String = "refused", field: String? = null) {
        failures += error(status, code, message, field)
    }

    private fun error(status: HttpStatusCode, code: String, message: String, field: String? = null) =
        status to """{"code":"$code","message":"$message"${if (field != null) ""","field":"$field"""" else ""}}"""

    private fun dto(group: Group) = AdminGroupDto(
        group.id, "admin", group.name, group.description, group.location, group.date, group.time, group.state,
        group.participants.map { ParticipantDto(it, it in accounts, it in completeProfiles, mustChangePassword = it in accounts && it in temporaryPasswords) },
        group.budget, group.currency,
    )

    private fun response(group: Group, created: List<CreatedAccountDto> = emptyList(), emails: EmailSummaryDto? = null, deletedRules: List<DrawRuleDto> = emptyList()) =
        HttpStatusCode.OK to Json.encodeToString(GroupChangeResponse(dto(group), created, emails, deletedRules))

    // --- draw rules: kept as given (the server's checks are the server's own tests), with the answers a test asks for ---

    private val rules = mutableMapOf<Long, MutableList<DrawRuleDto>>()
    private var nextRuleId = 1L

    /** What `GET .../rules` says about few possible draws. */
    var fewDraws = false

    /** The rules of a group as the server keeps them; a test can seed them. */
    fun rulesOf(id: Long): List<DrawRuleDto> = rules[id].orEmpty()

    fun addRule(id: Long, giver: String, receiver: String, kind: String): DrawRuleDto =
        DrawRuleDto(nextRuleId++, giver, receiver, kind).also { rules.getOrPut(id) { mutableListOf() } += it }

    /** The next add of a rule is refused with this explanation, as a conflict. */
    var nextRuleConflict: String? = null

    /** The next draw of the names is refused because of the rules, with this explanation. */
    var nextDrawConflict: String? = null

    /** The answer to a request under `/admin/groups/{id}/rules`, or null when the request is about something else. */
    fun rulesAnswer(method: String, path: String, body: String): Pair<HttpStatusCode, String>? {
        val match = Regex("/admin/groups/(\\d+)/rules(?:/(\\d+))?").matchEntire(path) ?: return null
        val group = visible(match.groupValues[1].toLong()) ?: return error(HttpStatusCode.NotFound, "not_found", "Group not found.")
        val ruleId = match.groupValues[2].toLongOrNull()
        val notNow = error(HttpStatusCode.Conflict, "invalid_state", "Draw rules can only be changed while the group is Pending.")
        return when {
            group.state == "draft" -> error(HttpStatusCode.Conflict, "invalid_state", "A Draft group has no draw rules yet.")
            method == "GET" && ruleId == null -> HttpStatusCode.OK to Json.encodeToString(DrawRulesDto(rulesOf(group.id), fewDraws && group.state == "pending"))
            group.state != "pending" -> notNow
            method == "POST" && ruleId == null -> {
                nextRuleConflict?.let { message -> nextRuleConflict = null; return error(HttpStatusCode.Conflict, "rules_conflict", message) }
                val request = Json.decodeFromString<AddDrawRuleRequest>(body)
                val made = buildList {
                    add(addRule(group.id, request.giver, request.receiver, request.kind))
                    if (request.bothWays) add(addRule(group.id, request.receiver, request.giver, request.kind))
                }
                HttpStatusCode.Created to Json.encodeToString(made)
            }
            method == "DELETE" && ruleId != null -> if (rules[group.id]?.removeAll { it.id == ruleId } == true) {
                HttpStatusCode.NoContent to ""
            } else {
                error(HttpStatusCode.NotFound, "not_found", "Rule not found.")
            }
            else -> null
        }
    }

    /** What the server says about the emails of a draw; null (the default) is a server with email off. */
    var drawEmails: EmailSummaryDto? = null

    private fun createMissingAccounts(group: Group): List<CreatedAccountDto> {
        val created = group.participants.filter { it !in accounts }
        accounts += created
        return created.map { CreatedAccountDto(it, "Temp-$it-1234") }
    }

    private fun blankName(name: String?) = name.isNullOrBlank() || name.trim().length > 100

    /** The answer to a request under `/admin/groups`, or null when the request is about something else. */
    fun answer(method: String, path: String, body: String): Pair<HttpStatusCode, String>? {
        val match = Regex("/admin/groups(?:/(\\d+)(?:/(participants|pending|activate|archive))?)?").matchEntire(path) ?: return null
        val id = match.groupValues[1].toLongOrNull()
        val action = match.groupValues[2]
        if (id == null) {
            return when (method) {
                "GET" -> HttpStatusCode.OK to Json.encodeToString(groups.filter { it.id !in foreign }.map(::dto))
                "POST" -> {
                    failures.removeFirstOrNull()?.let { return it }
                    val request = Json.decodeFromString<CreateGroupRequest>(body)
                    if (blankName(request.name)) return error(HttpStatusCode.BadRequest, "validation_failed", "Name must be 1 to 100 characters long.", "name")
                    val group = Group(nextId++, request.name.trim(), request.description, request.location, request.date, request.time, participants = request.participants, budget = request.budget, currency = request.currency)
                    groups += group
                    HttpStatusCode.Created to Json.encodeToString(dto(group))
                }
                else -> null
            }
        }
        val group = groups.firstOrNull { it.id == id && it.id !in foreign }
            ?: return error(HttpStatusCode.NotFound, "not_found", "Group not found.")
        if (method == "GET" && action.isEmpty()) return HttpStatusCode.OK to Json.encodeToString(dto(group))
        // everything below changes the group
        failures.removeFirstOrNull()?.let { return it }
        val notNow = error(HttpStatusCode.Conflict, "invalid_state", "The group is not in a state that allows this.")
        return when {
            method == "PUT" && action.isEmpty() -> {
                if (group.state == "archived") return notNow
                val request = Json.decodeFromString<GroupDetailsRequest>(body)
                if (blankName(request.name)) return error(HttpStatusCode.BadRequest, "validation_failed", "Name must be 1 to 100 characters long.", "name")
                group.name = request.name.trim()
                group.description = request.description; group.location = request.location
                group.date = request.date; group.time = request.time
                group.budget = request.budget; group.currency = request.currency
                HttpStatusCode.OK to Json.encodeToString(dto(group))
            }
            method == "PUT" && action == "participants" -> {
                if (group.state != "draft" && group.state != "pending") return notNow
                val names = Json.decodeFromString<ParticipantsRequest>(body).participants
                if (names.any { !Regex("[a-z0-9._-]{3,32}").matches(it.lowercase()) }) {
                    return error(HttpStatusCode.BadRequest, "validation_failed", "Username must be 3 to 32 characters long.", "participants")
                }
                group.participants = names.map { it.lowercase() }.distinct()
                val gone = rulesOf(group.id).filter { it.giver !in group.participants || it.receiver !in group.participants }
                rules[group.id]?.removeAll(gone)
                response(group, if (group.state == "pending") createMissingAccounts(group) else emptyList(), deletedRules = gone)
            }
            method == "DELETE" && action.isEmpty() -> {
                deleteRequests += confirmed
                val impact = impactOf(group)
                if (!confirmed && !impact.isEmpty()) {
                    HttpStatusCode.Conflict to Json.encodeToString(
                        ErrorResponse("confirmation_required", "Deleting this group also deletes data. Confirm to go on.", impact = impact),
                    )
                } else {
                    groups.remove(group)
                    HttpStatusCode.NoContent to ""
                }
            }
            method == "POST" && action == "pending" -> if (group.state == "draft") {
                group.state = "pending"
                response(group, createMissingAccounts(group))
            } else {
                notNow
            }
            method == "POST" && action == "activate" -> when {
                group.state != "pending" -> notNow
                group.participants.size < 4 -> error(HttpStatusCode.BadRequest, "validation_failed", "A group needs at least 4 participants.", "participants")
                nextDrawConflict != null -> error(HttpStatusCode.Conflict, "rules_conflict", nextDrawConflict!!).also { nextDrawConflict = null }
                else -> {
                    group.state = "active"
                    response(group, emails = drawEmails)
                }
            }
            method == "POST" && action == "archive" -> if (group.state == "active") {
                group.state = "archived"
                response(group)
            } else {
                notNow
            }
            else -> null
        }
    }
}

/**
 * The admin user routes with the server's behavior: the administrator manages the users added here
 * and themselves, a user outside that scope answers 404, the temporary password is returned only
 * while it is still the temporary one, and a reset makes a new one and requires a change.
 */
class FakeAdminUsers(val me: String = "admin") {
    class Row(
        val username: String,
        var mustChange: Boolean = false,
        var temporary: String? = null,
        /** The server would accept deleting this user: one the administrator created that is in no group. */
        var deletable: Boolean = false,
    )

    private val rows = mutableListOf(Row(me))
    private val managed = mutableSetOf(me)
    private var resets = 0
    private val failures = ArrayDeque<Pair<HttpStatusCode, String>>()

    val usernames: List<String> get() = rows.filter { it.username in managed }.map { it.username }
    fun row(username: String): Row = rows.first { it.username == username }

    /** A user the administrator manages; one who still has to change a temporary password has [temporary]. */
    fun add(username: String, temporary: String? = null): Row =
        Row(username, mustChange = temporary != null, temporary = temporary).also {
            rows += it
            managed += username
        }

    /** A user the administrator created who is in no group, so the administrator may delete them. */
    fun addUnassigned(username: String): Row =
        add(username).also { it.deletable = true }

    /** The user joined a group behind the screen's back: still managed, no longer deletable. */
    fun joinsAGroup(username: String) {
        row(username).deletable = false
    }

    /** A user that exists but is not the administrator's to manage. */
    fun addOutsider(username: String): Row = Row(username).also { rows += it }

    /** The user dropped out of the administrator's scope behind the screen's back. */
    fun leaveScope(username: String) {
        managed -= username
    }

    /** The person changed their own password: no temporary one is returned any more. */
    fun changesOwnPassword(username: String) {
        row(username).apply { mustChange = false; temporary = null }
    }

    fun failNextDeleteWith(status: HttpStatusCode, code: String, message: String = "refused") = failNextResetWith(status, code, message)

    /** The administrator's own password, which a hand-over must carry. */
    var ownPassword = "own-secret"

    /** The people the role was handed to, in order, as the server accepted them. */
    val handedOver = mutableListOf<String>()

    fun failNextResetWith(status: HttpStatusCode, code: String, message: String = "refused") {
        failures += status to """{"code":"$code","message":"$message"}"""
    }

    private fun dto(row: Row) = ManagedUserDto(
        AccountDto(row.username, row.username == me, row.mustChange),
        row.temporary,
        row.deletable,
    )

    /** The answer to a request under `/admin/users`, or null when the request is about something else. */
    fun answer(method: String, path: String, lastBody: String = ""): Pair<HttpStatusCode, String>? {
        val match = Regex("/admin/users(?:/([a-z0-9._-]+)(?:/(reset-password|hand-over-admin))?)?").matchEntire(path) ?: return null
        val username = match.groupValues[1]
        val reset = match.groupValues[2] == "reset-password"
        val handOver = match.groupValues[2] == "hand-over-admin"
        if (username.isEmpty()) {
            return if (method == "GET") HttpStatusCode.OK to Json.encodeToString(rows.filter { it.username in managed }.map(::dto)) else null
        }
        val row = rows.firstOrNull { it.username == username && it.username in managed }
            ?: return HttpStatusCode.NotFound to """{"code":"not_found","message":"User not found."}"""
        return when {
            method == "POST" && handOver -> {
                failures.removeFirstOrNull()?.let { return it }
                val given = Json.decodeFromString<HandOverAdminRequest>(lastBody).password
                when {
                    row.username == me || row.mustChange ->
                        HttpStatusCode.Conflict to """{"code":"invalid_state","message":"That person cannot take over."}"""
                    given != ownPassword ->
                        HttpStatusCode.BadRequest to """{"code":"validation_failed","message":"That password is not right.","field":"password"}"""
                    else -> {
                        handedOver += row.username
                        HttpStatusCode.NoContent to ""
                    }
                }
            }
            method == "GET" && !reset && !handOver -> HttpStatusCode.OK to Json.encodeToString(dto(row))
            method == "DELETE" && !reset && !handOver -> {
                failures.removeFirstOrNull()?.let { return it }
                if (!row.deletable) return HttpStatusCode.Conflict to """{"code":"invalid_state","message":"Only an account you created that is in no group can be deleted."}"""
                rows.remove(row)
                managed -= row.username
                HttpStatusCode.NoContent to ""
            }
            method == "POST" && reset -> {
                failures.removeFirstOrNull()?.let { return it }
                row.temporary = "NewTemp-${row.username}-${++resets}"
                row.mustChange = true
                HttpStatusCode.OK to Json.encodeToString(ResetPasswordResponse(row.username, row.temporary!!))
            }
            else -> null
        }
    }
}

/**
 * The owner's read-only routes with the server's behavior: they answer 404 for a group that is not
 * this administrator's or a username that does not take part, and 409 `invalid_state` when the group's
 * state has nothing to read (wishlists need a started group, the draw and the threads an Active or
 * Archived one). Nothing here writes, and reading changes nothing. A test can queue a failure.
 */
class FakeAdminOversight(private val groups: FakeAdminGroups) {
    private val wishlists = mutableMapOf<Pair<Long, String>, List<SharedWishlistItemDto>>()
    private val deliveries = mutableMapOf<Pair<Long, String>, DeliveryInfoDto>()
    private val letterBoxes = mutableMapOf<Long, List<AdminLetterBoxDto>>()
    private val pairs = mutableMapOf<Long, List<AssignmentDto>>()
    private val threads = mutableMapOf<Long, List<AdminQuestionThreadDto>>()
    private val failures = ArrayDeque<Pair<HttpStatusCode, String>>()

    fun draw(groupId: Long, vararg santaToReceiver: Pair<String, String>) {
        pairs[groupId] = santaToReceiver.map { AssignmentDto(it.first, it.second) }
    }

    fun wishlist(groupId: Long, username: String, vararg titles: String) {
        wishlists[groupId to username] = titles.map { SharedWishlistItemDto(it, null, null, "2026-12-01T10:00:00Z", "2026-12-01T10:00:00Z") }
    }

    fun wishlistItems(groupId: Long, username: String, items: List<SharedWishlistItemDto>) {
        wishlists[groupId to username] = items
    }

    fun deliveryInfo(groupId: Long, username: String, info: DeliveryInfoDto) {
        deliveries[groupId to username] = info
    }

    fun letterBoxes(groupId: Long, vararg box: AdminLetterBoxDto) {
        letterBoxes[groupId] = box.toList()
    }

    fun threads(groupId: Long, vararg thread: AdminQuestionThreadDto) {
        threads[groupId] = thread.toList()
    }

    fun failNextWith(status: HttpStatusCode, code: String, message: String = "refused") {
        failures += status to """{"code":"$code","message":"$message"}"""
    }

    private fun error(status: HttpStatusCode, code: String, message: String) =
        status to """{"code":"$code","message":"$message"}"""

    /** The answer to a request under `/admin/groups/{id}` about the draw, a wishlist or the threads, or null for anything else. */
    fun answer(method: String, path: String): Pair<HttpStatusCode, String>? {
        val match = Regex("/admin/groups/(\\d+)/(assignments|questions|letters|participants/([^/]+)/(wishlist|delivery-info))").matchEntire(path) ?: return null
        if (method != "GET") return null
        failures.removeFirstOrNull()?.let { return it }
        val id = match.groupValues[1].toLong()
        val group = groups.visible(id) ?: return error(HttpStatusCode.NotFound, "not_found", "Group not found.")
        val started = group.state == "active" || group.state == "archived"
        return when (match.groupValues[2]) {
            "assignments" ->
                if (started) HttpStatusCode.OK to Json.encodeToString(pairs[id].orEmpty())
                else error(HttpStatusCode.Conflict, "invalid_state", "The names have not been drawn.")
            "questions" ->
                if (started) HttpStatusCode.OK to Json.encodeToString(threads[id].orEmpty())
                else error(HttpStatusCode.Conflict, "invalid_state", "There are no questions yet.")
            "letters" ->
                if (started) HttpStatusCode.OK to Json.encodeToString(letterBoxes[id].orEmpty())
                else error(HttpStatusCode.Conflict, "invalid_state", "There are no letters yet.")
            else -> {
                val username = match.groupValues[3]
                val delivery = match.groupValues[4] == "delivery-info"
                when {
                    username !in group.participants -> error(HttpStatusCode.NotFound, "not_found", "Participant not found.")
                    group.state == "draft" -> error(HttpStatusCode.Conflict, "invalid_state", "A Draft group has nothing of this yet.")
                    delivery -> HttpStatusCode.OK to Json.encodeToString(deliveries[id to username] ?: DeliveryInfoDto())
                    else -> HttpStatusCode.OK to Json.encodeToString(wishlists[id to username].orEmpty())
                }
            }
        }
    }
}

/**
 * The notification routes with the server's behavior: four switches that are all on until changed, a save that must
 * name all four, devices keyed by their token (a token registered again belongs to the latest caller), and an answer
 * to the settings read that says whether push is available at all. A test can queue a failure for the next save.
 */
/** The announcement route: it keeps what it was sent and answers with a summary of each channel asked for, or with a queued failure. */
class FakeAnnouncements {
    val requests = mutableListOf<AnnouncementRequest>()

    /** What the server has kept, in the order sent; a send adds to it. */
    val kept = mutableListOf<AnnouncementDto>()
    private val readFailures = ArrayDeque<Pair<HttpStatusCode, String>>()

    fun keptBehindTheScenes(count: Int) {
        repeat(count) { kept += AnnouncementDto(kept.size + 1, "Title ${kept.size + 1}", "Message ${kept.size + 1}", "2026-12-04T09:00:00Z") }
    }

    fun failNextReadWith(status: HttpStatusCode, code: String, message: String = "refused") {
        readFailures += status to """{"code":"$code","message":"$message"}"""
    }

    var emails = EmailSummaryDto(sent = 4, failed = 0, noAddress = 0)
    var push = PushSummaryDto(reached = 3, notReached = 1)
    private val failures = ArrayDeque<Pair<HttpStatusCode, String>>()

    fun failNextWith(status: HttpStatusCode, code: String, message: String, field: String? = null) {
        failures += status to """{"code":"$code","message":"$message"${if (field != null) ""","field":"$field"""" else ""}}"""
    }

    fun answer(method: String, path: String, body: String): Pair<HttpStatusCode, String>? {
        if (method == "GET" && Regex("(/admin)?/groups/\\d+/announcements").matches(path)) {
            return readFailures.removeFirstOrNull() ?: (HttpStatusCode.OK to Json.encodeToString(kept.toList()))
        }
        if (method != "POST" || !Regex("/admin/groups/\\d+/announcements").matches(path)) return null
        return failures.removeFirstOrNull() ?: run {
            val request = Json.decodeFromString<AnnouncementRequest>(body)
            requests += request
            kept += AnnouncementDto(kept.size + 1, request.title.trim(), request.body.trim(), "2026-12-04T10:00:00Z")
            HttpStatusCode.OK to Json.encodeToString(AnnouncementResponse(emails.takeIf { request.email }, push.takeIf { request.push }))
        }
    }
}

class FakeNotifications {
    var available = true
    var settings = NotificationSettingsRequest(announcements = true, wishlist = true, questions = true, answers = true)

    /** Token to platform, for the devices the server knows. */
    val devices = linkedMapOf<String, String>()

    private val failures = ArrayDeque<Pair<HttpStatusCode, String>>()

    fun failNextSaveWith(status: HttpStatusCode, code: String = "internal_error", message: String = "refused") {
        failures += status to """{"code":"$code","message":"$message"}"""
    }

    private fun dto() = NotificationSettingsDto(available, settings.announcements, settings.wishlist, settings.questions, settings.answers, settings.letters)

    fun answer(method: String, path: String, body: String): Pair<HttpStatusCode, String>? = when {
        path == ApiRoutes.NOTIFICATIONS && method == "GET" -> HttpStatusCode.OK to Json.encodeToString(dto())
        path == ApiRoutes.NOTIFICATIONS && method == "PUT" ->
            failures.removeFirstOrNull() ?: run {
                settings = Json.decodeFromString<NotificationSettingsRequest>(body)
                HttpStatusCode.OK to Json.encodeToString(dto())
            }
        path == ApiRoutes.NOTIFICATION_DEVICES && method == "PUT" -> {
            val request = Json.decodeFromString<RegisterDeviceRequest>(body)
            devices[request.token] = request.platform
            HttpStatusCode.NoContent to ""
        }
        path == ApiRoutes.NOTIFICATION_DEVICES_REMOVE && method == "POST" -> {
            devices.remove(Json.decodeFromString<RemoveDeviceRequest>(body).token)
            HttpStatusCode.NoContent to ""
        }
        else -> null
    }
}

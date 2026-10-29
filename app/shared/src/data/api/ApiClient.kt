package com.usuallycoding.opensecretsanta.data.api

import com.usuallycoding.opensecretsanta.contract.AccountDto
import com.usuallycoding.opensecretsanta.contract.AdminGroupDto
import com.usuallycoding.opensecretsanta.contract.AdminQuestionThreadDto
import com.usuallycoding.opensecretsanta.contract.AnswerRequest
import com.usuallycoding.opensecretsanta.contract.AnnouncementRequest
import com.usuallycoding.opensecretsanta.contract.AnnouncementResponse
import com.usuallycoding.opensecretsanta.contract.ApiRoutes
import com.usuallycoding.opensecretsanta.contract.AskQuestionRequest
import com.usuallycoding.opensecretsanta.contract.AssignmentDto
import com.usuallycoding.opensecretsanta.contract.ChangePasswordRequest
import com.usuallycoding.opensecretsanta.contract.ChangePasswordResponse
import com.usuallycoding.opensecretsanta.contract.CreateGroupRequest
import com.usuallycoding.opensecretsanta.contract.ErrorResponse
import com.usuallycoding.opensecretsanta.contract.GroupChangeResponse
import com.usuallycoding.opensecretsanta.contract.GroupDetailsRequest
import com.usuallycoding.opensecretsanta.contract.LoginRequest
import com.usuallycoding.opensecretsanta.contract.LoginResponse
import com.usuallycoding.opensecretsanta.contract.ManagedUserDto
import com.usuallycoding.opensecretsanta.contract.MemberGroupDto
import com.usuallycoding.opensecretsanta.contract.NotificationSettingsDto
import com.usuallycoding.opensecretsanta.contract.NotificationSettingsRequest
import com.usuallycoding.opensecretsanta.contract.ParticipantsRequest
import com.usuallycoding.opensecretsanta.contract.ProfileUpdateRequest
import com.usuallycoding.opensecretsanta.contract.QuestionDto
import com.usuallycoding.opensecretsanta.contract.ReceiverDto
import com.usuallycoding.opensecretsanta.contract.RegisterDeviceRequest
import com.usuallycoding.opensecretsanta.contract.RemoveDeviceRequest
import com.usuallycoding.opensecretsanta.contract.ReorderWishlistRequest
import com.usuallycoding.opensecretsanta.contract.ResetPasswordResponse
import com.usuallycoding.opensecretsanta.contract.SantaRevealDto
import com.usuallycoding.opensecretsanta.contract.SharedWishlistItemDto
import com.usuallycoding.opensecretsanta.contract.WishlistItemDto
import com.usuallycoding.opensecretsanta.contract.WishlistItemRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json

/** The part of the session the client needs: which token to send, and what to do when the server refuses it. */
interface AuthContext {
    val token: String?

    /** Called for an authenticated request that the server answered with [ApiError.Unauthorized] or [ApiError.PasswordChangeRequired]. */
    fun rejected(error: ApiError)
}

private val wireJson = Json { ignoreUnknownKeys = true }

private const val REQUEST_TIMEOUT_MILLIS = 30_000L
private const val CONNECT_TIMEOUT_MILLIS = 10_000L

/** The one HTTP client of the app. Without [engine] each platform uses the engine it ships with. */
fun createHttpClient(engine: HttpClientEngine? = null): HttpClient {
    val configure: io.ktor.client.HttpClientConfig<*>.() -> Unit = {
        expectSuccess = false
        install(ContentNegotiation) { json(wireJson) }
        install(HttpTimeout) {
            requestTimeoutMillis = REQUEST_TIMEOUT_MILLIS
            connectTimeoutMillis = CONNECT_TIMEOUT_MILLIS
        }
    }
    return if (engine == null) HttpClient(configure) else HttpClient(engine, configure)
}

/**
 * Calls the server's API with the contract types and routes the server itself uses. Every answer
 * becomes an [ApiResult], so screens never see HTTP details. The only credential ever sent is the
 * bearer token, and the password only inside [login] and [changePassword].
 */
class ApiClient(
    private val http: HttpClient,
    private val baseUrl: String,
    private val auth: AuthContext,
) {
    suspend fun login(username: String, password: String): ApiResult<LoginResponse> =
        call(HttpMethod.Post, ApiRoutes.LOGIN, authenticated = false, body = LoginRequest(username, password))

    suspend fun account(): ApiResult<AccountDto> = call(HttpMethod.Get, ApiRoutes.ME)

    suspend fun groups(): ApiResult<List<MemberGroupDto>> = call(HttpMethod.Get, ApiRoutes.GROUPS)

    suspend fun group(id: Long): ApiResult<MemberGroupDto> = call(HttpMethod.Get, ApiRoutes.group(id))

    /** Replaces all five profile fields; a field left out is cleared. */
    suspend fun updateProfile(request: ProfileUpdateRequest): ApiResult<AccountDto> =
        call(HttpMethod.Put, ApiRoutes.ME, body = request)

    suspend fun wishlist(groupId: Long): ApiResult<List<WishlistItemDto>> = call(HttpMethod.Get, ApiRoutes.groupWishlist(groupId))

    suspend fun addWishlistItem(groupId: Long, request: WishlistItemRequest): ApiResult<WishlistItemDto> =
        call(HttpMethod.Post, ApiRoutes.groupWishlistItems(groupId), body = request)

    suspend fun updateWishlistItem(groupId: Long, itemId: Long, request: WishlistItemRequest): ApiResult<WishlistItemDto> =
        call(HttpMethod.Put, ApiRoutes.groupWishlistItem(groupId, itemId), body = request)

    /** The server answers 204 with no body. */
    suspend fun deleteWishlistItem(groupId: Long, itemId: Long): ApiResult<Unit> =
        call(HttpMethod.Delete, ApiRoutes.groupWishlistItem(groupId, itemId))

    /** [itemIds] must be exactly the person's current items in the order they want; the answer is the list in that order. */
    suspend fun reorderWishlist(groupId: Long, itemIds: List<Long>): ApiResult<List<WishlistItemDto>> =
        call(HttpMethod.Put, ApiRoutes.groupWishlistOrder(groupId), body = ReorderWishlistRequest(itemIds))

    suspend fun receiverWishlist(groupId: Long): ApiResult<List<SharedWishlistItemDto>> =
        call(HttpMethod.Get, ApiRoutes.groupReceiverWishlist(groupId))

    /** The person's receiver as the Santa sees them: just what is needed to post a gift. */
    suspend fun receiver(groupId: Long): ApiResult<ReceiverDto> = call(HttpMethod.Get, ApiRoutes.groupReceiver(groupId))

    /** The questions the person asked their receiver, in order. */
    suspend fun receiverQuestions(groupId: Long): ApiResult<List<QuestionDto>> =
        call(HttpMethod.Get, ApiRoutes.groupReceiverQuestions(groupId))

    /** Asks the receiver a question; the answer is the stored question with its number. */
    suspend fun askQuestion(groupId: Long, text: String): ApiResult<QuestionDto> =
        call(HttpMethod.Post, ApiRoutes.groupReceiverQuestions(groupId), body = AskQuestionRequest(text))

    /** The questions the person's Santa asked, in order, with nothing that names the Santa. */
    suspend fun santaQuestions(groupId: Long): ApiResult<List<QuestionDto>> =
        call(HttpMethod.Get, ApiRoutes.groupSantaQuestions(groupId))

    /** Sets or replaces the person's answer to question [number]; the answer is the question as stored. */
    suspend fun answerQuestion(groupId: Long, number: Int, text: String): ApiResult<QuestionDto> =
        call(HttpMethod.Put, ApiRoutes.groupSantaQuestionAnswer(groupId, number), body = AnswerRequest(text))

    /** Who the person's Santa was; the server only answers once the group is archived. */
    suspend fun santa(groupId: Long): ApiResult<SantaRevealDto> = call(HttpMethod.Get, ApiRoutes.groupSanta(groupId))

    // --- notifications ---------------------------------------------------------------------------

    /** The person's four switches, and whether the server can send push at all. */
    suspend fun notificationSettings(): ApiResult<NotificationSettingsDto> = call(HttpMethod.Get, ApiRoutes.NOTIFICATIONS)

    /** Replaces all four switches; the answer is what the server saved. */
    suspend fun saveNotificationSettings(request: NotificationSettingsRequest): ApiResult<NotificationSettingsDto> =
        call(HttpMethod.Put, ApiRoutes.NOTIFICATIONS, body = request)

    /** Tells the server this device's push token belongs to the signed-in person; [platform] is `android` or `web`. */
    suspend fun registerDevice(token: String, platform: String): ApiResult<Unit> =
        call(HttpMethod.Put, ApiRoutes.NOTIFICATION_DEVICES, body = RegisterDeviceRequest(token, platform))

    /** Removes this device's push token; the server answers 204 whether or not it knew it. */
    suspend fun removeDevice(token: String): ApiResult<Unit> =
        call(HttpMethod.Post, ApiRoutes.NOTIFICATION_DEVICES_REMOVE, body = RemoveDeviceRequest(token))

    // --- administrator: groups --------------------------------------------------------------

    /** The administrator's own groups, in the order the server returns them. */
    suspend fun adminGroups(): ApiResult<List<AdminGroupDto>> = call(HttpMethod.Get, ApiRoutes.ADMIN_GROUPS)

    suspend fun adminGroup(id: Long): ApiResult<AdminGroupDto> = call(HttpMethod.Get, ApiRoutes.adminGroup(id))

    /** Creates a Draft owned by the caller. */
    suspend fun createGroup(request: CreateGroupRequest): ApiResult<AdminGroupDto> =
        call(HttpMethod.Post, ApiRoutes.ADMIN_GROUPS, body = request)

    /** Replaces the editable details; a field left out is cleared. */
    suspend fun updateGroup(id: Long, request: GroupDetailsRequest): ApiResult<AdminGroupDto> =
        call(HttpMethod.Put, ApiRoutes.adminGroup(id), body = request)

    /** Replaces the whole participant list; in a Pending group the answer lists the accounts it created. */
    suspend fun setParticipants(id: Long, usernames: List<String>): ApiResult<GroupChangeResponse> =
        call(HttpMethod.Put, ApiRoutes.adminGroupParticipants(id), body = ParticipantsRequest(usernames))

    /**
     * The server answers 204 with no body. A group with a draw, wishlist items, questions or answers is only deleted with
     * [confirm]; without it the server answers `confirmation_required` and says what would go ([ApiError.ConfirmationRequired]).
     */
    suspend fun deleteGroup(id: Long, confirm: Boolean = false): ApiResult<Unit> =
        call(HttpMethod.Delete, ApiRoutes.adminGroup(id) + if (confirm) "?${ApiRoutes.CONFIRM}=true" else "")

    /** Draft to Pending; the answer lists the accounts it created, with their temporary passwords. */
    suspend fun makePending(id: Long): ApiResult<GroupChangeResponse> =
        call(HttpMethod.Post, ApiRoutes.adminGroupPending(id))

    /** Pending to Active: draws the names and freezes the participants. */
    suspend fun activate(id: Long): ApiResult<GroupChangeResponse> =
        call(HttpMethod.Post, ApiRoutes.adminGroupActivate(id))

    suspend fun archive(id: Long): ApiResult<GroupChangeResponse> =
        call(HttpMethod.Post, ApiRoutes.adminGroupArchive(id))

    /** Sends a Pending or Active group's participants a message of the owner's, by email, push notification or both. */
    suspend fun announce(id: Long, request: AnnouncementRequest): ApiResult<AnnouncementResponse> =
        call(HttpMethod.Post, ApiRoutes.adminGroupAnnouncements(id), body = request)

    // --- administrator: oversight (owner only, read-only) --------------------------------------

    /** Every Santa and receiver pair of an Active or Archived group the caller owns. */
    suspend fun adminAssignments(id: Long): ApiResult<List<AssignmentDto>> =
        call(HttpMethod.Get, ApiRoutes.adminGroupAssignments(id))

    /** One participant's wishlist for the group, in their order and without item ids. */
    suspend fun adminParticipantWishlist(id: Long, username: String): ApiResult<List<SharedWishlistItemDto>> =
        call(HttpMethod.Get, ApiRoutes.adminGroupParticipantWishlist(id, username))

    /** Every question thread of an Active or Archived group, naming both people in each. */
    suspend fun adminQuestions(id: Long): ApiResult<List<AdminQuestionThreadDto>> =
        call(HttpMethod.Get, ApiRoutes.adminGroupQuestions(id))

    // --- administrator: users -----------------------------------------------------------------

    /** The users the administrator manages (people in their non-Draft groups, and themselves). */
    suspend fun adminUsers(): ApiResult<List<ManagedUserDto>> = call(HttpMethod.Get, ApiRoutes.ADMIN_USERS)

    /** One managed user; the answer carries the temporary password only while it is still the temporary one. */
    suspend fun adminUser(username: String): ApiResult<ManagedUserDto> = call(HttpMethod.Get, ApiRoutes.adminUser(username))

    /** Gives a managed user a new temporary password, which they must change at their next sign-in. */
    suspend fun resetPassword(username: String): ApiResult<ResetPasswordResponse> =
        call(HttpMethod.Post, ApiRoutes.adminUserResetPassword(username))

    suspend fun changePassword(currentPassword: String, newPassword: String): ApiResult<ChangePasswordResponse> =
        call(HttpMethod.Put, ApiRoutes.ME_PASSWORD, body = ChangePasswordRequest(currentPassword, newPassword))

    private suspend inline fun <reified T> call(
        method: HttpMethod,
        path: String,
        authenticated: Boolean = true,
        body: Any? = null,
    ): ApiResult<T> {
        val response = try {
            http.request(baseUrl.trimEnd('/') + path) {
                this.method = method
                if (authenticated) auth.token?.let { header(HttpHeaders.Authorization, "Bearer $it") }
                if (body != null) {
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) { // not just Exception: on the web a failed fetch is a JsException, which only extends Throwable
            return ApiResult.Failure(ApiError.Network)
        }
        val result = if (response.status.isSuccess()) {
            try {
                ApiResult.Success(response.body<T>())
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                ApiResult.Failure(ApiError.Unexpected)
            }
        } else {
            ApiResult.Failure(response.toError())
        }
        if (authenticated && result is ApiResult.Failure &&
            (result.error == ApiError.Unauthorized || result.error == ApiError.PasswordChangeRequired)
        ) {
            auth.rejected(result.error)
        }
        return result
    }

    private suspend fun HttpResponse.toError(): ApiError {
        val code = status.value
        if (code == 401) return ApiError.Unauthorized
        if (code == 429) return ApiError.RateLimited(headers[HttpHeaders.RetryAfter]?.trim()?.toIntOrNull())
        val error = try {
            wireJson.decodeFromString(ErrorResponse.serializer(), bodyAsText())
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            return ApiError.Unexpected
        }
        return when {
            code == 403 && error.code == PASSWORD_CHANGE_REQUIRED -> ApiError.PasswordChangeRequired
            code == 400 -> ApiError.Validation(error.field, error.message)
            code == 409 && error.code == CONFIRMATION_REQUIRED && error.impact != null -> ApiError.ConfirmationRequired(error.impact!!)
            else -> ApiError.Rejected(code, error.code, error.message)
        }
    }

    private companion object {
        const val PASSWORD_CHANGE_REQUIRED = "password_change_required"
        const val CONFIRMATION_REQUIRED = "confirmation_required"
    }
}

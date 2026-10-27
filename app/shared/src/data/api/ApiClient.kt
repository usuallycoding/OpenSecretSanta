package com.usuallycoding.opensecretsanta.data.api

import com.usuallycoding.opensecretsanta.contract.AccountDto
import com.usuallycoding.opensecretsanta.contract.ApiRoutes
import com.usuallycoding.opensecretsanta.contract.ChangePasswordRequest
import com.usuallycoding.opensecretsanta.contract.ChangePasswordResponse
import com.usuallycoding.opensecretsanta.contract.ErrorResponse
import com.usuallycoding.opensecretsanta.contract.LoginRequest
import com.usuallycoding.opensecretsanta.contract.LoginResponse
import com.usuallycoding.opensecretsanta.contract.MemberGroupDto
import com.usuallycoding.opensecretsanta.contract.ProfileUpdateRequest
import com.usuallycoding.opensecretsanta.contract.ReorderWishlistRequest
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
            else -> ApiError.Rejected(code, error.code, error.message)
        }
    }

    private companion object {
        const val PASSWORD_CHANGE_REQUIRED = "password_change_required"
    }
}

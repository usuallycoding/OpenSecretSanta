package com.usuallycoding.opensecretsanta.server.presentation

import com.usuallycoding.opensecretsanta.contract.ApiRoutes
import com.usuallycoding.opensecretsanta.contract.MarkReadRequest
import com.usuallycoding.opensecretsanta.contract.ReadMarkDto
import com.usuallycoding.opensecretsanta.contract.ReadMarksDto
import com.usuallycoding.opensecretsanta.domain.error.DomainException
import com.usuallycoding.opensecretsanta.domain.gift.GetMyReadMarks
import com.usuallycoding.opensecretsanta.domain.gift.MarkRead
import com.usuallycoding.opensecretsanta.domain.gift.ReadKind
import com.usuallycoding.opensecretsanta.domain.gift.ReadMark
import com.usuallycoding.opensecretsanta.domain.gift.ReadMarks
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import java.time.Instant
import java.time.format.DateTimeParseException

private fun ReadMark.toDto() = ReadMarkDto(unread, seenUpToMillis?.let { Instant.ofEpochMilli(it).toString() })

private fun ReadMarks.toDto() = ReadMarksDto(wishlist.toDto(), answers.toDto(), letters.toDto())

/**
 * What a Santa has seen of their receiver's wishlist, answers and letters. Named by relationship like the routes beside it: under
 * `/receiver/...` the caller is the Santa, and the marks returned are the caller's own and nobody else's.
 */
fun Route.readMarkRoutes(getMarks: GetMyReadMarks, markRead: MarkRead) {
    get(ApiRoutes.GROUP_RECEIVER_READS) {
        val user = call.currentUser()
        call.respond(blocking { getMarks(user, call.groupId()) }.toDto())
    }
    put(ApiRoutes.GROUP_RECEIVER_READ) {
        val user = call.currentUser()
        val groupId = call.groupId()
        val kind = ReadKind.of(call.parameters["kind"]) ?: throw DomainException.NotFound("Not found.")
        val request = call.receive<MarkReadRequest>()
        val seenUpTo = try {
            Instant.parse(request.seenUpTo ?: throw DomainException.ValidationFailed("seenUpTo", "The time seen up to is required."))
        } catch (e: DateTimeParseException) {
            throw DomainException.ValidationFailed("seenUpTo", "The time seen up to must be an ISO-8601 instant such as 2026-12-24T18:30:00Z.")
        }
        call.respond(blocking { markRead(user, groupId, kind, seenUpTo.toEpochMilli()) }.toDto())
    }
}

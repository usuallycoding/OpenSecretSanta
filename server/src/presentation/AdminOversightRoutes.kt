package com.usuallycoding.opensecretsanta.server.presentation

import com.usuallycoding.opensecretsanta.contract.AdminQuestionThreadDto
import com.usuallycoding.opensecretsanta.contract.ApiRoutes
import com.usuallycoding.opensecretsanta.contract.QuestionDto
import com.usuallycoding.opensecretsanta.contract.SharedWishlistItemDto
import com.usuallycoding.opensecretsanta.domain.gift.AdminQuestionThread
import com.usuallycoding.opensecretsanta.domain.gift.AskedQuestion
import com.usuallycoding.opensecretsanta.domain.gift.ListQuestionThreads
import com.usuallycoding.opensecretsanta.domain.wishlist.GetParticipantWishlist
import com.usuallycoding.opensecretsanta.domain.wishlist.SharedWishlistItem
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import java.time.Instant

private fun iso(millis: Long?): String? = millis?.let { Instant.ofEpochMilli(it).toString() }

private fun SharedWishlistItem.toDto() = SharedWishlistItemDto(title, note, url, iso(createdAtMillis)!!, iso(updatedAtMillis)!!)

private fun AskedQuestion.toDto() = QuestionDto(number, text, iso(askedAtMillis)!!, answer, iso(answeredAtMillis), iso(answerUpdatedAtMillis))

private fun AdminQuestionThread.toDto() = AdminQuestionThreadDto(santa.value, receiver.value, questions.map { it.toDto() })

/**
 * What a group's owner may read of the exchange: wishlists and question threads. Read-only, owner
 * only (the use cases answer anyone else as a missing group), and nothing is recorded about a read.
 */
fun Route.adminOversightRoutes(
    getParticipantWishlist: GetParticipantWishlist,
    listQuestionThreads: ListQuestionThreads,
) {
    get(ApiRoutes.ADMIN_GROUP_PARTICIPANT_WISHLIST) {
        val actor = call.currentUser()
        val username = call.parameters["username"].orEmpty()
        call.respond(blocking { getParticipantWishlist(actor, call.groupId(), username) }.map { it.toDto() })
    }
    get(ApiRoutes.ADMIN_GROUP_QUESTIONS) {
        val actor = call.currentUser()
        call.respond(blocking { listQuestionThreads(actor, call.groupId()) }.map { it.toDto() })
    }
}

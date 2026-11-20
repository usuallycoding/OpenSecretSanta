package com.usuallycoding.opensecretsanta.server.presentation

import com.usuallycoding.opensecretsanta.contract.AdminQuestionThreadDto
import com.usuallycoding.opensecretsanta.contract.ApiRoutes
import com.usuallycoding.opensecretsanta.contract.AdminLetterBoxDto
import com.usuallycoding.opensecretsanta.contract.DeliveryInfoDto
import com.usuallycoding.opensecretsanta.contract.QuestionDto
import com.usuallycoding.opensecretsanta.contract.SharedWishlistItemDto
import com.usuallycoding.opensecretsanta.domain.delivery.DeliveryInfo
import com.usuallycoding.opensecretsanta.domain.delivery.GetParticipantDeliveryInfo
import com.usuallycoding.opensecretsanta.domain.gift.AdminQuestionThread
import com.usuallycoding.opensecretsanta.domain.gift.AskedQuestion
import com.usuallycoding.opensecretsanta.domain.gift.LetterBox
import com.usuallycoding.opensecretsanta.domain.gift.ListLetterBoxes
import com.usuallycoding.opensecretsanta.domain.gift.ListQuestionThreads
import com.usuallycoding.opensecretsanta.domain.wishlist.GetParticipantWishlist
import com.usuallycoding.opensecretsanta.domain.wishlist.LinkPreview
import com.usuallycoding.opensecretsanta.domain.wishlist.LinkPreviewResolver
import com.usuallycoding.opensecretsanta.domain.wishlist.SharedWishlistItem
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import java.time.Instant

private fun iso(millis: Long?): String? = millis?.let { Instant.ofEpochMilli(it).toString() }

private fun SharedWishlistItem.toDto(previews: Map<String, LinkPreview>) =
    SharedWishlistItemDto(title, note, urls.firstOrNull(), iso(createdAtMillis)!!, iso(updatedAtMillis)!!, urls, previews.toDtoMap(urls))

private fun DeliveryInfo.toDto() = DeliveryInfoDto(fullName, email, phone, address, deliveryInfo)

private fun AskedQuestion.toDto() = QuestionDto(number, text, iso(askedAtMillis)!!, answer, iso(answeredAtMillis), iso(answerUpdatedAtMillis))

private fun LetterBox.toDto() = AdminLetterBoxDto(santa.value, receiver.value, letters.map { it.toDto() })

private fun AdminQuestionThread.toDto() = AdminQuestionThreadDto(santa.value, receiver.value, questions.map { it.toDto() })

/**
 * What a group's owner may read of the exchange: wishlists, delivery info, letters and question threads. Read-only, owner
 * only (the use cases answer anyone else as a missing group), and nothing is recorded about a read.
 */
fun Route.adminOversightRoutes(
    getParticipantWishlist: GetParticipantWishlist,
    getParticipantDeliveryInfo: GetParticipantDeliveryInfo,
    listQuestionThreads: ListQuestionThreads,
    listLetterBoxes: ListLetterBoxes,
    previews: LinkPreviewResolver,
) {
    get(ApiRoutes.ADMIN_GROUP_PARTICIPANT_WISHLIST) {
        val actor = call.currentUser()
        val username = call.parameters["username"].orEmpty()
        val items = blocking { getParticipantWishlist(actor, call.groupId(), username) }
        val previewMap = blocking { previews.lookupAndEnqueue(items.flatMap { it.urls }) }
        call.respond(items.map { it.toDto(previewMap) })
    }
    get(ApiRoutes.ADMIN_GROUP_PARTICIPANT_DELIVERY_INFO) {
        val actor = call.currentUser()
        val username = call.parameters["username"].orEmpty()
        call.respond(blocking { getParticipantDeliveryInfo(actor, call.groupId(), username) }.toDto())
    }
    get(ApiRoutes.ADMIN_GROUP_LETTERS) {
        val actor = call.currentUser()
        call.respond(blocking { listLetterBoxes(actor, call.groupId()) }.map { it.toDto() })
    }
    get(ApiRoutes.ADMIN_GROUP_QUESTIONS) {
        val actor = call.currentUser()
        call.respond(blocking { listQuestionThreads(actor, call.groupId()) }.map { it.toDto() })
    }
}

package com.usuallycoding.opensecretsanta.server.presentation

import com.usuallycoding.opensecretsanta.contract.ApiRoutes
import com.usuallycoding.opensecretsanta.contract.LetterDto
import com.usuallycoding.opensecretsanta.contract.SendLetterRequest
import com.usuallycoding.opensecretsanta.domain.gift.Letter
import com.usuallycoding.opensecretsanta.domain.gift.ListLettersFromMyReceiver
import com.usuallycoding.opensecretsanta.domain.gift.ListMySentLetters
import com.usuallycoding.opensecretsanta.domain.gift.SendLetter
import com.usuallycoding.opensecretsanta.server.notification.NotificationDispatcher
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import java.time.Instant

internal fun Letter.toDto() = LetterDto(number, title, text, Instant.ofEpochMilli(sentAtMillis).toString())

/**
 * One-way letters from a receiver to their Santa. Routes are named by relationship, as the questions' are: under `/santa/...` the
 * caller is the receiver, under `/receiver/...` the caller is the Santa, and no path or body ever contains a username.
 */
fun Route.letterRoutes(
    send: SendLetter,
    listSent: ListMySentLetters,
    listFromReceiver: ListLettersFromMyReceiver,
    notifications: NotificationDispatcher,
) {
    post(ApiRoutes.GROUP_SANTA_LETTERS) {
        val user = call.currentUser()
        val groupId = call.groupId()
        val request = call.receive<SendLetterRequest>()
        val sent = blocking { send(user, groupId, request.title, request.text) }
        notifications.letterSent(groupId, user.username)
        call.respond(HttpStatusCode.Created, sent.toDto())
    }
    get(ApiRoutes.GROUP_SANTA_LETTERS) {
        val user = call.currentUser()
        call.respond(blocking { listSent(user, call.groupId()) }.map { it.toDto() })
    }
    get(ApiRoutes.GROUP_RECEIVER_LETTERS) {
        val user = call.currentUser()
        call.respond(blocking { listFromReceiver(user, call.groupId()) }.map { it.toDto() })
    }
}

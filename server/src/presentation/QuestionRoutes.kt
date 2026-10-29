package com.usuallycoding.opensecretsanta.server.presentation

import com.usuallycoding.opensecretsanta.server.notification.NotificationDispatcher
import com.usuallycoding.opensecretsanta.contract.AnswerRequest
import com.usuallycoding.opensecretsanta.contract.ApiRoutes
import com.usuallycoding.opensecretsanta.contract.AskQuestionRequest
import com.usuallycoding.opensecretsanta.contract.QuestionDto
import com.usuallycoding.opensecretsanta.domain.error.DomainException
import com.usuallycoding.opensecretsanta.domain.gift.AnswerQuestion
import com.usuallycoding.opensecretsanta.domain.gift.AskQuestion
import com.usuallycoding.opensecretsanta.domain.gift.AskedQuestion
import com.usuallycoding.opensecretsanta.domain.gift.ListMyQuestions
import com.usuallycoding.opensecretsanta.domain.gift.ListQuestionsFromMySanta
import com.usuallycoding.opensecretsanta.domain.gift.ReceivedQuestion
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import java.time.Instant

private fun iso(millis: Long?): String? = millis?.let { Instant.ofEpochMilli(it).toString() }

private fun AskedQuestion.toDto() = QuestionDto(number, text, iso(askedAtMillis)!!, answer, iso(answeredAtMillis), iso(answerUpdatedAtMillis))

private fun ReceivedQuestion.toDto() = QuestionDto(number, text, iso(askedAtMillis)!!, answer, iso(answeredAtMillis), iso(answerUpdatedAtMillis))

/**
 * The conversation between a Santa and their receiver. Routes are named by relationship: under
 * `/receiver/...` the caller is the Santa, under `/santa/...` the caller is the receiver, and no
 * path or body ever contains a username.
 */
fun Route.questionRoutes(
    ask: AskQuestion,
    listMine: ListMyQuestions,
    listFromSanta: ListQuestionsFromMySanta,
    answer: AnswerQuestion,
    notifications: NotificationDispatcher,
) {
    get(ApiRoutes.GROUP_RECEIVER_QUESTIONS) {
        val user = call.currentUser()
        call.respond(blocking { listMine(user, call.groupId()) }.map { it.toDto() })
    }
    post(ApiRoutes.GROUP_RECEIVER_QUESTIONS) {
        val user = call.currentUser()
        val groupId = call.groupId()
        val request = call.receive<AskQuestionRequest>()
        val asked = blocking { ask(user, groupId, request.text) }
        notifications.questionAsked(groupId, user.username)
        call.respond(HttpStatusCode.Created, asked.toDto())
    }
    get(ApiRoutes.GROUP_SANTA_QUESTIONS) {
        val user = call.currentUser()
        call.respond(blocking { listFromSanta(user, call.groupId()) }.map { it.toDto() })
    }
    put(ApiRoutes.GROUP_SANTA_QUESTION_ANSWER) {
        val user = call.currentUser()
        val groupId = call.groupId()
        val number = call.parameters["number"]?.toIntOrNull() ?: throw DomainException.NotFound("Question not found.")
        val request = call.receive<AnswerRequest>()
        val answered = blocking { answer(user, groupId, number, request.text) }
        notifications.questionAnswered(groupId, user.username)
        call.respond(answered.toDto())
    }
}

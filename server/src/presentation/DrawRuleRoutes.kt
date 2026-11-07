package com.usuallycoding.opensecretsanta.server.presentation

import com.usuallycoding.opensecretsanta.contract.AddDrawRuleRequest
import com.usuallycoding.opensecretsanta.contract.ApiRoutes
import com.usuallycoding.opensecretsanta.contract.DrawRuleDto
import com.usuallycoding.opensecretsanta.contract.DrawRulesDto
import com.usuallycoding.opensecretsanta.domain.error.DomainException
import com.usuallycoding.opensecretsanta.domain.group.AddDrawRules
import com.usuallycoding.opensecretsanta.domain.group.DeleteDrawRule
import com.usuallycoding.opensecretsanta.domain.group.DrawRule
import com.usuallycoding.opensecretsanta.domain.group.ListDrawRules
import com.usuallycoding.opensecretsanta.domain.group.RuleKind
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post

fun DrawRule.toDto() = DrawRuleDto(id, giver.value, receiver.value, kind.storedName)

/** Owner-only routes to read, add and delete a group's draw rules. They appear in no participant route. */
fun Route.drawRuleRoutes(listRules: ListDrawRules, addRules: AddDrawRules, deleteRule: DeleteDrawRule) {
    get(ApiRoutes.ADMIN_GROUP_RULES) {
        val actor = call.currentUser()
        val list = blocking { listRules(actor, call.groupId()) }
        call.respond(DrawRulesDto(list.rules.map { it.toDto() }, list.fewDraws))
    }
    post(ApiRoutes.ADMIN_GROUP_RULES) {
        val actor = call.currentUser()
        val id = call.groupId()
        val request = call.receive<AddDrawRuleRequest>()
        val kind = RuleKind.fromStored(request.kind) ?: throw DomainException.ValidationFailed("kind", "The kind of a rule is must or never.")
        val stored = blocking { addRules(actor, id, request.giver, request.receiver, kind, request.bothWays) }
        call.respond(HttpStatusCode.Created, stored.map { it.toDto() })
    }
    delete(ApiRoutes.ADMIN_GROUP_RULE) {
        val actor = call.currentUser()
        val id = call.groupId()
        val ruleId = call.parameters["ruleId"]?.toLongOrNull() ?: throw DomainException.NotFound("Rule not found.")
        blocking { deleteRule(actor, id, ruleId) }
        call.respond(HttpStatusCode.NoContent)
    }
}

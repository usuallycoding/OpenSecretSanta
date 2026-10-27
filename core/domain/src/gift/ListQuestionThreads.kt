package com.usuallycoding.opensecretsanta.domain.gift

import com.usuallycoding.opensecretsanta.domain.error.DomainException
import com.usuallycoding.opensecretsanta.domain.group.GroupRepository
import com.usuallycoding.opensecretsanta.domain.group.GroupState
import com.usuallycoding.opensecretsanta.domain.group.OwnedGroupLoader
import com.usuallycoding.opensecretsanta.domain.user.User
import com.usuallycoding.opensecretsanta.domain.user.Username

/** One Santa's thread as the group owner sees it: both people are named. */
data class AdminQuestionThread(
    val santa: Username,
    val receiver: Username,
    val questions: List<AskedQuestion>,
)

/**
 * The group owner reads every thread of the group, in assignment order. Read-only and unrecorded:
 * nothing a participant can see changes because the owner looked.
 */
class ListQuestionThreads(
    private val loadOwned: OwnedGroupLoader,
    private val groups: GroupRepository,
    private val questions: QuestionRepository,
) {
    operator fun invoke(actor: User, groupId: Long): List<AdminQuestionThread> {
        val group = loadOwned(actor, groupId)
        if (group.state != GroupState.ACTIVE && group.state != GroupState.ARCHIVED) {
            throw DomainException.InvalidState("Questions exist once the group is active.")
        }
        return groups.assignments(group.id).map {
            AdminQuestionThread(it.santa, it.receiver, questions.askedBy(group.id, it.santa))
        }
    }
}

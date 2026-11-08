package com.usuallycoding.opensecretsanta.domain.gift

import com.usuallycoding.opensecretsanta.domain.auth.Clock
import com.usuallycoding.opensecretsanta.domain.error.DomainException
import com.usuallycoding.opensecretsanta.domain.group.Group
import com.usuallycoding.opensecretsanta.domain.group.GroupRepository
import com.usuallycoding.opensecretsanta.domain.group.GroupState
import com.usuallycoding.opensecretsanta.domain.group.OwnedGroupLoader
import com.usuallycoding.opensecretsanta.domain.group.ParticipantGroupLoader
import com.usuallycoding.opensecretsanta.domain.user.User

private fun cleanField(field: String, label: String, value: String, maxLength: Int): String {
    val trimmed = value.trim()
    if (trimmed.isEmpty() || trimmed.length > maxLength) {
        throw DomainException.ValidationFailed(field, "$label must be 1 to $maxLength characters long.")
    }
    return trimmed
}

private fun Group.requireActive() {
    if (state != GroupState.ACTIVE) throw DomainException.InvalidState("Letters can only be sent while the group is active.")
}

private fun Group.requireDrawn() {
    if (state != GroupState.ACTIVE && state != GroupState.ARCHIVED) throw DomainException.InvalidState("Letters start once the group is active.")
}

/** A receiver sends their Santa a letter with a title and a text, both required; up to [LetterLimits.MAX_LETTERS_PER_RECEIVER] each, numbered from 1. */
class SendLetter(
    private val loadParticipantGroup: ParticipantGroupLoader,
    private val letters: LetterRepository,
    private val clock: Clock,
) {
    operator fun invoke(user: User, groupId: Long, title: String, text: String): Letter {
        val group = loadParticipantGroup(user, groupId)
        group.requireActive()
        val cleanTitle = cleanField("title", "Title", title, LetterLimits.MAX_TITLE_LENGTH)
        val cleanText = cleanField("text", "Text", text, LetterLimits.MAX_TEXT_LENGTH)
        return letters.send(group.id, user.username, cleanTitle, cleanText, clock.nowMillis(), LetterLimits.MAX_LETTERS_PER_RECEIVER)
    }
}

/** The letters the participant has sent their Santa. The result has no field that names the Santa. */
class ListMySentLetters(
    private val loadParticipantGroup: ParticipantGroupLoader,
    private val letters: LetterRepository,
) {
    operator fun invoke(user: User, groupId: Long): List<Letter> {
        val group = loadParticipantGroup(user, groupId)
        group.requireDrawn()
        return letters.sentBy(group.id, user.username)
    }
}

/** The letters the participant's receiver has sent them. */
class ListLettersFromMyReceiver(
    private val loadParticipantGroup: ParticipantGroupLoader,
    private val letters: LetterRepository,
) {
    operator fun invoke(user: User, groupId: Long): List<Letter> {
        val group = loadParticipantGroup(user, groupId)
        group.requireDrawn()
        return letters.sentToSanta(group.id, user.username)
    }
}

/**
 * The group owner reads every receiver's letters, in assignment order, naming both people. Read-only and unrecorded: nothing a
 * participant can see changes because the owner looked.
 */
class ListLetterBoxes(
    private val loadOwned: OwnedGroupLoader,
    private val groups: GroupRepository,
    private val letters: LetterRepository,
) {
    operator fun invoke(actor: User, groupId: Long): List<LetterBox> {
        val group = loadOwned(actor, groupId)
        group.requireDrawn()
        return groups.assignments(group.id).map { LetterBox(it.santa, it.receiver, letters.sentBy(group.id, it.receiver)) }
    }
}

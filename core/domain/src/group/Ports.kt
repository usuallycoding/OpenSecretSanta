package com.usuallycoding.opensecretsanta.domain.group

import com.usuallycoding.opensecretsanta.domain.user.NewUser
import com.usuallycoding.opensecretsanta.domain.user.Username

/** Source of randomness for the draw, replaceable by a fixed order in tests. */
interface Shuffler {
    fun <T> shuffled(items: List<T>): List<T>
}

/**
 * Storage for groups. Every method that changes state states what it expects the current state to
 * be and applies the whole change in one transaction, throwing `InvalidState` when the group is no
 * longer in that state and `NotFound` when it does not exist.
 */
interface GroupRepository {
    fun create(owner: Username, details: GroupDetails, participants: List<Username>): Group

    fun find(id: Long): Group?

    fun findOwnedBy(owner: Username): List<Group>

    /** Groups [username] participates in, excluding Draft ones. */
    fun findVisibleTo(username: Username): List<Group>

    /** Allowed in every state but Archived. */
    fun updateDetails(id: Long, details: GroupDetails): Group

    /** Allowed in every state: the group goes with its participants, draw, wishlist items, questions and answers. */
    fun delete(id: Long)

    /** What [delete] would take besides the details and participants; `NotFound` when there is no such group. */
    fun impact(id: Long): DeletionImpact

    /** Allowed in Draft and Pending; inserts [newUsers] in the same transaction. */
    fun replaceParticipants(id: Long, participants: List<Username>, newUsers: List<NewUser>): Group

    /** Draft to Pending, inserting [newUsers] in the same transaction. */
    fun moveToPending(id: Long, newUsers: List<NewUser>): Group

    /** Pending to Active, storing [assignments]; they must cover exactly the stored participants. */
    fun activate(id: Long, assignments: List<Assignment>): Group

    /** Active to Archived. */
    fun archive(id: Long): Group

    fun assignments(id: Long): List<Assignment>

    /** Who [santa] gives to in this group, or null when there is no such assignment. */
    fun receiverOf(groupId: Long, santa: Username): Username?

    /** Who gives to [receiver] in this group, or null when there is no such assignment. */
    fun santaOf(groupId: Long, receiver: Username): Username?
}

package com.usuallycoding.opensecretsanta.domain

import com.usuallycoding.opensecretsanta.domain.error.DomainException
import com.usuallycoding.opensecretsanta.domain.group.Assignment
import com.usuallycoding.opensecretsanta.domain.group.Group
import com.usuallycoding.opensecretsanta.domain.group.GroupDetails
import com.usuallycoding.opensecretsanta.domain.group.GroupRepository
import com.usuallycoding.opensecretsanta.domain.group.GroupState
import com.usuallycoding.opensecretsanta.domain.user.NewUser
import com.usuallycoding.opensecretsanta.domain.user.User
import com.usuallycoding.opensecretsanta.domain.user.Username

/** In-memory repository that applies the same state rules as the real one. */
class InMemoryGroups(private val users: InMemoryUsers) : GroupRepository {
    private val groups = linkedMapOf<Long, Group>()
    private val assignmentsByGroup = mutableMapOf<Long, List<Assignment>>()
    private var nextId = 1L

    /** When set, the next state-changing call throws it instead of changing anything. */
    var failNextWith: RuntimeException? = null

    private fun fail() {
        failNextWith?.let {
            failNextWith = null
            throw it
        }
    }

    private fun require(id: Long): Group = groups[id] ?: throw DomainException.NotFound("Group not found.")

    private fun expect(group: Group, vararg states: GroupState) {
        if (group.state !in states) throw DomainException.InvalidState("Group is ${group.state}, expected ${states.toList()}")
    }

    override fun create(owner: Username, details: GroupDetails, participants: List<Username>): Group =
        Group(nextId++, owner, details, GroupState.DRAFT, participants).also { groups[it.id] = it }

    override fun find(id: Long) = groups[id]
    override fun findOwnedBy(owner: Username) = groups.values.filter { it.owner == owner }
    override fun findVisibleTo(username: Username) =
        groups.values.filter { it.state != GroupState.DRAFT && username in it.participants }

    override fun updateDetails(id: Long, details: GroupDetails): Group {
        fail()
        val group = require(id)
        if (!group.state.allowsDetailChanges) throw DomainException.InvalidState("archived")
        return group.copy(details = details).also { groups[id] = it }
    }

    override fun deleteDraft(id: Long) {
        fail()
        expect(require(id), GroupState.DRAFT)
        groups.remove(id)
    }

    override fun replaceParticipants(id: Long, participants: List<Username>, newUsers: List<NewUser>): Group {
        fail()
        val group = require(id)
        expect(group, GroupState.DRAFT, GroupState.PENDING)
        users.insertAll(newUsers)
        return group.copy(participants = participants).also { groups[id] = it }
    }

    override fun moveToPending(id: Long, newUsers: List<NewUser>): Group {
        fail()
        val group = require(id)
        expect(group, GroupState.DRAFT)
        users.insertAll(newUsers)
        return group.copy(state = GroupState.PENDING).also { groups[id] = it }
    }

    override fun activate(id: Long, assignments: List<Assignment>): Group {
        fail()
        val group = require(id)
        expect(group, GroupState.PENDING)
        if (assignments.map { it.santa }.toSet() != group.participants.toSet()) {
            throw DomainException.InvalidState("participants changed")
        }
        assignmentsByGroup[id] = assignments
        return group.copy(state = GroupState.ACTIVE).also { groups[id] = it }
    }

    override fun archive(id: Long): Group {
        fail()
        val group = require(id)
        expect(group, GroupState.ACTIVE)
        return group.copy(state = GroupState.ARCHIVED).also { groups[id] = it }
    }

    override fun assignments(id: Long) = assignmentsByGroup[id].orEmpty()

    override fun receiverOf(groupId: Long, santa: Username) =
        assignments(groupId).firstOrNull { it.santa == santa }?.receiver

    override fun santaOf(groupId: Long, receiver: Username) =
        assignments(groupId).firstOrNull { it.receiver == receiver }?.santa

    /** Test helper: puts a group straight into a state with the given participants. */
    fun seed(owner: Username, state: GroupState, vararg participants: Username, name: String = "Group"): Group {
        val group = create(owner, GroupDetails(name), participants.toList()).copy(state = state)
        groups[group.id] = group
        return group
    }
}

fun InMemoryUsers.userNamed(name: String): User = find(u(name))!!

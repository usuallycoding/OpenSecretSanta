package com.usuallycoding.opensecretsanta.domain

import com.usuallycoding.opensecretsanta.domain.announcement.ListAnnouncements
import com.usuallycoding.opensecretsanta.domain.error.DomainException
import com.usuallycoding.opensecretsanta.domain.group.Group
import com.usuallycoding.opensecretsanta.domain.group.GroupState
import com.usuallycoding.opensecretsanta.domain.group.OwnedGroupLoader
import com.usuallycoding.opensecretsanta.domain.group.ParticipantGroupLoader
import com.usuallycoding.opensecretsanta.domain.user.Username
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ListAnnouncementsTest {
    private val users = InMemoryUsers()
    private val groups = InMemoryGroups(users)
    private val announcements = InMemoryAnnouncements(groups)
    private val list = ListAnnouncements(ParticipantGroupLoader(groups), OwnedGroupLoader(groups), announcements)
    private val owner = users.add("boss", admin = true).let { users.userNamed("boss") }
    private val names = listOf("alice", "bob")

    private fun u(name: String) = Username.parse(name)

    init {
        for (name in names) users.add(name)
    }

    private fun group(state: GroupState, withAnnouncements: Int = 2): Group {
        val g = groups.seed(owner.username, state, *names.map(::u).toTypedArray())
        announcements.seed(g.id, withAnnouncements)
        return g
    }

    @Test
    fun aParticipantReadsThemInTheOrderSentInPendingActiveAndArchivedGroups() {
        for (state in listOf(GroupState.PENDING, GroupState.ACTIVE, GroupState.ARCHIVED)) {
            val g = group(state)
            assertEquals(listOf(1, 2), list.forParticipant(users.userNamed("alice"), g.id).map { it.number })
        }
    }

    @Test
    fun aGroupWithNoAnnouncementsGivesAnEmptyList() {
        assertTrue(list.forParticipant(users.userNamed("alice"), group(GroupState.ACTIVE, withAnnouncements = 0).id).isEmpty())
    }

    @Test
    fun aStrangerAndADraftAndAMissingGroupAreNotFoundForAParticipant() {
        users.add("eve")
        assertFailsWith<DomainException.NotFound> { list.forParticipant(users.userNamed("eve"), group(GroupState.ACTIVE).id) }
        assertFailsWith<DomainException.NotFound> { list.forParticipant(users.userNamed("alice"), group(GroupState.DRAFT).id) }
        assertFailsWith<DomainException.NotFound> { list.forParticipant(users.userNamed("alice"), 9999) }
    }

    @Test
    fun theOwnerReadsThemWithoutTakingPart() {
        val g = group(GroupState.ACTIVE)
        assertTrue(owner.username !in g.participants)

        assertEquals(listOf(1, 2), list.forOwner(owner, g.id).map { it.number })
    }

    @Test
    fun anotherAdministratorIsNotFoundAndANonAdministratorIsForbidden() {
        val g = group(GroupState.ACTIVE)
        val other = users.add("other", admin = true).let { users.userNamed("other") }

        assertFailsWith<DomainException.NotFound> { list.forOwner(other, g.id) }
        assertFailsWith<DomainException.Forbidden> { list.forOwner(users.userNamed("alice"), g.id) }
    }

    @Test
    fun aDraftGroupHasNoAnnouncementsForTheOwner() {
        assertFailsWith<DomainException.InvalidState> { list.forOwner(owner, group(GroupState.DRAFT, withAnnouncements = 0).id) }
    }
}

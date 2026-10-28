package com.usuallycoding.opensecretsanta.ui

import com.usuallycoding.opensecretsanta.contract.CreateGroupRequest
import com.usuallycoding.opensecretsanta.contract.GroupDetailsRequest
import com.usuallycoding.opensecretsanta.contract.ParticipantsRequest
import com.usuallycoding.opensecretsanta.contract.DeletionImpactDto
import com.usuallycoding.opensecretsanta.data.api.ApiError
import com.usuallycoding.opensecretsanta.data.api.ApiResult
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The administrator's group calls against a server that behaves like the real one (see [FakeAdminGroups]). */
class AdminGroupsApiTest {
    private val server = ScriptedServer().also { it.session() }
    private val api get() = server.api
    private val fake get() = server.adminGroups

    private fun <T> success(result: ApiResult<T>): T = (result as ApiResult.Success).value

    private fun failure(result: ApiResult<*>): ApiError = (result as ApiResult.Failure).error

    @Test
    fun theOwnGroupsComeBackAndAnotherAdminsAreNotThere() = runTest {
        val mine = fake.add("Office party", participants = listOf("alice", "newcomer"))
        fake.addForeign("Not mine")

        val groups = success(api.adminGroups())

        assertEquals(listOf(mine), groups.map { it.id })
        assertEquals(listOf("alice", "newcomer"), groups.single().participants.map { it.username })
        assertEquals(listOf(true, false), groups.single().participants.map { it.hasAccount })
        assertEquals(listOf(true, false), groups.single().participants.map { it.profileComplete })
        assertEquals("GET /admin/groups", server.calls.last())
    }

    @Test
    fun aGroupIsCreatedAsADraftWithTheRequestAsBody() = runTest {
        val created = success(api.createGroup(CreateGroupRequest("Office party", "Small gifts", "Town Hall", "2026-12-24", "18:30")))

        assertEquals("draft", created.state)
        assertEquals("Town Hall", created.location)
        assertEquals("POST /admin/groups", server.calls.last())
        assertEquals(CreateGroupRequest("Office party", "Small gifts", "Town Hall", "2026-12-24", "18:30"), Json.decodeFromString(server.bodies.last()))
        assertEquals(listOf(created.id), fake.ids)
    }

    @Test
    fun aBlankNameNamesTheNameField() = runTest {
        assertEquals(ApiError.Validation("name", "Name must be 1 to 100 characters long."), failure(api.createGroup(CreateGroupRequest("  "))))
    }

    @Test
    fun aGroupCanBeReadAndItsDetailsReplaced() = runTest {
        val id = fake.add("Office party")

        success(api.updateGroup(id, GroupDetailsRequest("Office party 2", location = "Town Hall")))
        val read = success(api.adminGroup(id))

        assertEquals("Office party 2", read.name)
        assertEquals("Town Hall", read.location)
        assertEquals("PUT /admin/groups/$id", server.calls[server.calls.size - 2])
        assertEquals(GroupDetailsRequest("Office party 2", location = "Town Hall"), Json.decodeFromString(server.bodies[server.bodies.size - 2]))
    }

    @Test
    fun anArchivedGroupCannotBeEdited() = runTest {
        val id = fake.add("Old", state = "archived")

        assertEquals(409, (failure(api.updateGroup(id, GroupDetailsRequest("New"))) as ApiError.Rejected).status)
    }

    @Test
    fun aGroupOfAnotherAdminOrOneThatIsGoneIsNotFound() = runTest {
        val foreign = fake.addForeign("Not mine")
        val gone = fake.add("Gone").also { fake.removeBehindTheScenes(it) }

        assertEquals(ApiError.Rejected(404, "not_found", "Group not found."), failure(api.adminGroup(foreign)))
        assertEquals(ApiError.Rejected(404, "not_found", "Group not found."), failure(api.adminGroup(gone)))
    }

    @Test
    fun theParticipantsAreReplacedAndNothingIsCreatedInADraft() = runTest {
        val id = fake.add("Office party")

        val change = success(api.setParticipants(id, listOf("Alice", "newcomer", "alice")))

        assertEquals(listOf("alice", "newcomer"), change.group.participants.map { it.username })
        assertEquals(emptyList(), change.createdAccounts)
        assertEquals(ParticipantsRequest(listOf("Alice", "newcomer", "alice")), Json.decodeFromString(server.bodies.last()))
        assertEquals("PUT /admin/groups/$id/participants", server.calls.last())
    }

    @Test
    fun aPendingGroupCreatesTheAccountsItLacksAndReturnsTheirPasswords() = runTest {
        val id = fake.add("Office party", state = "pending", participants = listOf("alice"))

        val change = success(api.setParticipants(id, listOf("alice", "dave")))

        assertEquals(listOf("dave"), change.createdAccounts.map { it.username })
        assertEquals("Temp-dave-1234", change.createdAccounts.single().temporaryPassword)
    }

    @Test
    fun anInvalidUsernameNamesTheParticipantsField() = runTest {
        val id = fake.add("Office party")

        val error = failure(api.setParticipants(id, listOf("x")))

        assertEquals("participants", (error as ApiError.Validation).field)
    }

    @Test
    fun theRosterIsFrozenOnceTheNamesAreDrawn() = runTest {
        val id = fake.add("Office party", state = "active", participants = listOf("a1", "b2", "c3", "d4"))

        assertEquals(409, (failure(api.setParticipants(id, listOf("a1"))) as ApiError.Rejected).status)
    }

    @Test
    fun makingAGroupPendingCreatesAccountsForNewPeopleOnly() = runTest {
        val id = fake.add("Office party", participants = listOf("alice", "bob", "newcomer"))

        val change = success(api.makePending(id))

        assertEquals("pending", change.group.state)
        assertEquals(listOf("newcomer"), change.createdAccounts.map { it.username })
        assertEquals("POST /admin/groups/$id/pending", server.calls.last())
    }

    @Test
    fun theNamesAreDrawnWithFourPeopleAndRefusedWithThree() = runTest {
        val enough = fake.add("Four", state = "pending", participants = listOf("a1a", "b2b", "c3c", "d4d"))
        val few = fake.add("Three", state = "pending", participants = listOf("a1a", "b2b", "c3c"))

        assertEquals("active", success(api.activate(enough)).group.state)
        assertEquals("participants", (failure(api.activate(few)) as ApiError.Validation).field)
        assertEquals("POST /admin/groups/$few/activate", server.calls.last())
    }

    @Test
    fun anActiveGroupIsArchivedAndAStepOutOfOrderIsRefused() = runTest {
        val active = fake.add("Active", state = "active")
        val draft = fake.add("Draft")

        assertEquals("archived", success(api.archive(active)).group.state)
        assertEquals("POST /admin/groups/$active/archive", server.calls.last())
        assertEquals(409, (failure(api.archive(draft)) as ApiError.Rejected).status)
        assertEquals(409, (failure(api.activate(draft)) as ApiError.Rejected).status)
    }

    @Test
    fun aGroupWithNothingLinkedIsDeletedAndTheAnswerHasNoBody() = runTest {
        val draft = fake.add("Draft")

        assertEquals(ApiResult.Success(Unit), api.deleteGroup(draft))

        assertEquals("DELETE /admin/groups/$draft", server.calls.last())
        assertEquals(listOf(false), fake.deleteRequests)
        assertEquals(emptyList(), fake.ids)
    }

    @Test
    fun aGroupWithLinkedDataIsRefusedWithTheCountsUntilItIsConfirmed() = runTest {
        val id = fake.add("Pending", state = "pending")
        fake.impacts[id] = DeletionImpactDto(wishlistItems = 2, questions = 0, answers = 0, draw = false)

        val refused = failure(api.deleteGroup(id)) as ApiError.ConfirmationRequired

        assertEquals(DeletionImpactDto(2, 0, 0, draw = false), refused.impact)
        assertEquals(listOf(id), fake.ids)

        assertEquals(ApiResult.Success(Unit), api.deleteGroup(id, confirm = true))
        assertEquals(listOf(false, true), fake.deleteRequests)
        assertEquals(emptyList(), fake.ids)
    }

    @Test
    fun aDrawnGroupIsRefusedWithoutConfirmationEvenWithNothingElse() = runTest {
        val id = fake.add("Active", state = "active")

        val refused = failure(api.deleteGroup(id)) as ApiError.ConfirmationRequired

        assertEquals(DeletionImpactDto(0, 0, 0, draw = true), refused.impact)
    }

    @Test
    fun aQueuedFailureAnswersTheNextWriteOnly() = runTest {
        val id = fake.add("Office party")
        fake.failNextWriteWith(HttpStatusCode.Conflict, "invalid_state", "Nope.")

        assertTrue(api.makePending(id) is ApiResult.Failure)
        assertEquals("pending", success(api.makePending(id)).group.state)
    }
}

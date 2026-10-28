package com.usuallycoding.opensecretsanta.ui

import com.usuallycoding.opensecretsanta.contract.AdminQuestionThreadDto
import com.usuallycoding.opensecretsanta.contract.QuestionDto
import com.usuallycoding.opensecretsanta.data.api.ApiError
import com.usuallycoding.opensecretsanta.data.api.ApiResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The owner's read calls against a server that behaves like the real one (see [FakeAdminOversight]). */
class AdminOversightApiTest {
    private val server = ScriptedServer().also { it.session() }
    private val api get() = server.api
    private val oversight get() = server.oversight

    private fun <T> success(result: ApiResult<T>): T = (result as ApiResult.Success).value

    private fun failure(result: ApiResult<*>): ApiError = (result as ApiResult.Failure).error

    @Test
    fun theDrawListsEveryPairOfAnActiveGroup() = runTest {
        val id = server.adminGroups.add("Party", state = "active", participants = listOf("alice", "bob", "carol"))
        oversight.draw(id, "alice" to "bob", "bob" to "carol", "carol" to "alice")

        val pairs = success(api.adminAssignments(id))

        assertEquals(listOf("alice" to "bob", "bob" to "carol", "carol" to "alice"), pairs.map { it.santa to it.receiver })
        assertEquals("GET /admin/groups/$id/assignments", server.calls.last())
    }

    @Test
    fun aParticipantsWishlistComesInTheirOrder() = runTest {
        val id = server.adminGroups.add("Party", state = "pending", participants = listOf("alice", "bob"))
        oversight.wishlist(id, "bob", "Socks", "Book")

        val items = success(api.adminParticipantWishlist(id, "bob"))

        assertEquals(listOf("Socks", "Book"), items.map { it.title })
        assertEquals("GET /admin/groups/$id/participants/bob/wishlist", server.calls.last())
    }

    @Test
    fun theThreadsNameBothPeopleAndCarryTheAnswers() = runTest {
        val id = server.adminGroups.add("Party", state = "active", participants = listOf("alice", "bob"))
        oversight.threads(
            id,
            AdminQuestionThreadDto(
                "alice", "bob",
                listOf(QuestionDto(1, "Size?", "2026-12-02T09:00:00Z", "M", "2026-12-03T09:00:00Z", "2026-12-04T09:00:00Z")),
            ),
        )

        val threads = success(api.adminQuestions(id))

        assertEquals("alice", threads.single().santa)
        assertEquals("M", threads.single().questions.single().answer)
        assertEquals("GET /admin/groups/$id/questions", server.calls.last())
    }

    @Test
    fun aGroupOfAnotherAdministratorAnswersAsIfItDidNotExist() = runTest {
        val id = server.adminGroups.addForeign("Not mine")

        assertEquals(ApiError.Rejected(404, "not_found", "Group not found."), failure(api.adminAssignments(id)))
        assertEquals(ApiError.Rejected(404, "not_found", "Group not found."), failure(api.adminQuestions(id)))
        assertEquals(ApiError.Rejected(404, "not_found", "Group not found."), failure(api.adminParticipantWishlist(id, "bob")))
    }

    @Test
    fun aNameThatDoesNotTakePartAnswers404() = runTest {
        val id = server.adminGroups.add("Party", state = "active", participants = listOf("alice"))

        assertEquals(ApiError.Rejected(404, "not_found", "Participant not found."), failure(api.adminParticipantWishlist(id, "zed")))
    }

    @Test
    fun aGroupThatIsNotInAReadableStateAnswers409() = runTest {
        val pending = server.adminGroups.add("Soon", state = "pending", participants = listOf("alice"))
        val draft = server.adminGroups.add("Idea", state = "draft", participants = listOf("alice"))

        assertEquals(409, (failure(api.adminAssignments(pending)) as ApiError.Rejected).status)
        assertEquals(409, (failure(api.adminQuestions(pending)) as ApiError.Rejected).status)
        assertEquals(409, (failure(api.adminParticipantWishlist(draft, "alice")) as ApiError.Rejected).status)
    }

    @Test
    fun readingOnlyEverSendsGets() = runTest {
        val id = server.adminGroups.add("Party", state = "active", participants = listOf("alice"))
        api.adminAssignments(id)
        api.adminQuestions(id)
        api.adminParticipantWishlist(id, "alice")

        assertTrue(server.calls.all { it.startsWith("GET ") })
    }
}

package com.usuallycoding.opensecretsanta.ui

import com.usuallycoding.opensecretsanta.data.session.SessionManager
import com.usuallycoding.opensecretsanta.data.session.SessionState
import com.usuallycoding.opensecretsanta.ui.admin.AdminUserViewModel
import com.usuallycoding.opensecretsanta.ui.admin.UserNotice
import com.usuallycoding.opensecretsanta.ui.shell.ShellViewModel
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HandOverAdminViewModelTest {
    private val server = ScriptedServer()
    private val fake get() = server.adminUsers
    private var administrator = true

    init {
        server.accountBody = { """{"username":"admin","isAdmin":$administrator,"mustChangePassword":false}""" }
    }

    private suspend fun signedIn(): SessionManager = server.session().also { it.signIn("admin", "own-secret") }

    private suspend fun detail(session: SessionManager, username: String, me: String = "admin") =
        AdminUserViewModel(username, me, server.api, session, immediateScope()).also { it.refresh().join() }

    private fun handOvers() = server.calls.count { it.endsWith("/hand-over-admin") }

    @Test
    fun theOwnPageOffersNoHandover() = runTest {
        val model = detail(signedIn(), "admin")
        assertFalse(model.canHandOver)
        model.askHandOver()
        assertFalse(model.ui.value.handingOver)
    }

    @Test
    fun aPersonOnATemporaryPasswordCannotBeChosen() = runTest {
        fake.add("bobby", temporary = "Temp-bobby-1")
        val model = detail(signedIn(), "bobby")

        assertTrue(model.canHandOver)
        assertTrue(model.handOverBlocked)
        model.askHandOver()
        assertFalse(model.ui.value.handingOver)
    }

    @Test
    fun askingOpensTheConfirmationAndCancellingSendsNothing() = runTest {
        fake.add("alice")
        val model = detail(signedIn(), "alice")

        model.askHandOver()
        assertTrue(model.ui.value.handingOver)
        model.cancelHandOver()

        assertFalse(model.ui.value.handingOver)
        assertEquals(0, handOvers())
    }

    @Test
    fun nothingIsSentWithoutAPasswordOrConfirmation() = runTest {
        fake.add("alice")
        val model = detail(signedIn(), "alice")

        assertNull(model.confirmHandOver("own-secret"))
        model.askHandOver()
        assertNull(model.confirmHandOver(""))
        assertEquals(0, handOvers())
    }

    @Test
    fun aWrongPasswordIsSaidInTheOpenConfirmation() = runTest {
        fake.add("alice")
        val session = signedIn()
        val model = detail(session, "alice")
        model.askHandOver()

        model.confirmHandOver("nope")!!.join()

        assertEquals("That password is not right.", model.ui.value.handOverError)
        assertTrue(model.ui.value.handingOver)
        assertFalse(model.ui.value.busy)
        assertEquals(emptyList(), fake.handedOver)
        assertTrue((session.state.value as SessionState.SignedIn).account.isAdmin)
    }

    @Test
    fun theRightPasswordHandsOverAndTheAccountBecomesAParticipants() = runTest {
        fake.add("alice")
        val session = signedIn()
        val model = detail(session, "alice")
        model.askHandOver()
        administrator = false // what the server says once the role has moved

        model.confirmHandOver("own-secret")!!.join()

        assertEquals(listOf("alice"), fake.handedOver)
        val account = (session.state.value as SessionState.SignedIn).account
        assertEquals("admin", account.username)
        assertFalse(account.isAdmin)
        assertEquals("alice", session.handedOverTo.value)
        assertFalse(model.ui.value.handingOver)
    }

    @Test
    fun theAccountIsAParticipantsEvenWhenTheServerCannotBeAskedAfterwards() = runTest {
        fake.add("alice")
        val session = signedIn()
        val model = detail(session, "alice")
        model.askHandOver()
        // the hand-over itself goes through, and /me is not reachable afterwards
        server.accountBody = { throw RuntimeException("down") }

        model.confirmHandOver("own-secret")!!.join()

        assertFalse((session.state.value as SessionState.SignedIn).account.isAdmin)
        assertEquals("alice", session.handedOverTo.value)
    }

    @Test
    fun theMessageIsKeptByTheShellUntilDismissed() = runTest {
        fake.add("alice")
        val session = signedIn()
        val model = detail(session, "alice")
        model.askHandOver()
        administrator = false
        model.confirmHandOver("own-secret")!!.join()
        val account = (session.state.value as SessionState.SignedIn).account
        val shell = ShellViewModel(account, session, immediateScope())

        assertEquals("alice", shell.handedOverTo.value)
        assertFalse(shell.canSwitch)
        shell.dismissHandedOver()
        assertNull(shell.handedOverTo.value)
    }

    @Test
    fun aPersonWhoLeftTheScopeClosesTheDialogAndShowsAsGone() = runTest {
        fake.add("alice")
        val model = detail(signedIn(), "alice")
        model.askHandOver()
        fake.leaveScope("alice")

        model.confirmHandOver("own-secret")!!.join()

        assertFalse(model.ui.value.handingOver)
        assertEquals(404, (model.data.value.error as com.usuallycoding.opensecretsanta.data.api.ApiError.Rejected).status)
    }

    @Test
    fun aRefusalClosesTheDialogWithANoticeAndChangesNothing() = runTest {
        fake.add("alice")
        val session = signedIn()
        val model = detail(session, "alice")
        model.askHandOver()
        fake.failNextResetWith(HttpStatusCode.Conflict, "invalid_state", "That person cannot take over.")

        model.confirmHandOver("own-secret")!!.join()

        assertFalse(model.ui.value.handingOver)
        assertEquals(UserNotice.HandOverRefused, model.ui.value.notice)
        assertTrue((session.state.value as SessionState.SignedIn).account.isAdmin)
    }

    @Test
    fun tooManyAttemptsAreSaidInTheDialog() = runTest {
        fake.add("alice")
        val model = detail(signedIn(), "alice")
        model.askHandOver()
        fake.failNextResetWith(HttpStatusCode.TooManyRequests, "too_many_attempts", "Too many attempts.")

        model.confirmHandOver("own-secret")!!.join()

        assertTrue(model.ui.value.handingOver)
        assertTrue("Too many attempts" in model.ui.value.handOverError.orEmpty())
        assertFalse(model.ui.value.busy)
    }

    @Test
    fun anUnreachableServerIsSaidInTheDialogAndNothingMoves() = runTest {
        fake.add("alice")
        val session = signedIn()
        val model = detail(session, "alice")
        model.askHandOver()
        server.unreachable = true

        model.confirmHandOver("own-secret")!!.join()

        assertEquals(Strings.SERVER_UNREACHABLE, model.ui.value.handOverError)
        assertTrue(model.ui.value.handingOver)
        assertTrue((session.state.value as SessionState.SignedIn).account.isAdmin)
        assertEquals(emptyList(), fake.handedOver)
    }
}

package com.usuallycoding.opensecretsanta.ui

import com.usuallycoding.opensecretsanta.data.session.SessionState
import com.usuallycoding.opensecretsanta.ui.login.LoginProblem
import com.usuallycoding.opensecretsanta.ui.login.LoginViewModel
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LoginViewModelTest {
    private val server = ScriptedServer()
    private val session = server.session()
    private val model = LoginViewModel(session, immediateScope())

    private fun fill(user: String = "alice", password: String = "password1") {
        model.onUsernameChange(user)
        model.onPasswordChange(password)
    }

    @Test
    fun theActionNeedsBothFields() {
        assertFalse(model.state.value.canSubmit)
        model.onUsernameChange("alice")
        assertFalse(model.state.value.canSubmit)
        model.onUsernameChange("   ")
        model.onPasswordChange("password1")
        assertFalse(model.state.value.canSubmit)
        fill()
        assertTrue(model.state.value.canSubmit)
    }

    @Test
    fun blankFieldsSendNothing() = runTest {
        model.submit()?.join()
        model.onUsernameChange("alice")
        model.submit()?.join()

        assertTrue(server.calls.isEmpty())
    }

    @Test
    fun aSuccessfulSignInMovesTheSession() = runTest {
        fill()

        model.submit()?.join()

        assertEquals(SessionState.SignedIn(com.usuallycoding.opensecretsanta.data.session.Account("alice", null, false)), session.state.value)
        assertFalse(model.state.value.inProgress)
        assertNull(model.state.value.problem)
    }

    @Test
    fun theUsernameIsTrimmedButOtherwiseSentAsTyped() = runTest {
        fill(user = "  Alice ")

        model.submit()?.join()

        assertTrue("\"Alice\"" in server.bodies.first(), server.bodies.first())
    }

    @Test
    fun wrongCredentialsShowOneGenericMessageKeepTheUsernameAndClearThePassword() = runTest {
        server.loginAnswer = HttpStatusCode.Unauthorized to ("""{"code":"invalid_credentials","message":"x"}""" to emptyMap())
        fill()

        model.submit()?.join()

        val state = model.state.value
        assertEquals(LoginProblem.WrongCredentials, state.problem)
        assertEquals("alice", state.username)
        assertEquals("", state.password)
        assertFalse(state.inProgress)
    }

    @Test
    fun aLockoutShowsRoundedUpMinutes() = runTest {
        server.loginAnswer = HttpStatusCode.TooManyRequests to ("""{"code":"too_many_attempts","message":"x"}""" to mapOf(HttpHeaders.RetryAfter to "890"))
        fill()

        model.submit()?.join()

        assertEquals(LoginProblem.Paused(15), model.state.value.problem)
        assertEquals("Too many attempts. Sign-in is paused for about 15 minutes.", Strings.signInPaused(15))
    }

    @Test
    fun aLockoutWithoutSecondsStillExplains() = runTest {
        server.loginAnswer = HttpStatusCode.TooManyRequests to ("""{"code":"too_many_attempts","message":"x"}""" to emptyMap())
        fill()

        model.submit()?.join()

        assertEquals(LoginProblem.Paused(null), model.state.value.problem)
    }

    @Test
    fun anUnreachableServerCanBeTriedAgain() = runTest {
        server.unreachable = true
        fill()

        model.submit()?.join()
        assertEquals(LoginProblem.Unreachable, model.state.value.problem)
        assertEquals("password1", model.state.value.password) // kept so a retry is one tap

        server.unreachable = false
        model.submit()?.join()
        assertNull(model.state.value.problem)
        assertTrue(session.state.value is SessionState.SignedIn)
    }

    @Test
    fun anUnexpectedAnswerIsAGenericServerProblem() = runTest {
        server.loginAnswer = HttpStatusCode.InternalServerError to ("""{"code":"internal_error","message":"x"}""" to emptyMap())
        fill()

        model.submit()?.join()

        assertEquals(LoginProblem.ServerFailed, model.state.value.problem)
    }

    @Test
    fun aTemporaryPasswordLeadsToThePasswordChange() = runTest {
        server.loginAnswer = HttpStatusCode.OK to ("""{"token":"t1","mustChangePassword":true}""" to emptyMap())
        server.accountBody = { """{"username":"alice","isAdmin":false,"mustChangePassword":true}""" }
        fill()

        model.submit()?.join()

        assertTrue(session.state.value is SessionState.PasswordChangeRequired)
    }

    @Test
    fun staySignedInIsTickedUntilTheCheckboxIsUnticked() = runTest {
        assertTrue(model.state.value.staySignedIn)

        model.onStaySignedInChange(false)
        assertFalse(model.state.value.staySignedIn)

        model.onStaySignedInChange(true)
        assertTrue(model.state.value.staySignedIn)
    }
}

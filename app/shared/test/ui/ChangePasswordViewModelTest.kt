package com.usuallycoding.opensecretsanta.ui

import com.usuallycoding.opensecretsanta.data.session.SessionState
import com.usuallycoding.opensecretsanta.ui.password.ChangePasswordViewModel
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ChangePasswordViewModelTest {
    private val server = ScriptedServer().apply {
        loginAnswer = HttpStatusCode.OK to ("""{"token":"t1","mustChangePassword":true}""" to emptyMap())
        accountBody = { token ->
            val must = token != "t2"
            """{"username":"alice","isAdmin":false,"mustChangePassword":$must}"""
        }
    }
    private val session = server.session()
    private val model = ChangePasswordViewModel(session, immediateScope())

    private suspend fun signInWithTemporaryPassword() {
        session.signIn("alice", "TempPass0001")
        server.calls.clear()
        server.bodies.clear()
    }

    private fun fill(current: String = "TempPass0001", new: String = "a-better-password", repeat: String = new) {
        model.onCurrentChange(current)
        model.onNewChange(new)
        model.onRepeatChange(repeat)
    }

    @Test
    fun theActionNeedsAllThreeFields() {
        assertFalse(model.state.value.canSubmit)
        model.onCurrentChange("x")
        model.onNewChange("y")
        assertFalse(model.state.value.canSubmit)
        model.onRepeatChange("y")
        assertTrue(model.state.value.canSubmit)
    }

    @Test
    fun aShortPasswordIsRefusedBeforeSending() = runTest {
        signInWithTemporaryPassword()
        fill(new = "short")

        model.submit()?.join()

        assertNotNull(model.state.value.errors.new)
        assertTrue(server.calls.isEmpty())
    }

    @Test
    fun theSamePasswordIsRefusedBeforeSending() = runTest {
        signInWithTemporaryPassword()
        fill(current = "TempPass0001", new = "TempPass0001")

        model.submit()?.join()

        assertNotNull(model.state.value.errors.new)
        assertTrue(server.calls.isEmpty())
    }

    @Test
    fun aMismatchedRepeatIsRefusedBeforeSending() = runTest {
        signInWithTemporaryPassword()
        fill(new = "a-better-password", repeat = "a-better-passwordd")

        model.submit()?.join()

        assertEquals(Strings.PASSWORDS_DO_NOT_MATCH, model.state.value.errors.repeat)
        assertNull(model.state.value.errors.new)
        assertTrue(server.calls.isEmpty())
    }

    @Test
    fun severalProblemsAreShownTogether() = runTest {
        signInWithTemporaryPassword()
        fill(new = "short", repeat = "other")

        model.submit()?.join()

        assertNotNull(model.state.value.errors.new)
        assertNotNull(model.state.value.errors.repeat)
    }

    @Test
    fun aServerValidationErrorLandsOnItsField() = runTest {
        signInWithTemporaryPassword()
        server.passwordAnswer = HttpStatusCode.BadRequest to """{"code":"validation_failed","message":"Too easy.","field":"newPassword"}"""
        fill()

        model.submit()?.join()

        assertEquals("Too easy.", model.state.value.errors.new)
        assertNull(model.state.value.errors.current)
    }

    @Test
    fun aWrongCurrentPasswordIsShownOnThatField() = runTest {
        signInWithTemporaryPassword()
        server.passwordAnswer = HttpStatusCode.Forbidden to """{"code":"forbidden","message":"The current password is incorrect."}"""
        fill()

        model.submit()?.join()

        assertEquals("The current password is incorrect.", model.state.value.errors.current)
        assertTrue(session.state.value is SessionState.PasswordChangeRequired) // the session is not ended by it
    }

    @Test
    fun anUnreachableServerIsExplainedAndTheFormKept() = runTest {
        signInWithTemporaryPassword()
        server.unreachable = true
        fill()

        model.submit()?.join()

        assertEquals(Strings.SERVER_UNREACHABLE, model.state.value.errors.general)
        assertEquals("a-better-password", model.state.value.new)
    }

    @Test
    fun successReplacesTheTokenAndContinues() = runTest {
        signInWithTemporaryPassword()
        fill()

        model.submit()?.join()

        assertTrue(session.state.value is SessionState.SignedIn)
        assertEquals("t2", session.token)
        assertFalse(model.state.value.errors.any)
        assertTrue("TempPass0001" in server.bodies.first() && "a-better-password" in server.bodies.first())
    }

    @Test
    fun aChangeIsDoneAndResettingForgetsEverything() = runTest {
        signInWithTemporaryPassword()
        fill()
        assertFalse(model.state.value.done)

        model.submit()?.join()
        assertTrue(model.state.value.done)

        model.reset()
        assertFalse(model.state.value.done)
        assertEquals("", model.state.value.current)
        assertEquals("", model.state.value.new)
        assertEquals("", model.state.value.repeat)
    }

    @Test
    fun aRefusedChangeIsNotDone() = runTest {
        signInWithTemporaryPassword()
        server.passwordAnswer = HttpStatusCode.Forbidden to """{"code":"forbidden","message":"The current password is incorrect."}"""
        fill()

        model.submit()?.join()
        assertFalse(model.state.value.done)

        model.reset()
        assertFalse(model.state.value.errors.any)
    }
}

package com.usuallycoding.opensecretsanta.ui

import com.usuallycoding.opensecretsanta.data.session.Account
import com.usuallycoding.opensecretsanta.data.session.SessionState
import com.usuallycoding.opensecretsanta.ui.shell.AppView
import com.usuallycoding.opensecretsanta.ui.shell.ShellViewModel
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ShellViewModelTest {
    private val server = ScriptedServer()
    private val session = server.session()

    private fun shell(admin: Boolean) = ShellViewModel(Account("alice", null, admin), session, immediateScope())

    @Test
    fun anAdministratorStartsInTheAdminViewAndSwitchesBothWays() {
        val model = shell(admin = true)

        assertEquals(AppView.Admin, model.view.value)
        assertTrue(model.canSwitch)
        model.select(AppView.Participant)
        assertEquals(AppView.Participant, model.view.value)
        model.select(AppView.Admin)
        assertEquals(AppView.Admin, model.view.value)
    }

    @Test
    fun aNormalParticipantCanNeverReachTheAdminView() {
        val model = shell(admin = false)

        assertEquals(AppView.Participant, model.view.value)
        assertFalse(model.canSwitch)
        model.select(AppView.Admin)
        assertEquals(AppView.Participant, model.view.value)
    }

    @Test
    fun signingOutShowsTheSignInScreen() = runTest {
        session.signIn("alice", "password1")
        assertTrue(session.state.value is SessionState.SignedIn)

        shell(admin = false).signOut()

        assertEquals(SessionState.SignedOut(), session.state.value)
    }
}

package com.usuallycoding.opensecretsanta

import com.usuallycoding.opensecretsanta.ui.Strings
import com.usuallycoding.opensecretsanta.ui.form.mountWebChangePasswordForm
import com.usuallycoding.opensecretsanta.ui.form.mountWebLoginForm
import com.usuallycoding.opensecretsanta.ui.login.LoginUiState
import com.usuallycoding.opensecretsanta.ui.password.ChangePasswordUiState
import com.usuallycoding.opensecretsanta.ui.password.PasswordErrors
import kotlinx.browser.document
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.events.Event
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WebAuthFormsTest {

    private fun hostContainer(): HTMLDivElement =
        (document.createElement("div") as HTMLDivElement).also { document.body!!.appendChild(it) }

    @Test
    fun loginFormExposesSemanticInputsRevealToggleSubmitSyncAndCleansUp() {
        val host = hostContainer()
        var user = ""
        var pass = ""
        var stay = true
        var submits = 0

        val handle = mountWebLoginForm(
            onUsernameChange = { user = it },
            onPasswordChange = { pass = it },
            onStaySignedInChange = { stay = it },
            onSubmit = { submits++ },
            container = host,
        )

        assertNotNull(host.querySelector("form"))
        assertNotNull(document.getElementById("oss-auth-styles"))
        assertEquals(1, document.querySelectorAll("#oss-auth-styles").length)
        assertEquals("username", handle.usernameInput.getAttribute("autocomplete"))
        assertEquals("current-password", handle.passwordField.input.getAttribute("autocomplete"))
        assertEquals("password", handle.passwordField.input.type)
        assertEquals("submit", handle.submitButton.type)
        assertEquals(Strings.SHOW_PASSWORD, handle.passwordField.toggleButton.getAttribute("aria-label"))

        // Reveal toggle flips password type and label, and hidePasswords() restores it.
        handle.passwordField.toggle()
        assertEquals("text", handle.passwordField.input.type)
        assertEquals(Strings.HIDE_PASSWORD, handle.passwordField.toggleButton.getAttribute("aria-label"))
        handle.hidePasswords()
        assertEquals("password", handle.passwordField.input.type)

        // Autofilled values are synced on submit even if input events were skipped.
        handle.usernameInput.value = "alice"
        handle.passwordField.input.value = "secret123"
        handle.form.dispatchEvent(Event("submit"))
        assertEquals("alice", user)
        assertEquals("secret123", pass)
        assertEquals(1, submits)

        // Syncing state updates disabled/logo/version cleanly.
        handle.sync(LoginUiState(username = "alice", password = "secret123"), sessionEnded = true, darkLogo = true, downloadAction = null)
        assertTrue(handle.logo.getAttribute("src") == "app_logo_dark.png")
        assertFalse(handle.submitButton.disabled)
        assertTrue(stay)

        handle.dispose()
        assertNull(host.querySelector("form"))
        host.remove()
    }

    @Test
    fun changePasswordFormExposesHiddenUsernameCurrentAndNewPasswordHints() {
        val host = hostContainer()
        var current = ""
        var newPass = ""
        var repeat = ""
        var submits = 0
        var dismissed = 0

        val handle = mountWebChangePasswordForm(
            isSheet = true,
            username = "bob",
            onCurrentChange = { current = it },
            onNewChange = { newPass = it },
            onRepeatChange = { repeat = it },
            onSubmit = { submits++ },
            onSecondary = { dismissed++ },
            container = host,
        )

        assertNotNull(host.querySelector("form"))
        assertEquals("username", handle.hiddenUsernameInput.getAttribute("autocomplete"))
        assertTrue(handle.hiddenUsernameInput.readOnly)
        assertEquals("bob", handle.hiddenUsernameInput.value)
        assertEquals("current-password", handle.currentField.input.getAttribute("autocomplete"))
        assertEquals("new-password", handle.newField.input.getAttribute("autocomplete"))
        assertEquals("new-password", handle.repeatField.input.getAttribute("autocomplete"))
        assertEquals("submit", handle.submitButton.type)

        // Independent reveal toggles.
        handle.newField.toggle()
        assertEquals("text", handle.newField.input.type)
        assertEquals("password", handle.currentField.input.type)
        assertEquals("password", handle.repeatField.input.type)
        handle.hidePasswords()
        assertEquals("password", handle.newField.input.type)

        // Submit syncs values and runs callback.
        handle.currentField.input.value = "oldpass"
        handle.newField.input.value = "newpass123"
        handle.repeatField.input.value = "newpass123"
        handle.form.dispatchEvent(Event("submit"))
        assertEquals("oldpass", current)
        assertEquals("newpass123", newPass)
        assertEquals("newpass123", repeat)
        assertEquals(1, submits)

        // Syncing errors and done state updates DOM visibility.
        handle.sync("bob", ChangePasswordUiState(errors = PasswordErrors(new = "Too short")), isSheet = true)
        assertEquals("Too short", handle.newField.errorText.textContent)

        handle.sync("bob", ChangePasswordUiState(done = true), isSheet = true)
        assertEquals("none", handle.fieldsWrapper.style.display)
        assertEquals("flex", handle.doneWrapper.style.display)

        handle.dispose()
        assertNull(host.querySelector("form"))
        assertEquals(0, dismissed)
        host.remove()
    }
}

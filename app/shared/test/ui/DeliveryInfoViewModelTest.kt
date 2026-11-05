package com.usuallycoding.opensecretsanta.ui

import com.usuallycoding.opensecretsanta.contract.DeliveryInfoDto
import com.usuallycoding.opensecretsanta.ui.load.loadMessage
import com.usuallycoding.opensecretsanta.ui.delivery.DeliveryField
import com.usuallycoding.opensecretsanta.ui.delivery.DeliveryForm
import com.usuallycoding.opensecretsanta.ui.delivery.DeliveryInfoViewModel
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DeliveryInfoViewModelTest {
    private val server = ScriptedServer().also { it.session() }

    private fun account(
        fullName: String? = "Alice Archer",
        email: String? = "alice@example.org",
        phone: String? = "555-0101",
        address: String? = "1 Main St",
        deliveryInfo: String? = "Back door",
    ): String {
        fun field(key: String, value: String?) = if (value == null) "" else ""","$key":"$value""""
        return "{" + listOf(field("fullName", fullName), field("email", email), field("phone", phone), field("address", address), field("deliveryInfo", deliveryInfo)).filter { it.isNotEmpty() }.joinToString(",") { it.removePrefix(",") } + "}"
    }

    private fun serverHas(json: String) {
        server.deliveryInfoBody = json
    }

    /** The first load runs on the client's own threads, so wait for it before looking at the state. */
    private suspend fun viewModel() = DeliveryInfoViewModel(1, server.api, immediateScope()).also { it.refresh().join() }

    private val full = DeliveryForm("Alice Archer", "alice@example.org", "555-0101", "1 Main St", "Back door")

    private fun putBodies() = server.bodies.filterIndexed { index, _ -> server.calls[index].startsWith("PUT") }

    @Test
    fun anExistingProfileFillsTheFormAndNothingCanBeSavedYet() = runTest {
        serverHas(account())

        val ui = viewModel().ui.value

        assertEquals(full, ui.form)
        assertEquals(full, ui.saved)
        assertFalse(ui.dirty)
        assertFalse(ui.canSave)
    }

    @Test
    fun aNewAccountHasAnEmptyForm() = runTest {
        serverHas(account(null, null, null, null, null))

        val ui = viewModel().ui.value

        assertEquals(DeliveryForm(), ui.form)
        assertFalse(ui.form.hasNameAndAddress)
        assertFalse(ui.canSave)
    }

    @Test
    fun savingIsUnavailableWhileNothingChangedAndSendsNothing() = runTest {
        serverHas(account())
        val model = viewModel()

        assertNull(model.save())

        assertTrue(putBodies().isEmpty())
    }

    @Test
    fun aChangeCanBeSavedAndBecomesTheNewStartingPoint() = runTest {
        serverHas(account())
        val model = viewModel()
        model.edit(DeliveryField.Address, "2 Side Street")
        assertTrue(model.ui.value.canSave)
        server.deliveryInfoAnswer = HttpStatusCode.OK to account(address = "2 Side Street")

        model.save()?.join()

        val ui = model.ui.value
        assertEquals("2 Side Street", ui.saved?.address)
        assertFalse(ui.canSave)
        assertTrue(ui.justSaved)
        assertFalse(ui.saving)
        assertEquals(
            DeliveryInfoDto("Alice Archer", "alice@example.org", "555-0101", "2 Side Street", "Back door"),
            Json.decodeFromString<DeliveryInfoDto>(putBodies().single()),
        )
    }

    @Test
    fun aClearedFieldIsSentAsClearedNotAsBlankText() = runTest {
        serverHas(account())
        val model = viewModel()
        model.edit(DeliveryField.Phone, "   ")
        server.deliveryInfoAnswer = HttpStatusCode.OK to account(phone = null)

        model.save()?.join()

        assertNull(Json.decodeFromString<DeliveryInfoDto>(putBodies().single()).phone)
        assertEquals("", model.ui.value.form.phone)
        assertFalse(model.ui.value.canSave)
    }

    @Test
    fun anInvalidEmailIsShownNextToItsFieldAndNothingIsSent() = runTest {
        serverHas(account())
        val model = viewModel()
        model.edit(DeliveryField.Email, "not-an-email")

        assertNull(model.save())

        assertEquals("Email is not a valid address.", model.ui.value.errors.fields[DeliveryField.Email])
        assertTrue(putBodies().isEmpty())
        assertEquals("not-an-email", model.ui.value.form.email) // the edit is kept
    }

    @Test
    fun aTooLongFieldNamesTheLimitWithItsLabelAndNothingIsSent() = runTest {
        serverHas(account())
        val model = viewModel()
        model.edit(DeliveryField.DeliveryInfo, "x".repeat(501))

        assertNull(model.save())

        assertEquals("Delivery information must be at most 500 characters long.", model.ui.value.errors.fields[DeliveryField.DeliveryInfo])
        assertTrue(putBodies().isEmpty())
    }

    @Test
    fun aValidationErrorFromTheServerLandsOnItsFieldAndKeepsTheEdits() = runTest {
        serverHas(account())
        val model = viewModel()
        model.edit(DeliveryField.FullName, "Alice Q. Archer")
        server.deliveryInfoAnswer = HttpStatusCode.BadRequest to """{"code":"validation_failed","message":"fullName must be at most 500 characters long.","field":"fullName"}"""

        model.save()?.join()

        val ui = model.ui.value
        assertEquals("Full name must be at most 500 characters long.", ui.errors.fields[DeliveryField.FullName])
        assertEquals("Alice Q. Archer", ui.form.fullName)
        assertTrue(ui.canSave)
        assertFalse(ui.saving)
    }

    @Test
    fun anUnreachableServerIsExplainedAndTheEditsAreKept() = runTest {
        serverHas(account())
        val model = viewModel()
        model.edit(DeliveryField.Address, "2 Side Street")
        server.unreachable = true

        model.save()?.join()

        val ui = model.ui.value
        assertEquals(Strings.SERVER_UNREACHABLE, ui.errors.general)
        assertEquals("2 Side Street", ui.form.address)
        assertTrue(ui.canSave) // and it can be tried again
        assertFalse(ui.justSaved)
    }

    @Test
    fun editingAFieldClearsItsErrorAndTheSavedNotice() = runTest {
        serverHas(account())
        val model = viewModel()
        model.edit(DeliveryField.Email, "nope")
        model.save()
        assertEquals("Email is not a valid address.", model.ui.value.errors.fields[DeliveryField.Email])

        model.edit(DeliveryField.Email, "alice@example.org")

        assertTrue(model.ui.value.errors.fields.isEmpty())
    }

    @Test
    fun aReloadNeverReplacesWhatThePersonTyped() = runTest {
        serverHas(account())
        val model = viewModel()
        model.edit(DeliveryField.FullName, "Typed by me")
        serverHas(account(fullName = "Changed elsewhere", address = "9 New Road"))

        model.refresh().join()

        val ui = model.ui.value
        assertEquals("Typed by me", ui.form.fullName)
        assertEquals("1 Main St", ui.form.address) // the untouched fields were not overwritten either
        assertEquals("Changed elsewhere", ui.saved?.fullName) // but the starting point is what the server has now
        assertTrue(ui.canSave)
    }

    @Test
    fun aReloadOfAnUntouchedFormShowsWhatTheServerHasNow() = runTest {
        serverHas(account())
        val model = viewModel()
        serverHas(account(address = "9 New Road"))

        model.refresh().join()

        assertEquals("9 New Road", model.ui.value.form.address)
        assertFalse(model.ui.value.canSave)
    }

    @Test
    fun theHintIsAboutNameAndAddressOnly() = runTest {
        serverHas(account(fullName = "Alice", address = null))
        val model = viewModel()
        assertFalse(model.ui.value.form.hasNameAndAddress)

        model.edit(DeliveryField.Address, "1 Main St")

        assertTrue(model.ui.value.form.hasNameAndAddress)
    }

    @Test
    fun theProfileFailingToLoadCanBeRetried() = runTest {
        server.unreachable = true
        val model = viewModel()
        assertNull(model.load.value.value)
        assertEquals(Strings.SERVER_UNREACHABLE, model.load.value.error?.loadMessage())

        server.unreachable = false
        serverHas(account())
        model.refresh().join()

        assertEquals(full, model.ui.value.form)
    }

    @Test
    fun aGroupThatClosedForEditsIsSaidSoAndTheEditsAreKept() = runTest {
        serverHas(account())
        val model = viewModel()
        model.edit(DeliveryField.Address, "2 Side Street")
        server.deliveryInfoAnswer = HttpStatusCode.Conflict to """{"code":"invalid_state","message":"closed"}"""

        model.save()?.join()

        val ui = model.ui.value
        assertEquals(Strings.GROUP_CLOSED_FOR_EDITS, ui.errors.general)
        assertEquals("2 Side Street", ui.form.address)
        assertFalse(ui.justSaved)
    }

    @Test
    fun aGroupThatIsGoneIsSaidSo() = runTest {
        serverHas(account())
        val model = viewModel()
        model.edit(DeliveryField.Address, "2 Side Street")
        server.deliveryInfoAnswer = HttpStatusCode.NotFound to """{"code":"not_found","message":"Group not found."}"""

        model.save()?.join()

        assertEquals(Strings.GROUP_GONE, model.ui.value.errors.general)
    }

    @Test
    fun theDetailsAreReadAndSavedForThatGroupOnly() = runTest {
        serverHas(account())
        val model = viewModel()
        model.edit(DeliveryField.Address, "2 Side Street")

        model.save()?.join()

        assertTrue(server.calls.contains("GET /groups/1/delivery-info"))
        assertTrue(server.calls.contains("PUT /groups/1/delivery-info"))
    }
}

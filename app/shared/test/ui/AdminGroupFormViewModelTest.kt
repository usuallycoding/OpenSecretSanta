package com.usuallycoding.opensecretsanta.ui

import com.usuallycoding.opensecretsanta.contract.CreateGroupRequest
import com.usuallycoding.opensecretsanta.contract.GroupDetailsRequest
import com.usuallycoding.opensecretsanta.data.api.ApiError
import com.usuallycoding.opensecretsanta.ui.admin.AdminGroupFormViewModel
import com.usuallycoding.opensecretsanta.ui.admin.GroupField
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class AdminGroupFormViewModelTest {
    private val server = ScriptedServer().also { it.session() }
    private val fake get() = server.adminGroups

    private fun newForm() = AdminGroupFormViewModel(null, server.api, immediateScope())

    private suspend fun editForm(id: Long) = AdminGroupFormViewModel(id, server.api, immediateScope()).also { it.refresh().join() }

    private fun AdminGroupFormViewModel.fill(name: String = "", description: String = "", location: String = "", date: String = "", time: String = "") {
        edit(GroupField.Name, name); edit(GroupField.Description, description); edit(GroupField.Location, location)
        edit(GroupField.Date, date); edit(GroupField.Time, time)
    }

    private fun writes() = server.calls.count { it.startsWith("POST") || it.startsWith("PUT") }

    // --- creating ---------------------------------------------------------------------------------

    @Test
    fun aGroupWithOnlyANameIsCreatedAsADraftAndTheNewIdIsReported() = runTest {
        val form = newForm()
        form.fill(name = "  Office party  ")

        form.save()?.join()

        assertEquals(fake.ids.single(), form.saved.value)
        assertEquals("Office party", fake.group(form.saved.value!!).name)
        assertEquals("draft", fake.group(form.saved.value!!).state)
        assertEquals(CreateGroupRequest("Office party"), Json.decodeFromString(server.bodies.last()))
    }

    @Test
    fun everyFieldIsSentTrimmedAndTheTimeInItsPlainForm() = runTest {
        val form = newForm()
        form.fill("Office party", " Small gifts ", " Town Hall ", " 2026-12-24 ", " 18:30 ")

        form.save()?.join()

        assertEquals(
            CreateGroupRequest("Office party", "Small gifts", "Town Hall", "2026-12-24", "18:30"),
            Json.decodeFromString(server.bodies.last()),
        )
    }

    @Test
    fun blanksAreSentAsNothing() = runTest {
        val form = newForm()
        form.fill("Office party", "   ", "", "  ", "")

        form.save()?.join()

        assertEquals(CreateGroupRequest("Office party", null, null, null, null), Json.decodeFromString(server.bodies.last()))
    }

    @Test
    fun aBlankOrTooLongNameIsNotSentAndIsShownOnTheNameField() = runTest {
        val form = newForm()

        form.fill(name = "   ")
        assertNull(form.save())
        assertEquals("Name must be 1 to 100 characters long.", form.form.value.errors[GroupField.Name])

        form.fill(name = "x".repeat(101))
        assertNull(form.save())
        assertNotNull(form.form.value.errors[GroupField.Name])
        assertEquals(0, writes())
    }

    @Test
    fun anInvalidDateOrTimeIsNotSentAndSaysWhatIsExpected() = runTest {
        val form = newForm()

        form.fill(name = "Party", date = "2026-13-45")
        assertNull(form.save())
        assertEquals("Date must be an ISO date such as 2026-12-24.", form.form.value.errors[GroupField.Date])

        form.fill(name = "Party", time = "25:99")
        assertNull(form.save())
        assertEquals("Time must be a time of day such as 18:30.", form.form.value.errors[GroupField.Time])
        assertEquals(0, writes())
    }

    @Test
    fun aTooLongDescriptionOrLocationIsShownOnItsField() = runTest {
        val form = newForm()

        form.fill(name = "Party", description = "d".repeat(2001))
        assertNull(form.save())
        assertNotNull(form.form.value.errors[GroupField.Description])

        form.fill(name = "Party", location = "l".repeat(201))
        assertNull(form.save())
        assertNotNull(form.form.value.errors[GroupField.Location])
    }

    @Test
    fun typingClearsThatFieldsError() = runTest {
        val form = newForm()
        form.fill(name = " ", date = "nope")
        form.save()
        assertNotNull(form.form.value.errors[GroupField.Name])

        form.edit(GroupField.Name, "Party")

        assertNull(form.form.value.errors[GroupField.Name])
    }

    @Test
    fun aServerFieldErrorIsShownOnItsFieldAndTheFormKeepsWhatWasTyped() = runTest {
        val form = newForm()
        form.fill("Party", location = "Town Hall")
        fake.failNextWriteWith(HttpStatusCode.BadRequest, "validation_failed", "Location is not acceptable.", "location")

        form.save()?.join()

        assertEquals("Location is not acceptable.", form.form.value.errors[GroupField.Location])
        assertEquals("Town Hall", form.form.value.location)
        assertFalse(form.form.value.saving)
        assertNull(form.saved.value)
    }

    @Test
    fun aNetworkFailureKeepsTheTextAndCanBeRetried() = runTest {
        val form = newForm()
        form.fill("Party")
        server.unreachable = true

        form.save()?.join()

        assertEquals(Strings.SERVER_UNREACHABLE, form.form.value.general)
        assertEquals("Party", form.form.value.name)
        assertFalse(form.form.value.saving)

        server.unreachable = false
        form.save()?.join()

        assertNotNull(form.saved.value)
    }

    // --- editing ------------------------------------------------------------------------------------

    @Test
    fun theFormIsFilledFromTheGroupBeingEdited() = runTest {
        val id = fake.add("Office party")
        fake.group(id).apply { description = "Small gifts"; location = "Town Hall"; date = "2026-12-24"; time = "18:30" }

        val form = editForm(id)

        assertEquals("Office party", form.form.value.name)
        assertEquals("Small gifts", form.form.value.description)
        assertEquals("Town Hall", form.form.value.location)
        assertEquals("2026-12-24", form.form.value.date)
        assertEquals("18:30", form.form.value.time)
    }

    @Test
    fun aRefreshDoesNotOverwriteWhatWasTyped() = runTest {
        val id = fake.add("Office party")
        val form = editForm(id)
        form.edit(GroupField.Name, "Renamed")

        form.refresh().join()

        assertEquals("Renamed", form.form.value.name)
    }

    @Test
    fun aClearedFieldIsSavedAsEmptyAndTheEditedIdIsReported() = runTest {
        val id = fake.add("Office party")
        fake.group(id).description = "Small gifts"
        val form = editForm(id)

        form.edit(GroupField.Description, "")
        form.save()?.join()

        assertEquals(id, form.saved.value)
        assertNull(fake.group(id).description)
        assertEquals(GroupDetailsRequest("Office party", null, null, null, null), Json.decodeFromString(server.bodies.last()))
    }

    @Test
    fun editingAnArchivedGroupIsRefusedAndSaysSo() = runTest {
        val id = fake.add("Old")
        val form = editForm(id)
        fake.moveBehindTheScenes(id, "archived")

        form.save()?.join()

        assertEquals(Strings.GROUP_CLOSED_FOR_EDITS, form.form.value.general)
        assertNull(form.saved.value)
    }

    @Test
    fun aGroupThatIsNotThereCannotBeLoadedForEditing() = runTest {
        val id = fake.addForeign("Not mine")

        val form = editForm(id)

        assertEquals(404, (form.loaded.value.error as ApiError.Rejected).status)
        assertNull(form.loaded.value.value)
    }
}

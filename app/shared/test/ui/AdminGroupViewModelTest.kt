package com.usuallycoding.opensecretsanta.ui

import com.usuallycoding.opensecretsanta.contract.DeletionImpactDto
import com.usuallycoding.opensecretsanta.data.api.ApiError
import com.usuallycoding.opensecretsanta.ui.admin.AdminGroupViewModel
import com.usuallycoding.opensecretsanta.ui.admin.AdminNotice
import com.usuallycoding.opensecretsanta.ui.admin.AdminPhase
import com.usuallycoding.opensecretsanta.ui.admin.AdminStep
import com.usuallycoding.opensecretsanta.ui.groups.isGone
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AdminGroupViewModelTest {
    private val server = ScriptedServer().also { it.session() }
    private val fake get() = server.adminGroups

    private suspend fun viewModel(id: Long) = AdminGroupViewModel(id, "admin", server.api, immediateScope()).also { it.refresh().join() }

    private fun AdminGroupViewModel.names() = data.value.value!!.participants.map { it.username }

    private fun writes() = server.calls.count { it.startsWith("POST") || it.startsWith("PUT") || it.startsWith("DELETE") }

    private val four = listOf("alice", "bob", "carol", "dave")

    // --- loading and what each phase allows ------------------------------------------------------------

    @Test
    fun theGroupIsShownWithItsParticipantsAndTheirMarks() = runTest {
        val id = fake.add("Office party", participants = listOf("alice", "bob", "newcomer"))

        val group = viewModel(id).data.value.value!!

        assertEquals("Office party", group.name)
        assertEquals(AdminPhase.Draft, group.phase)
        assertEquals(listOf(true, true, false), group.participants.map { it.hasAccount })
        assertEquals(listOf(true, false, false), group.participants.map { it.profileComplete })
    }

    @Test
    fun aDraftCanBeEditedMadePendingAndDeletedAndItsParticipantsChanged() = runTest {
        val model = viewModel(fake.add("Draft"))

        assertTrue(model.canEditDetails)
        assertTrue(model.canEditParticipants)
        assertTrue(model.canDelete)
        assertEquals(AdminStep.MakePending, model.nextStep)
    }

    @Test
    fun aPendingGroupCanBeDrawnAndDeleted() = runTest {
        val model = viewModel(fake.add("Pending", state = "pending", participants = four))

        assertTrue(model.canEditDetails)
        assertTrue(model.canEditParticipants)
        assertTrue(model.canDelete)
        assertEquals(AdminStep.DrawNames, model.nextStep)
        assertFalse(model.drawNeedsMorePeople)
    }

    @Test
    fun anActiveGroupHasAFrozenRosterAndCanBeArchived() = runTest {
        val model = viewModel(fake.add("Active", state = "active", participants = four))

        assertTrue(model.canEditDetails)
        assertFalse(model.canEditParticipants)
        assertTrue(model.canDelete)
        assertEquals(AdminStep.Archive, model.nextStep)
    }

    @Test
    fun anArchivedGroupAllowsNothingButDeleting() = runTest {
        val model = viewModel(fake.add("Old", state = "archived", participants = four))

        assertFalse(model.canEditDetails)
        assertFalse(model.canEditParticipants)
        assertTrue(model.canDelete)
        assertNull(model.nextStep)
        model.openParticipants()
        model.ask(AdminStep.Archive)
        assertNull(model.ui.value.participants)
        assertNull(model.ui.value.confirming)
    }

    @Test
    fun aGroupOfAnotherAdminIsReportedAsGone() = runTest {
        val model = viewModel(fake.addForeign("Not mine"))

        assertTrue(model.data.value.isGone())
    }

    @Test
    fun anUnreachableServerCanBeRetried() = runTest {
        val id = fake.add("Office party")
        server.unreachable = true
        val model = AdminGroupViewModel(id, "admin", server.api, immediateScope()).also { it.refresh().join() }
        assertEquals(ApiError.Network, model.data.value.error)

        server.unreachable = false
        model.refresh().join()

        assertEquals("Office party", model.data.value.value!!.name)
    }

    // --- the participants sheet ---------------------------------------------------------------------------

    @Test
    fun aUsernameIsAddedAndSavedAndTheSheetCloses() = runTest {
        val id = fake.add("Draft", participants = listOf("alice"))
        val model = viewModel(id)

        model.openParticipants()
        assertEquals(listOf("alice"), model.ui.value.participants!!.names)
        model.typeUsername("  Dave ")
        model.addUsername()
        assertEquals(listOf("alice", "dave"), model.ui.value.participants!!.names)
        assertEquals("", model.ui.value.participants!!.input)
        model.saveParticipants()?.join()

        assertNull(model.ui.value.participants)
        assertEquals(listOf("alice", "dave"), model.names())
        assertEquals(listOf("alice", "dave"), fake.group(id).participants)
    }

    @Test
    fun aUsernameIsRemoved() = runTest {
        val id = fake.add("Draft", participants = listOf("alice", "bob"))
        val model = viewModel(id)

        model.openParticipants()
        model.removeUsername("bob")
        model.saveParticipants()?.join()

        assertEquals(listOf("alice"), model.names())
    }

    @Test
    fun anInvalidOrRepeatedUsernameIsNotAddedAndTheSheetSaysWhy() = runTest {
        val model = viewModel(fake.add("Draft", participants = listOf("alice")))
        model.openParticipants()

        model.typeUsername("x"); model.addUsername()
        assertEquals("Username must be 3 to 32 characters long.", model.ui.value.participants!!.error)
        model.typeUsername("bad name"); model.addUsername()
        assertEquals("Username may only contain letters, digits, '.', '_' and '-'.", model.ui.value.participants!!.error)
        model.typeUsername("Alice"); model.addUsername()
        assertEquals(Strings.ADMIN_ALREADY_LISTED, model.ui.value.participants!!.error)

        assertEquals(listOf("alice"), model.ui.value.participants!!.names)
        assertEquals(0, writes())
    }

    @Test
    fun typingClearsTheUsernameError() = runTest {
        val model = viewModel(fake.add("Draft"))
        model.openParticipants()
        model.typeUsername("x"); model.addUsername()

        model.typeUsername("xy")

        assertNull(model.ui.value.participants!!.error)
    }

    @Test
    fun dismissingTheSheetDiscardsTheChanges() = runTest {
        val model = viewModel(fake.add("Draft", participants = listOf("alice")))
        model.openParticipants(); model.typeUsername("dave"); model.addUsername()

        model.dismissParticipants()
        model.openParticipants()

        assertEquals(listOf("alice"), model.ui.value.participants!!.names)
    }

    @Test
    fun aRosterFrozenElsewhereClosesTheSheetReloadsAndSaysSo() = runTest {
        val id = fake.add("Pending", state = "pending", participants = four)
        val model = viewModel(id)
        model.openParticipants(); model.typeUsername("erin"); model.addUsername()
        fake.moveBehindTheScenes(id, "active")

        model.saveParticipants()?.join()

        assertNull(model.ui.value.participants)
        assertEquals(AdminNotice.RosterFrozen, model.ui.value.notice)
        assertEquals(AdminPhase.Active, model.data.value.value!!.phase)
        assertEquals(four, fake.group(id).participants)
    }

    @Test
    fun aNetworkFailureKeepsTheSheetAndTheEditsAndCanBeRetried() = runTest {
        val model = viewModel(fake.add("Draft"))
        model.openParticipants(); model.typeUsername("dave"); model.addUsername()
        server.unreachable = true

        model.saveParticipants()?.join()

        assertEquals(Strings.SERVER_UNREACHABLE, model.ui.value.participants!!.general)
        assertEquals(listOf("dave"), model.ui.value.participants!!.names)
        assertFalse(model.ui.value.participants!!.saving)

        server.unreachable = false
        model.saveParticipants()?.join()

        assertEquals(listOf("dave"), model.names())
    }

    @Test
    fun aServerRefusalOfTheListStaysInTheSheet() = runTest {
        val model = viewModel(fake.add("Draft"))
        model.openParticipants(); model.typeUsername("dave"); model.addUsername()
        fake.failNextWriteWith(HttpStatusCode.BadRequest, "validation_failed", "Participants are not acceptable.", "participants")

        model.saveParticipants()?.join()

        assertEquals("Participants are not acceptable.", model.ui.value.participants!!.error)
        assertEquals(listOf("dave"), model.ui.value.participants!!.names)
    }

    // --- the steps -------------------------------------------------------------------------------------------

    @Test
    fun makingAGroupPendingNeedsAConfirmationAndNothingIsSentBeforeIt() = runTest {
        val model = viewModel(fake.add("Draft", participants = listOf("alice", "newcomer")))

        model.ask(AdminStep.MakePending)
        assertEquals(AdminStep.MakePending, model.ui.value.confirming)
        assertEquals(0, writes())
        model.confirmStep()?.join()

        assertEquals(AdminPhase.Pending, model.data.value.value!!.phase)
        assertNull(model.ui.value.confirming)
        assertFalse(model.ui.value.busy)
    }

    @Test
    fun cancellingAConfirmationSendsNothing() = runTest {
        val model = viewModel(fake.add("Draft"))

        model.ask(AdminStep.MakePending)
        model.cancelStep()

        assertNull(model.ui.value.confirming)
        assertNull(model.confirmStep())
        assertEquals(0, writes())
    }

    @Test
    fun theTemporaryPasswordsOfCreatedAccountsAreShownOnceAndGoneAfterDismissal() = runTest {
        val id = fake.add("Draft", participants = listOf("alice", "bob", "newcomer", "other"))
        val model = viewModel(id)
        model.ask(AdminStep.MakePending)
        model.confirmStep()?.join()

        val passwords = model.ui.value.passwords!!
        assertEquals(listOf("newcomer", "other"), passwords.map { it.username })
        assertEquals("Temp-newcomer-1234", passwords.first().temporaryPassword)

        model.dismissPasswords()

        assertNull(model.ui.value.passwords)
        assertFalse("Temp-newcomer" in "${model.data.value} ${model.ui.value}")
    }

    @Test
    fun noDialogWhenNoAccountWasCreated() = runTest {
        val model = viewModel(fake.add("Draft", participants = listOf("alice", "bob")))
        model.ask(AdminStep.MakePending)
        model.confirmStep()?.join()

        assertNull(model.ui.value.passwords)
    }

    @Test
    fun aPersonAddedWhilePendingGetsAnAccountAndAPasswordDialog() = runTest {
        val model = viewModel(fake.add("Pending", state = "pending", participants = listOf("alice")))
        model.openParticipants(); model.typeUsername("dave"); model.addUsername()

        model.saveParticipants()?.join()

        assertEquals(listOf("dave"), model.ui.value.passwords!!.map { it.username })
    }

    @Test
    fun drawingTheNamesNeedsFourPeopleAndWithThreeItIsUnavailable() = runTest {
        val model = viewModel(fake.add("Pending", state = "pending", participants = listOf("alice", "bob", "carol")))

        assertTrue(model.drawNeedsMorePeople)
        model.ask(AdminStep.DrawNames)

        assertNull(model.ui.value.confirming)
        assertEquals(0, writes())
    }

    @Test
    fun theNamesAreDrawnAndTheRosterIsFrozen() = runTest {
        val model = viewModel(fake.add("Pending", state = "pending", participants = four))

        model.ask(AdminStep.DrawNames)
        model.confirmStep()?.join()

        assertEquals(AdminPhase.Active, model.data.value.value!!.phase)
        assertFalse(model.canEditParticipants)
    }

    @Test
    fun anActiveGroupIsArchived() = runTest {
        val model = viewModel(fake.add("Active", state = "active", participants = four))

        model.ask(AdminStep.Archive)
        model.confirmStep()?.join()

        assertEquals(AdminPhase.Archived, model.data.value.value!!.phase)
        assertNull(model.nextStep)
    }

    @Test
    fun aDraftIsDeletedAndTheScreenIsToldToLeave() = runTest {
        val id = fake.add("Draft")
        val model = viewModel(id)

        model.ask(AdminStep.Delete)
        model.confirmStep()?.join()

        assertTrue(model.ui.value.deleted)
        assertEquals(emptyList(), fake.ids)
    }

    @Test
    fun aDraftOrAPendingGroupWithNothingLinkedIsDeletedWithOneConfirmation() = runTest {
        for (state in listOf("draft", "pending")) {
            val model = viewModel(fake.add("Group", state = state, participants = four))

            model.ask(AdminStep.Delete)
            model.confirmStep()?.join()

            assertTrue(model.ui.value.deleted, state)
            assertNull(model.ui.value.confirmingDelete, state)
        }
        assertEquals(emptyList(), fake.ids)
        assertEquals(listOf(false, false), fake.deleteRequests)
    }

    @Test
    fun aGroupWithLinkedDataAsksAgainWithTheCountsAndIsKeptUntilTheSecondConfirmation() = runTest {
        val id = fake.add("Active", state = "active", participants = four)
        fake.impacts[id] = DeletionImpactDto(wishlistItems = 12, questions = 7, answers = 5, draw = true)
        val model = viewModel(id)

        model.ask(AdminStep.Delete)
        model.confirmStep()?.join()

        assertEquals(DeletionImpactDto(12, 7, 5, draw = true), model.ui.value.confirmingDelete)
        assertFalse(model.ui.value.deleted)
        assertFalse(model.ui.value.busy)
        assertEquals(listOf(id), fake.ids)

        model.confirmDelete()?.join()

        assertTrue(model.ui.value.deleted)
        assertNull(model.ui.value.confirmingDelete)
        assertEquals(emptyList(), fake.ids)
        assertEquals(listOf(false, true), fake.deleteRequests)
    }

    @Test
    fun aDrawnGroupAlwaysAsksTwice() = runTest {
        for (state in listOf("active", "archived")) {
            val model = viewModel(fake.add("Group", state = state, participants = four))

            model.ask(AdminStep.Delete)
            model.confirmStep()?.join()

            assertNotNull(model.ui.value.confirmingDelete, state)
        }
    }

    @Test
    fun cancellingTheSecondConfirmationDeletesNothing() = runTest {
        val id = fake.add("Active", state = "active", participants = four)
        val model = viewModel(id)
        model.ask(AdminStep.Delete)
        model.confirmStep()?.join()

        model.cancelDelete()

        assertNull(model.ui.value.confirmingDelete)
        assertFalse(model.ui.value.deleted)
        assertEquals(listOf(id), fake.ids)
        assertEquals(listOf(false), fake.deleteRequests)
        assertNull(model.confirmDelete())
    }

    @Test
    fun aGroupThatWentAwayMeanwhileIsReportedAsGoneAtTheSecondConfirmation() = runTest {
        val id = fake.add("Active", state = "active", participants = four)
        val model = viewModel(id)
        model.ask(AdminStep.Delete)
        model.confirmStep()?.join()
        fake.removeBehindTheScenes(id)

        model.confirmDelete()?.join()

        assertTrue(model.data.value.isGone())
        assertFalse(model.ui.value.deleted)
    }

    @Test
    fun aStepThatDoesNotFitTheStateIsNotAskedFor() = runTest {
        val model = viewModel(fake.add("Draft"))

        model.ask(AdminStep.DrawNames)
        assertNull(model.ui.value.confirming)
        model.ask(AdminStep.Archive)
        assertNull(model.ui.value.confirming)
        model.ask(AdminStep.Delete)
        assertEquals(AdminStep.Delete, model.ui.value.confirming)
    }

    @Test
    fun aStepRefusedBecauseTheGroupChangedReloadsAndSaysSo() = runTest {
        val id = fake.add("Draft", participants = listOf("alice"))
        val model = viewModel(id)
        model.ask(AdminStep.MakePending)
        fake.moveBehindTheScenes(id, "pending")

        model.confirmStep()?.join()

        assertEquals(AdminNotice.ChangedElsewhere, model.ui.value.notice)
        assertEquals(AdminPhase.Pending, model.data.value.value!!.phase)
        assertFalse(model.ui.value.busy)
    }

    @Test
    fun aGroupDeletedElsewhereIsShownAsGone() = runTest {
        val id = fake.add("Draft")
        val model = viewModel(id)
        model.ask(AdminStep.MakePending)
        fake.removeBehindTheScenes(id)

        model.confirmStep()?.join()

        assertTrue(model.data.value.isGone())
        assertNull(model.ui.value.notice)
    }

    @Test
    fun aNetworkFailureOnAStepIsANoticeAndChangesNothing() = runTest {
        val model = viewModel(fake.add("Draft"))
        model.ask(AdminStep.MakePending)
        server.unreachable = true

        model.confirmStep()?.join()

        assertEquals(AdminNotice.Unreachable, model.ui.value.notice)
        assertEquals(AdminPhase.Draft, model.data.value.value!!.phase)
        assertFalse(model.ui.value.busy)
        model.dismissNotice()
        assertNull(model.ui.value.notice)
    }

    @Test
    fun aGroupHeldByTheScreenKeepsNoPasswordsInItsLoadedData() = runTest {
        val model = viewModel(fake.add("Draft", participants = listOf("newcomer")))
        model.ask(AdminStep.MakePending)
        model.confirmStep()?.join()
        assertNotNull(model.ui.value.passwords)

        assertFalse("Temp-" in model.data.value.toString())
    }
}

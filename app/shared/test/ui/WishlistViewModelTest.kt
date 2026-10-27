package com.usuallycoding.opensecretsanta.ui

import com.usuallycoding.opensecretsanta.data.api.ApiError
import com.usuallycoding.opensecretsanta.ui.groups.GroupPhase
import com.usuallycoding.opensecretsanta.ui.groups.isGone
import com.usuallycoding.opensecretsanta.ui.wishlist.WishField
import com.usuallycoding.opensecretsanta.ui.wishlist.WishlistNotice
import com.usuallycoding.opensecretsanta.ui.wishlist.WishlistViewModel
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WishlistViewModelTest {
    private val server = ScriptedServer().also { it.session() }

    private fun groupIs(state: String) {
        server.groupAnswer = { HttpStatusCode.OK to """{"id":1,"name":"Office party","state":"$state","participants":["alice","bob","carol","dave"]}""" }
    }

    /** The first load runs on the client's own threads, so wait for it before looking at the state. */
    private suspend fun viewModel(state: String = "active"): WishlistViewModel {
        groupIs(state)
        return WishlistViewModel(1, server.api, immediateScope()).also { it.refresh().join() }
    }

    private fun WishlistViewModel.titles() = data.value.value!!.items.map { it.title }

    private fun WishlistViewModel.fill(title: String = "", note: String = "", url: String = "") {
        edit(WishField.Title, title); edit(WishField.Note, note); edit(WishField.Url, url)
    }

    private fun changesSent() = server.calls.count { it.startsWith("POST") || it.startsWith("PUT") || it.startsWith("DELETE") }

    // --- 2.1 loading -----------------------------------------------------------------------------

    @Test
    fun theListIsShownInThePersonsOrderWithItsIds() = runTest {
        val socks = server.wishlist.add("Socks", "Grey", "https://shop.example/socks")
        val tea = server.wishlist.add("Tea")

        val items = viewModel().data.value.value!!.items

        assertEquals(listOf("Socks", "Tea"), items.map { it.title })
        assertEquals(listOf(socks, tea), items.map { it.id })
        assertEquals("Grey", items.first().note)
        assertEquals("https://shop.example/socks", items.first().url)
        assertNull(items.last().note)
    }

    @Test
    fun anEmptyListIsEmptyNotAnError() = runTest {
        val state = viewModel().data.value

        assertEquals(emptyList(), state.value!!.items)
        assertNull(state.error)
    }

    @Test
    fun theListCanBeChangedWhilePendingAndActiveButNotOnceArchived() = runTest {
        server.wishlist.add("Socks")

        assertTrue(viewModel("pending").editable)
        assertTrue(viewModel("active").editable)
        val archived = viewModel("archived")
        assertFalse(archived.editable)
        assertEquals(GroupPhase.Archived, archived.data.value.value!!.phase)
        assertEquals(listOf("Socks"), archived.titles()) // still readable
        assertFalse(viewModel("frozen").editable)
    }

    @Test
    fun anUnreachableServerCanBeRetried() = runTest {
        server.wishlist.add("Socks")
        server.unreachable = true
        groupIs("active")
        val model = WishlistViewModel(1, server.api, immediateScope()).also { it.refresh().join() }

        assertEquals(ApiError.Network, model.data.value.error)
        assertNull(model.data.value.value)

        server.unreachable = false
        model.refresh().join()

        assertEquals(listOf("Socks"), model.titles())
    }

    @Test
    fun aGroupThatIsGoneIsReportedAsGone() = runTest {
        server.groupAnswer = { HttpStatusCode.NotFound to """{"code":"not_found","message":"Group not found."}""" }

        val model = WishlistViewModel(1, server.api, immediateScope()).also { it.refresh().join() }

        assertTrue(model.data.value.isGone())
    }

    // --- 2.2 adding and editing --------------------------------------------------------------------

    @Test
    fun anAddedItemAppearsLastAndTheSheetCloses() = runTest {
        server.wishlist.add("Socks")
        val model = viewModel()

        model.openAdd()
        assertNotNull(model.ui.value.editor)
        assertTrue(model.ui.value.editor!!.adding)
        model.fill("Tea", "Loose leaf", "https://tea.example")
        model.save()?.join()

        assertEquals(listOf("Socks", "Tea"), model.titles())
        assertEquals("Loose leaf", model.data.value.value!!.items.last().note)
        assertNull(model.ui.value.editor)
        assertEquals(listOf("Socks", "Tea"), server.wishlist.titles)
    }

    @Test
    fun anEditKeepsItsPlaceAndShowsWhatTheServerSaved() = runTest {
        server.wishlist.add("Socks")
        val tea = server.wishlist.add("Tea")
        server.wishlist.add("Book")
        val model = viewModel()

        model.openEdit(tea)
        assertEquals("Tea", model.ui.value.editor!!.title)
        model.edit(WishField.Title, "  Green tea  ")
        model.save()?.join()

        assertEquals(listOf("Socks", "Green tea", "Book"), model.titles())
        assertNull(model.ui.value.editor)
    }

    @Test
    fun clearingTheNoteAndTheLinkRemovesThem() = runTest {
        val socks = server.wishlist.add("Socks", "Grey", "https://shop.example/socks")
        val model = viewModel()

        model.openEdit(socks)
        model.edit(WishField.Note, "")
        model.edit(WishField.Url, "   ")
        model.save()?.join()

        val item = model.data.value.value!!.items.single()
        assertNull(item.note)
        assertNull(item.url)
    }

    @Test
    fun anEmptyTitleIsRefusedNextToItsFieldAndNothingIsSent() = runTest {
        val model = viewModel()
        model.openAdd()
        model.fill(title = "   ")

        assertNull(model.save())

        assertTrue(WishField.Title in model.ui.value.editor!!.errors)
        assertEquals(0, changesSent())
    }

    @Test
    fun tooLongFieldsAreRefusedNextToTheirFields() = runTest {
        val model = viewModel()
        model.openAdd()

        model.fill(title = "t".repeat(201))
        assertNull(model.save())
        assertTrue(WishField.Title in model.ui.value.editor!!.errors)

        model.fill(title = "ok", note = "n".repeat(1001))
        assertNull(model.save())
        assertTrue(WishField.Note in model.ui.value.editor!!.errors)
        assertFalse(WishField.Title in model.ui.value.editor!!.errors)

        model.fill(title = "ok", url = "https://shop.example/" + "x".repeat(2000))
        assertNull(model.save())
        assertTrue(WishField.Url in model.ui.value.editor!!.errors)
        assertEquals(0, changesSent())
    }

    @Test
    fun aLinkThatIsNotAWebAddressIsRefused() = runTest {
        val model = viewModel()
        model.openAdd()

        for (bad in listOf("javascript:alert(1)", "ftp://host/file", "file:///etc/passwd", "//host/path", "not a url", "data:text/html,x")) {
            model.fill(title = "Socks", url = bad)
            assertNull(model.save(), bad)
            assertTrue(WishField.Url in model.ui.value.editor!!.errors, bad)
        }
        assertEquals(0, changesSent())
    }

    @Test
    fun aValidLinkIsAccepted() = runTest {
        val model = viewModel()
        model.openAdd()
        model.fill(title = "Socks", url = "HTTPS://shop.example/socks?color=grey")

        model.save()?.join()

        assertEquals(1, server.wishlist.ids.size)
    }

    @Test
    fun editingAFieldClearsItsOwnError() = runTest {
        val model = viewModel()
        model.openAdd()
        model.fill(title = "")
        model.save()
        assertTrue(WishField.Title in model.ui.value.editor!!.errors)

        model.edit(WishField.Title, "Socks")

        assertTrue(model.ui.value.editor!!.errors.isEmpty())
    }

    @Test
    fun aServerFieldErrorKeepsTheSheetOpenWithTheEdits() = runTest {
        val model = viewModel()
        model.openAdd()
        model.fill(title = "Socks", note = "Grey")
        server.wishlist.failNextChangeWith(HttpStatusCode.BadRequest, "validation_failed", "Title must be 1 to 200 characters long.", field = "title")

        model.save()?.join()

        val editor = model.ui.value.editor!!
        assertEquals("Title must be 1 to 200 characters long.", editor.errors[WishField.Title])
        assertEquals("Socks", editor.title)
        assertEquals("Grey", editor.note)
        assertFalse(editor.saving)
    }

    @Test
    fun aNetworkFailureKeepsTheSheetAndTheEdits() = runTest {
        val model = viewModel()
        model.openAdd()
        model.fill(title = "Socks")
        server.unreachable = true

        model.save()?.join()

        val editor = model.ui.value.editor!!
        assertEquals(Strings.SERVER_UNREACHABLE, editor.general)
        assertEquals("Socks", editor.title)
        assertFalse(editor.saving)

        server.unreachable = false
        model.save()?.join() // and it can be tried again
        assertEquals(listOf("Socks"), model.titles())
    }

    @Test
    fun dismissingTheSheetDiscardsWhatWasTyped() = runTest {
        val model = viewModel()
        model.openAdd()
        model.fill(title = "Socks")

        model.dismissEditor()

        assertNull(model.ui.value.editor)
        assertEquals(0, changesSent())
        model.openAdd()
        assertEquals("", model.ui.value.editor!!.title)
    }

    @Test
    fun addingIsUnavailableAtFiftyItems() = runTest {
        repeat(50) { server.wishlist.add("Item $it") }
        val model = viewModel()

        assertFalse(model.canAdd)
        model.openAdd()

        assertNull(model.ui.value.editor)
    }

    @Test
    fun addingAndEditingAreUnavailableOnceArchived() = runTest {
        val socks = server.wishlist.add("Socks")
        val model = viewModel("archived")

        model.openAdd()
        model.openEdit(socks)

        assertNull(model.ui.value.editor)
    }

    // --- 2.3 deleting and reordering ----------------------------------------------------------------

    @Test
    fun confirmingADeleteRemovesOnlyThatItemAndClosesTheSheet() = runTest {
        server.wishlist.add("Socks")
        val tea = server.wishlist.add("Tea")
        server.wishlist.add("Book")
        val model = viewModel()
        model.openEdit(tea)

        model.requestDelete()
        assertEquals("Tea", model.ui.value.deleting?.title)
        model.confirmDelete()?.join()

        assertEquals(listOf("Socks", "Book"), model.titles())
        assertEquals(listOf("Socks", "Book"), server.wishlist.titles)
        assertNull(model.ui.value.deleting)
        assertNull(model.ui.value.editor)
    }

    @Test
    fun cancellingADeleteChangesNothingAndSendsNothing() = runTest {
        val tea = server.wishlist.add("Tea")
        val model = viewModel()
        model.openEdit(tea)
        model.requestDelete()

        model.cancelDelete()

        assertNull(model.ui.value.deleting)
        assertEquals(listOf("Tea"), model.titles())
        assertNotNull(model.ui.value.editor) // the sheet is still open
        assertEquals(0, changesSent())
    }

    @Test
    fun movingAnItemUpAndDownSendsAllIdsAndShowsTheServersOrder() = runTest {
        val a = server.wishlist.add("A")
        val b = server.wishlist.add("B")
        val c = server.wishlist.add("C")
        val model = viewModel()

        model.moveUp(b)?.join()
        assertEquals(listOf("B", "A", "C"), model.titles())

        model.moveDown(b)?.join()
        assertEquals(listOf("A", "B", "C"), model.titles())

        model.moveDown(a)?.join()
        assertEquals(listOf("B", "A", "C"), model.titles())
        assertEquals(listOf(b, a, c), server.wishlist.ids)
        assertFalse(model.ui.value.moving)
    }

    @Test
    fun theFirstCannotMoveUpAndTheLastCannotMoveDown() = runTest {
        val a = server.wishlist.add("A")
        server.wishlist.add("B")
        val c = server.wishlist.add("C")
        val model = viewModel()

        assertNull(model.moveUp(a))
        assertNull(model.moveDown(c))
        assertNull(model.moveUp(9999))

        assertEquals(0, changesSent())
    }

    @Test
    fun aListChangedElsewhereIsReloadedWithANotice() = runTest {
        val a = server.wishlist.add("A")
        val b = server.wishlist.add("B")
        val model = viewModel()
        server.wishlist.add("C, added on another device")

        model.moveUp(b)?.join()

        assertEquals(WishlistNotice.ChangedElsewhere, model.ui.value.notice)
        assertEquals(listOf("A", "B", "C, added on another device"), model.titles()) // reloaded, not reshuffled
        assertEquals(listOf(a, b), model.data.value.value!!.items.map { it.id }.take(2))
    }

    @Test
    fun aGroupArchivedMeanwhileMakesTheListReadOnlyWithANotice() = runTest {
        val socks = server.wishlist.add("Socks")
        val model = viewModel()
        model.openEdit(socks)
        model.edit(WishField.Title, "Wool socks")
        server.wishlist.editable = false
        groupIs("archived")

        model.save()?.join()

        assertNull(model.ui.value.editor)
        assertEquals(WishlistNotice.Locked, model.ui.value.notice)
        assertFalse(model.editable)
        assertEquals(listOf("Socks"), model.titles())
    }

    @Test
    fun aListThatFilledUpElsewhereSaysItIsFull() = runTest {
        repeat(49) { server.wishlist.add("Item $it") }
        val model = viewModel()
        server.wishlist.add("The fiftieth, from another device")
        model.openAdd()
        model.fill(title = "One too many")

        model.save()?.join()

        assertEquals(WishlistNotice.ListFull, model.ui.value.notice)
        assertNull(model.ui.value.editor)
        assertEquals(50, model.data.value.value!!.items.size)
        assertFalse(model.canAdd)
    }

    @Test
    fun anItemDeletedElsewhereClosesTheSheetAndSaysSo() = runTest {
        val socks = server.wishlist.add("Socks")
        server.wishlist.add("Tea")
        val model = viewModel()
        model.openEdit(socks)
        model.edit(WishField.Title, "Wool socks")
        server.wishlist.removeBehindTheScenes(socks)

        model.save()?.join()

        assertNull(model.ui.value.editor)
        assertEquals(WishlistNotice.ItemGone, model.ui.value.notice)
        assertEquals(listOf("Tea"), model.titles())
    }

    @Test
    fun aDeleteOfAnItemAlreadyGoneSaysSoToo() = runTest {
        val socks = server.wishlist.add("Socks")
        val model = viewModel()
        model.openEdit(socks)
        model.requestDelete()
        server.wishlist.removeBehindTheScenes(socks)

        model.confirmDelete()?.join()

        assertEquals(WishlistNotice.ItemGone, model.ui.value.notice)
        assertEquals(emptyList(), model.titles())
    }

    @Test
    fun aNetworkFailureOnADeleteOrAMoveIsANoticeAndChangesNothing() = runTest {
        server.wishlist.add("A")
        val b = server.wishlist.add("B")
        val model = viewModel()
        server.unreachable = true

        model.moveUp(b)?.join()
        assertEquals(WishlistNotice.Unreachable, model.ui.value.notice)
        assertFalse(model.ui.value.moving)

        model.dismissNotice()
        model.openEdit(b)
        model.requestDelete()
        model.confirmDelete()?.join()

        assertEquals(WishlistNotice.Unreachable, model.ui.value.notice)
        server.unreachable = false
        assertEquals(listOf("A", "B"), model.titles())
    }

    @Test
    fun aNoticeCanBeDismissed() = runTest {
        val b = server.wishlist.also { it.add("A") }.add("B")
        val model = viewModel()
        server.wishlist.add("C")
        model.moveUp(b)?.join()
        assertNotNull(model.ui.value.notice)

        model.dismissNotice()

        assertNull(model.ui.value.notice)
    }
}

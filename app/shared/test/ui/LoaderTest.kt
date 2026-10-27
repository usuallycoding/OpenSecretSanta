package com.usuallycoding.opensecretsanta.ui

import com.usuallycoding.opensecretsanta.data.api.ApiError
import com.usuallycoding.opensecretsanta.data.api.ApiResult
import com.usuallycoding.opensecretsanta.ui.load.LoadState
import com.usuallycoding.opensecretsanta.ui.load.Loader
import com.usuallycoding.opensecretsanta.ui.load.loadMessage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LoaderTest {
    /** A fetch the test controls: every call waits for [release] and then answers with the next result. */
    private class ScriptedFetch(vararg results: ApiResult<String>) {
        private val queue = results.toMutableList()
        var calls = 0
        var gate: CompletableDeferred<Unit>? = null

        suspend fun fetch(): ApiResult<String> {
            calls++
            gate?.await()
            return queue.removeFirst()
        }
    }

    private fun ok(value: String) = ApiResult.Success(value)

    private fun failure(error: ApiError) = ApiResult.Failure(error)

    @Test
    fun theFirstLoadStartsAtOnceAndShowsProgressUntilItAnswers() = runTest {
        val fetch = ScriptedFetch(ok("groups")).apply { gate = CompletableDeferred() }

        val loader = Loader(immediateScope(), fetch::fetch)

        assertEquals(LoadState<String>(null, loading = true, error = null), loader.state.value)
        fetch.gate!!.complete(Unit) // the unconfined test scope finishes the load right here
        assertEquals(LoadState("groups", loading = false, error = null), loader.state.value)
        assertEquals(1, fetch.calls)
    }

    @Test
    fun aFailedFirstLoadHasNoDataAndCanBeRetried() = runTest {
        val fetch = ScriptedFetch(failure(ApiError.Network), ok("groups"))
        val loader = Loader(immediateScope(), fetch::fetch) // the first load has already failed

        assertEquals(LoadState<String>(null, loading = false, error = ApiError.Network), loader.state.value)

        loader.refresh().join()

        assertEquals(LoadState("groups", loading = false, error = null), loader.state.value)
        assertEquals(2, fetch.calls)
    }

    @Test
    fun aRefreshKeepsShowingTheOldValueWhileItRuns() = runTest {
        val fetch = ScriptedFetch(ok("before"), ok("after"))
        val loader = Loader(immediateScope(), fetch::fetch) // loads "before"
        fetch.gate = CompletableDeferred()

        val running = loader.refresh()

        assertEquals(LoadState("before", loading = true, error = null), loader.state.value)
        fetch.gate!!.complete(Unit)
        running.join()
        assertEquals(LoadState("after", loading = false, error = null), loader.state.value)
    }

    @Test
    fun aFailedRefreshKeepsTheOldValueAndReportsTheError() = runTest {
        val fetch = ScriptedFetch(ok("before"), failure(ApiError.Unexpected), ok("again"))
        val loader = Loader(immediateScope(), fetch::fetch) // loads "before"

        loader.refresh().join()

        assertEquals(LoadState("before", loading = false, error = ApiError.Unexpected), loader.state.value)

        loader.refresh().join() // the error is cleared as soon as the next try starts, and the value replaced on success
        assertEquals(LoadState("again", loading = false, error = null), loader.state.value)
    }

    @Test
    fun aRefreshWhileOneIsRunningIsIgnored() = runTest {
        val fetch = ScriptedFetch(ok("one"), ok("two")).apply { gate = CompletableDeferred() }
        val loader = Loader(immediateScope(), fetch::fetch)

        val first = loader.refresh()
        val second = loader.refresh()

        assertTrue(first === second)
        assertEquals(1, fetch.calls)
        fetch.gate!!.complete(Unit)
        assertEquals("one", loader.state.value.value)
    }

    @Test
    fun aRefreshStartedAfterOneFinishedRunsAgain() = runTest {
        val fetch = ScriptedFetch(ok("one"), ok("two"))
        val loader = Loader(immediateScope(), fetch::fetch) // loads "one"

        loader.refresh().join()

        assertEquals("two", loader.state.value.value)
        assertEquals(2, fetch.calls)
    }

    @Test
    fun errorsBecomeMessagesWithoutTechnicalDetails() {
        assertEquals(Strings.SERVER_UNREACHABLE, ApiError.Network.loadMessage())
        for (error in listOf(ApiError.Unexpected, ApiError.Unauthorized, ApiError.Rejected(500, "internal_error", "boom"), ApiError.RateLimited(60))) {
            assertEquals(Strings.SERVER_FAILED, error.loadMessage())
        }
        assertNull(LoadState<String>().error)
    }

    @Test
    fun aReplacedValueShowsAsLoadedAndALaterRefreshStillWorks() = runTest {
        val fetch = ScriptedFetch(ok("from the server"), ok("reloaded"))
        val loader = Loader(immediateScope(), fetch::fetch)

        loader.replace("changed locally")

        assertEquals(LoadState("changed locally", loading = false, error = null), loader.state.value)

        loader.refresh().join()

        assertEquals("reloaded", loader.state.value.value)
        assertEquals(2, fetch.calls)
    }

    @Test
    fun replacingClearsAnErrorLeftByAFailedRefresh() = runTest {
        val fetch = ScriptedFetch(ok("one"), failure(ApiError.Network))
        val loader = Loader(immediateScope(), fetch::fetch)
        loader.refresh().join()
        assertEquals(ApiError.Network, loader.state.value.error)

        loader.replace("two")

        assertNull(loader.state.value.error)
        assertEquals("two", loader.state.value.value)
    }
}

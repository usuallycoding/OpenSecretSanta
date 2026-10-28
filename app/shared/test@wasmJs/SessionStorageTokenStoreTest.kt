package com.usuallycoding.opensecretsanta.data.session

import kotlinx.browser.localStorage
import kotlinx.browser.sessionStorage
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** The web's token store against the browser's real `sessionStorage`. */
class SessionStorageTokenStoreTest {
    @BeforeTest
    fun clean() {
        sessionStorage.removeItem(SessionStorageTokenStore.KEY)
        localStorage.removeItem(SessionStorageTokenStore.KEY)
    }

    @AfterTest
    fun cleanUp() = clean()

    @Test
    fun anEmptyStoreReadsAsNothing() = runTest {
        assertNull(SessionStorageTokenStore().read())
    }

    @Test
    fun aWrittenTokenIsReadBack() = runTest {
        val store = SessionStorageTokenStore()

        store.write("token-1")

        assertEquals("token-1", store.read())
    }

    @Test
    fun aSecondInstanceReadsWhatTheFirstWroteLikeAPageRefreshDoes() = runTest {
        SessionStorageTokenStore().write("token-1")

        assertEquals("token-1", SessionStorageTokenStore().read())
    }

    @Test
    fun writingAgainReplacesTheToken() = runTest {
        val store = SessionStorageTokenStore()
        store.write("old")

        store.write("new")

        assertEquals("new", store.read())
    }

    @Test
    fun clearingRemovesTheToken() = runTest {
        val store = SessionStorageTokenStore()
        store.write("token-1")

        store.clear()

        assertNull(store.read())
        assertNull(sessionStorage.getItem(SessionStorageTokenStore.KEY))
    }

    @Test
    fun theTokenIsInSessionStorageAndNotInLocalStorage() = runTest {
        SessionStorageTokenStore().write("token-1")

        assertEquals("token-1", sessionStorage.getItem(SessionStorageTokenStore.KEY))
        assertNull(localStorage.getItem(SessionStorageTokenStore.KEY))
    }
}

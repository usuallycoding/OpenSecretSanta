package com.usuallycoding.opensecretsanta.data.session

import kotlinx.browser.localStorage
import kotlinx.browser.sessionStorage
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** The web's token store against the browser's real `sessionStorage` and `localStorage`. */
class BrowserTokenStoreTest {
    private val key = BrowserTokenStore.KEY

    @BeforeTest
    fun clean() {
        sessionStorage.removeItem(key)
        localStorage.removeItem(key)
    }

    @AfterTest
    fun cleanUp() = clean()

    @Test
    fun anEmptyStoreReadsAsNothing() = runTest {
        assertNull(BrowserTokenStore().read())
    }

    @Test
    fun aWrittenTokenIsReadBack() = runTest {
        val store = BrowserTokenStore()

        store.write("token-1", remember = true)

        assertEquals("token-1", store.read())
    }

    @Test
    fun aSecondInstanceReadsWhatTheFirstWroteLikeAPageRefreshDoes() = runTest {
        BrowserTokenStore().write("token-1", remember = false)

        assertEquals("token-1", BrowserTokenStore().read())
    }

    @Test
    fun rememberingKeepsTheTokenInLocalStorageOnly() = runTest {
        BrowserTokenStore().write("token-1", remember = true)

        assertEquals("token-1", localStorage.getItem(key))
        assertNull(sessionStorage.getItem(key))
    }

    @Test
    fun notRememberingKeepsTheTokenInSessionStorageOnly() = runTest {
        BrowserTokenStore().write("token-1", remember = false)

        assertEquals("token-1", sessionStorage.getItem(key))
        assertNull(localStorage.getItem(key))
    }

    @Test
    fun aTokenWithNoChoiceIsShortLivedWhenNothingWasStored() = runTest {
        BrowserTokenStore().write("token-1")

        assertEquals("token-1", sessionStorage.getItem(key))
        assertNull(localStorage.getItem(key))
    }

    @Test
    fun aRefreshedTokenStaysWhereTheOldOneWas() = runTest {
        val store = BrowserTokenStore()
        store.write("old", remember = true)
        store.write("new")
        assertEquals("new", localStorage.getItem(key))
        assertNull(sessionStorage.getItem(key))

        store.write("old", remember = false)
        store.write("newer")
        assertEquals("newer", sessionStorage.getItem(key))
        assertNull(localStorage.getItem(key))
    }

    @Test
    fun changingTheChoiceMovesTheTokenAndLeavesNoStaleCopy() = runTest {
        val store = BrowserTokenStore()
        store.write("a", remember = false)

        store.write("b", remember = true)

        assertEquals("b", store.read())
        assertEquals("b", localStorage.getItem(key))
        assertNull(sessionStorage.getItem(key))
    }

    @Test
    fun aStaleCopyInTheOtherStorageIsNotReadFirst() = runTest {
        sessionStorage.setItem(key, "stale")
        localStorage.setItem(key, "fresh")

        assertEquals("fresh", BrowserTokenStore().read())
    }

    @Test
    fun clearingRemovesTheTokenFromBothStorages() = runTest {
        sessionStorage.setItem(key, "a")
        localStorage.setItem(key, "b")

        BrowserTokenStore().clear()

        assertNull(BrowserTokenStore().read())
        assertNull(sessionStorage.getItem(key))
        assertNull(localStorage.getItem(key))
    }
}

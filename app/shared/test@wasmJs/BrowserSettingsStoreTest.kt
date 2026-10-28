package com.usuallycoding.opensecretsanta.data.settings

import kotlinx.browser.localStorage
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** The web's settings store against the browser's real `localStorage`. */
class BrowserSettingsStoreTest {
    private val key = BrowserSettingsStore.KEY_PREFIX + "appearance.mode"

    @BeforeTest
    fun clean() = localStorage.removeItem(key)

    @AfterTest
    fun cleanUp() = clean()

    @Test
    fun anEmptyStoreReadsAsNothing() {
        assertNull(BrowserSettingsStore().get("appearance.mode"))
    }

    @Test
    fun aValueIsKeptInLocalStorageAndReadBackByAnotherInstanceLikeAReloadDoes() {
        BrowserSettingsStore().put("appearance.mode", "Dark")

        assertEquals("Dark", localStorage.getItem(key))
        assertEquals("Dark", BrowserSettingsStore().get("appearance.mode"))
    }

    @Test
    fun writingAgainReplacesTheValue() {
        val store = BrowserSettingsStore()
        store.put("appearance.mode", "Dark")

        store.put("appearance.mode", "Light")

        assertEquals("Light", store.get("appearance.mode"))
    }
}

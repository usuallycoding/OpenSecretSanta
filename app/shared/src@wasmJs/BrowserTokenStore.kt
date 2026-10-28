package com.usuallycoding.opensecretsanta.data.session

import kotlinx.browser.localStorage
import kotlinx.browser.sessionStorage
import org.w3c.dom.Storage

/**
 * Keeps the token in the browser: in `localStorage` when the person chose to stay signed in (it survives closing the tab
 * and the browser) and in the tab's `sessionStorage` when not (it survives a refresh and is gone with the tab). It is in
 * one of the two at a time, and nothing goes to a cookie. A storage that cannot be reached (blocked, a sandboxed frame)
 * reads as "nothing stored" and ignores writes, so a broken storage can only cost a sign-in.
 */
class BrowserTokenStore : TokenStore {
    override suspend fun read(): String? = get(localStorage) ?: get(sessionStorage)

    override suspend fun write(token: String, remember: Boolean?) {
        // null (a refreshed token) stays where the token is now; with none stored, the short-lived place
        val longLived = remember ?: (get(localStorage) != null)
        val (keep, drop) = if (longLived) localStorage to sessionStorage else sessionStorage to localStorage
        try {
            keep.setItem(KEY, token)
        } catch (_: Throwable) {
            // not stored: the person signs in again after closing the tab
        }
        remove(drop)
    }

    override suspend fun clear() {
        remove(localStorage)
        remove(sessionStorage)
    }

    private fun get(storage: Storage): String? = try {
        storage.getItem(KEY)
    } catch (_: Throwable) {
        null
    }

    private fun remove(storage: Storage) {
        try {
            storage.removeItem(KEY)
        } catch (_: Throwable) {
            // nothing to remove if the storage cannot be reached
        }
    }

    companion object {
        /** The key the token is kept under. */
        const val KEY = "opensecretsanta.token"
    }
}

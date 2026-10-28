package com.usuallycoding.opensecretsanta.data.session

import kotlinx.browser.sessionStorage

/**
 * Keeps the token in the tab's `sessionStorage`: it survives a refresh of the page and is gone when the tab is
 * closed. Nothing is written to `localStorage` or to a cookie. A storage that cannot be reached (blocked, a
 * sandboxed frame) reads as "nothing stored" and ignores writes, so a broken storage can only cost a sign-in.
 */
class SessionStorageTokenStore : TokenStore {
    override suspend fun read(): String? = try {
        sessionStorage.getItem(KEY)
    } catch (_: Throwable) {
        null
    }

    override suspend fun write(token: String) {
        try {
            sessionStorage.setItem(KEY, token)
        } catch (_: Throwable) {
            // not stored: the person signs in again after a refresh
        }
    }

    override suspend fun clear() {
        try {
            sessionStorage.removeItem(KEY)
        } catch (_: Throwable) {
            // nothing to remove if the storage cannot be reached
        }
    }

    companion object {
        /** The key the token is kept under. */
        const val KEY = "opensecretsanta.token"
    }
}

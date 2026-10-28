package com.usuallycoding.opensecretsanta.data.settings

import kotlinx.browser.localStorage

/**
 * Device preferences in the browser's `localStorage`. They are not secrets, so unlike the sign-in token they may outlive
 * the tab. A storage that cannot be reached (blocked, a sandboxed frame) reads as "nothing stored" and ignores writes.
 */
class BrowserSettingsStore : SettingsStore {
    override fun get(key: String): String? = try {
        localStorage.getItem(KEY_PREFIX + key)
    } catch (_: Throwable) {
        null
    }

    override fun put(key: String, value: String) {
        try {
            localStorage.setItem(KEY_PREFIX + key, value)
        } catch (_: Throwable) {
            // not kept: the choice lasts for this run only
        }
    }

    companion object {
        const val KEY_PREFIX = "opensecretsanta."
    }
}

package com.usuallycoding.opensecretsanta.data.settings

/**
 * Where small preferences of the device are kept between runs (not secrets, not per account): `SharedPreferences` on
 * Android, `NSUserDefaults` on iOS, the browser's `localStorage` on the web. It is synchronous so that a preference can be
 * read before the first screen is drawn. Implementations must not throw for ordinary storage problems: [get] answers
 * null and [put] does nothing when the storage cannot be used.
 */
interface SettingsStore {
    fun get(key: String): String?

    fun put(key: String, value: String)
}

/** Keeps the preferences only for the life of the process, which is what tests and a storage that cannot be reached need. */
class MemorySettingsStore : SettingsStore {
    private val values = mutableMapOf<String, String>()

    override fun get(key: String): String? = values[key]

    override fun put(key: String, value: String) {
        values[key] = value
    }
}

package com.usuallycoding.opensecretsanta.data.settings

import platform.Foundation.NSUserDefaults

/** Device preferences in `NSUserDefaults`. */
class UserDefaultsSettingsStore : SettingsStore {
    private val defaults = NSUserDefaults.standardUserDefaults

    override fun get(key: String): String? = defaults.stringForKey(KEY_PREFIX + key)

    override fun put(key: String, value: String) {
        defaults.setObject(value, forKey = KEY_PREFIX + key)
    }

    private companion object {
        const val KEY_PREFIX = "opensecretsanta."
    }
}

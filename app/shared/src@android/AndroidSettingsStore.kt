package com.usuallycoding.opensecretsanta.data.settings

import android.content.Context

/** Device preferences in a private `SharedPreferences` file; a failing storage reads as "nothing stored" and ignores writes. */
class AndroidSettingsStore(context: Context) : SettingsStore {
    private val prefs = context.applicationContext.getSharedPreferences("opensecretsanta.settings", Context.MODE_PRIVATE)

    override fun get(key: String): String? = try {
        prefs.getString(key, null)
    } catch (_: Exception) {
        null
    }

    override fun put(key: String, value: String) {
        try {
            prefs.edit().putString(key, value).apply()
        } catch (_: Exception) {
            // not kept: the choice lasts for this run only
        }
    }
}

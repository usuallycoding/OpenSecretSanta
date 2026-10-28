package com.usuallycoding.opensecretsanta.data.settings

import com.usuallycoding.opensecretsanta.ui.theme.Appearance
import com.usuallycoding.opensecretsanta.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * The person's choice of appearance, kept on the device. It is read from the [store] when it is created, so the very
 * first screen can already use it, and written whenever it changes. A stored value that cannot be read counts as the default.
 */
class AppearanceSettings(private val store: SettingsStore) {
    private val _appearance = MutableStateFlow(
        Appearance(
            mode = store.get(MODE_KEY)?.let { stored -> ThemeMode.entries.firstOrNull { it.name == stored } } ?: ThemeMode.System,
            dynamicColor = store.get(DYNAMIC_KEY) == TRUE,
        ),
    )

    val appearance: StateFlow<Appearance> = _appearance.asStateFlow()

    fun setMode(mode: ThemeMode) {
        store.put(MODE_KEY, mode.name)
        _appearance.update { it.copy(mode = mode) }
    }

    fun setDynamicColor(on: Boolean) {
        store.put(DYNAMIC_KEY, if (on) TRUE else "false")
        _appearance.update { it.copy(dynamicColor = on) }
    }

    private companion object {
        const val MODE_KEY = "appearance.mode"
        const val DYNAMIC_KEY = "appearance.dynamic"
        const val TRUE = "true"
    }
}

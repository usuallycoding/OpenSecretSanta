package com.usuallycoding.opensecretsanta.ui.theme

/** Whether the app follows the device's light or dark setting or is held to one of them. */
enum class ThemeMode { System, Light, Dark }

/** How the person wants the app to look: the mode, and (Android 12 and later) the wallpaper's colours in place of the app's. */
data class Appearance(
    val mode: ThemeMode = ThemeMode.System,
    val dynamicColor: Boolean = false,
)

/** Dark when the mode says Dark, or says System and the device is dark. */
fun useDarkTheme(mode: ThemeMode, systemDark: Boolean): Boolean = when (mode) {
    ThemeMode.Dark -> true
    ThemeMode.Light -> false
    ThemeMode.System -> systemDark
}

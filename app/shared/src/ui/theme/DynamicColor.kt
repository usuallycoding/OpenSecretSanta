package com.usuallycoding.opensecretsanta.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

/** Whether the platform can colour the app from the wallpaper: Android 12 and later only. */
expect val dynamicColorSupported: Boolean

/** The wallpaper's colours in the light or [dark] variant, or null where [dynamicColorSupported] is false. */
@Composable
expect fun dynamicColorScheme(dark: Boolean): ColorScheme?

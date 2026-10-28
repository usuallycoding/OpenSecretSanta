package com.usuallycoding.opensecretsanta.ui

import com.usuallycoding.opensecretsanta.ui.theme.Appearance
import com.usuallycoding.opensecretsanta.ui.theme.ThemeMode
import com.usuallycoding.opensecretsanta.ui.theme.useDarkTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppearanceTest {
    @Test
    fun followingTheDeviceIsDarkOnlyWhenTheDeviceIs() {
        assertTrue(useDarkTheme(ThemeMode.System, systemDark = true))
        assertFalse(useDarkTheme(ThemeMode.System, systemDark = false))
    }

    @Test
    fun darkIsDarkWhateverTheDeviceSays() {
        assertTrue(useDarkTheme(ThemeMode.Dark, systemDark = true))
        assertTrue(useDarkTheme(ThemeMode.Dark, systemDark = false))
    }

    @Test
    fun lightIsLightWhateverTheDeviceSays() {
        assertFalse(useDarkTheme(ThemeMode.Light, systemDark = true))
        assertFalse(useDarkTheme(ThemeMode.Light, systemDark = false))
    }

    @Test
    fun theDefaultFollowsTheDeviceWithDynamicColoursOff() {
        assertEquals(Appearance(mode = ThemeMode.System, dynamicColor = false), Appearance())
    }
}

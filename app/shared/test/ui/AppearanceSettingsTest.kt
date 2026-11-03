package com.usuallycoding.opensecretsanta.ui

import com.usuallycoding.opensecretsanta.data.settings.AppearanceSettings
import com.usuallycoding.opensecretsanta.data.settings.MemorySettingsStore
import com.usuallycoding.opensecretsanta.data.settings.SettingsStore
import com.usuallycoding.opensecretsanta.ui.theme.Appearance
import com.usuallycoding.opensecretsanta.ui.theme.ThemeMode
import kotlin.test.Test
import kotlin.test.assertEquals

class AppearanceSettingsTest {
    @Test
    fun withNothingStoredTheAppFollowsTheDeviceWithDynamicColoursOff() {
        assertEquals(Appearance(ThemeMode.System, dynamicColor = false), AppearanceSettings(MemorySettingsStore()).appearance.value)
    }

    @Test
    fun whatWasStoredIsReadWhenTheSettingsAreCreatedSoTheFirstScreenCanUseIt() {
        val store = MemorySettingsStore().apply {
            put("appearance.mode", "Dark")
            put("appearance.dynamic", "true")
        }

        assertEquals(Appearance(ThemeMode.Dark, dynamicColor = true), AppearanceSettings(store).appearance.value)
    }

    @Test
    fun everyModeIsStoredAndReadBack() {
        for (mode in ThemeMode.entries) {
            val store = MemorySettingsStore()
            AppearanceSettings(store).setMode(mode)

            assertEquals(mode, AppearanceSettings(store).appearance.value.mode, mode.name)
        }
    }

    @Test
    fun theDynamicSwitchIsStoredAndReadBack() {
        val store = MemorySettingsStore()
        val settings = AppearanceSettings(store)

        settings.setDynamicColor(true)
        assertEquals(true, AppearanceSettings(store).appearance.value.dynamicColor)

        settings.setDynamicColor(false)
        assertEquals(false, AppearanceSettings(store).appearance.value.dynamicColor)
    }

    @Test
    fun theSnowIsOffByDefault() {
        assertEquals(false, AppearanceSettings(MemorySettingsStore()).appearance.value.snow)
    }

    @Test
    fun theSnowSwitchIsStoredAndReadBack() {
        val store = MemorySettingsStore()
        val settings = AppearanceSettings(store)

        settings.setSnow(true)
        assertEquals(true, settings.appearance.value.snow)
        assertEquals(true, AppearanceSettings(store).appearance.value.snow)

        settings.setSnow(false)
        assertEquals(false, AppearanceSettings(store).appearance.value.snow)
    }

    @Test
    fun theSnowDoesNotChangeTheOtherChoices() {
        val settings = AppearanceSettings(MemorySettingsStore())
        settings.setMode(ThemeMode.Dark)
        settings.setDynamicColor(true)

        settings.setSnow(true)

        assertEquals(Appearance(ThemeMode.Dark, dynamicColor = true, snow = true), settings.appearance.value)
    }

    @Test
    fun aChangeShowsAtOnceAndTheOtherChoiceIsKept() {
        val settings = AppearanceSettings(MemorySettingsStore())
        settings.setDynamicColor(true)

        settings.setMode(ThemeMode.Light)

        assertEquals(Appearance(ThemeMode.Light, dynamicColor = true), settings.appearance.value)
    }

    @Test
    fun valuesThatCannotBeReadCountAsTheDefaults() {
        val store = MemorySettingsStore().apply {
            put("appearance.mode", "Sepia")
            put("appearance.dynamic", "maybe")
            put("appearance.snow", "sometimes")
        }

        assertEquals(Appearance(), AppearanceSettings(store).appearance.value)
    }

    @Test
    fun aStorageThatKeepsNothingStillLetsThePersonChooseForThisRun() {
        val broken = object : SettingsStore {
            override fun get(key: String): String? = null

            override fun put(key: String, value: String) = Unit
        }
        val settings = AppearanceSettings(broken)

        settings.setMode(ThemeMode.Dark)

        settings.setSnow(true)

        assertEquals(ThemeMode.Dark, settings.appearance.value.mode)
        assertEquals(true, settings.appearance.value.snow)
        assertEquals(Appearance(), AppearanceSettings(broken).appearance.value)
    }
}

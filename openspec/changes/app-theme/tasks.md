# Tasks

## 1. The theme

- [x] 1.1 Fix `Theme.kt` and `Type.kt` so every platform builds (dynamic colour behind `expect/actual`, default fonts, the unused schemes and imports removed) and add `ThemeMode`, `Appearance` and the dark/light rule with unit tests for each combination of mode and device setting
- [x] 1.2 Add `SettingsStore` with the memory, Android (`SharedPreferences`), iOS (`NSUserDefaults`) and web (`localStorage`) implementations, `AppearanceSettings` (read at creation, written on change) and their tests (defaults, each stored value, unreadable values, a broken store), change `initKoin` to take a list of platform modules and pass both stores from the three hosts

## 2. The screens

- [x] 2.1 Replace the old `AppTheme` in `App.kt` with the new one driven by `AppearanceSettings`, add "Appearance" to the top-bar menu and the `AppearanceSheet` (three choices, the dynamic switch only where supported, applied at once, sheet back handling), with strings
- [x] 2.2 Add the Android day/night window theme and use it in the manifest, and the web page background for light and dark in `styles.css`

## 3. Check and document

- [x] 3.1 Verify in headless Chrome with the device set to light and to dark (emulated): default follows it, "Dark" and "Light" override at once and survive a reload and signing out, no dynamic switch on the web, the sign-in screen and both views use the palette; run `./kotlin build` for every target and `./kotlin test`, update the README, and note what could not be tried (dynamic colours and the Android window theme on a device, iOS)

## Workflow follow-up

- Archive the change after review and sync the delta specs into `openspec/specs`.

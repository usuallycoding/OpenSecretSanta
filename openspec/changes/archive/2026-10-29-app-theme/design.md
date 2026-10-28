# Design

## Context

The new files `ui/theme/Color.kt`, `Theme.kt` and `Type.kt` (a Material Theme Builder export) are in shared code. `Theme.kt` imports `android.os.Build`, `LocalContext` and `dynamicLightColorScheme`/`dynamicDarkColorScheme`, and defines four contrast schemes nobody uses; `Type.kt` uses `androidx.compose.ui.text.googlefonts` and the Android resource `R.array.com_google_android_gms_fonts_certs`. None of that compiles for iOS or the web. `App.kt` still has its own `AppTheme` that picks Material's stock `darkColorScheme()` or `lightColorScheme()` from `isSystemInDarkTheme()`, and `App(gate = …)` wraps the gate (the Android local-network screen) and the content inside it, so both already sit under the theme. Platform storage is injected by each host through `initKoin(baseUrl, platformModule)` (the token store); the Android manifest theme is `Theme.Material.Light.NoActionBar`; the web page has no background of its own before the app loads. The top-bar menu is `ShellMenu`, and "Change password" already opens a bottom sheet from `ShellScreen`.

## Goals / Non-Goals

**Goals:**
- Every platform builds and shows the same palette and type; the device's setting is followed by default.
- A light/dark choice and, on Android 12+, a dynamic-colour switch, remembered on the device and applied before the first frame.

**Non-Goals:**
- Medium and high contrast schemes (unused today; removed), a custom font bundle, a per-account theme, theming the web page's pre-load background by the in-app choice, changing any screen's layout.

## Decisions

1. **Fixing the theme files.** `Color.kt` stays as it is. `Theme.kt` keeps the light and dark `ColorScheme`s and `AppTheme(appearance: Appearance, content)` in shared code; the four contrast schemes, `ColorFamily`, `unspecified_scheme` and the unused imports go. Dynamic colour moves behind `expect val dynamicColorSupported: Boolean` and `expect @Composable fun dynamicColorScheme(dark: Boolean): ColorScheme?`: Android returns `Build.VERSION.SDK_INT >= S` and `dynamicDark/LightColorScheme(LocalContext.current)`; iOS and the web say `false` and `null`.
2. **Fonts.** `Type.kt` keeps `AppTypography` but with `FontFamily.Default` for both the display and the body families, so each platform shows its own default font (Roboto on Android, San Francisco on iOS, the browser's on the web) and the Google Fonts code and its certificate resource go. The type scale (sizes, weights) is Material's baseline, as in the file.
3. **The model.** `enum ThemeMode { System, Light, Dark }` and `data class Appearance(mode: ThemeMode = System, dynamicColor: Boolean = false)` in `ui/theme`, with a pure `resolveColorScheme`-style function `useDark(mode, systemDark)` so the rule is unit tested: Dark when the mode is Dark, or System and the device is dark. `AppTheme` picks dynamic when `appearance.dynamicColor && dynamicColorSupported`, otherwise the light or dark palette.
4. **Holder and storage.** `AppearanceSettings(store: SettingsStore)` (a Koin singleton) holds `val appearance: StateFlow<Appearance>`, read synchronously from the store when it is created and written on every `setMode`/`setDynamicColor`. `SettingsStore` is a small synchronous interface (`get(key): String?`, `put(key, value)`) that never throws for ordinary storage problems, with `MemorySettingsStore` as the default; Android uses `SharedPreferences`, iOS `NSUserDefaults` and the web `localStorage` (a preference, not a token). Values are the enum name and `"true"`/`"false"`; anything unreadable counts as the default. Synchronous reads let `App()` pick the right theme in its first composition.
5. **Wiring.** `initKoin(baseUrl, platformModules: List<Module> = listOf(tokenStoreModule { Memory }, settingsStoreModule { Memory }))` (the single `platformModule` becomes a list) and each host passes both its token store and its settings store. The old `AppTheme` in `App.kt` is removed; `App()` reads `koinInject<AppearanceSettings>()` and wraps everything in `ui.theme.AppTheme`.
6. **The sheet.** `ShellMenu` gets "Appearance" above "Change password"; `ShellScreen` keeps a flag like the password sheet's. `AppearanceSheet` is a `ModalBottomSheet` (via `SheetFrame`) with three radio rows (Follow device, Light, Dark) and, only when `dynamicColorSupported`, a "Dynamic colours" row with a `Switch`. Changes call `AppearanceSettings` and show at once, with no Save button. It takes part in the existing back and Escape handling for sheets.
7. **Android window.** `res/values/themes.xml` and `res/values-night/themes.xml` define `Theme.OpenSecretSanta` from `Theme.Material.Light.NoActionBar` and `Theme.Material.NoActionBar`, and the manifest uses it, so the window behind Compose is dark at night and there is no white flash. (A person who forces the opposite of the device sees the window colour of the device for the instant before the first frame.)
8. **Web.** `styles.css` gets `@media (prefers-color-scheme: dark) { html, body { background: <the dark palette's background> } }` and the light background otherwise, so the page does not flash before the app loads.
9. **Defaults.** Follow device, dynamic colours off, so the designed palette is what people see unless they ask for the wallpaper's.

## Risks / Trade-offs

- Dynamic colours and the Android 12+ check cannot be seen outside a device or emulator with Android 12+; the build and the pure rules are tested here and the rest is for a device check.
- With the in-app choice opposite to the device's, the Android window and the web page background show the device's colour for a moment before the first frame.
- The default font differs per platform, so text widths differ slightly between them.
- Removing the contrast schemes drops the (unused) option of honouring Android's contrast setting; `Color.kt` keeps the values should that be wanted later.

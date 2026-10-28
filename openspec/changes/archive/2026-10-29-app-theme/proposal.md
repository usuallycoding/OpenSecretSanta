# Proposal

## Why

The app has a designed palette and type scale (`ui/theme`: `Color.kt`, `Theme.kt`, `Type.kt`), but it still draws with Material's stock purple colours, and the new theme files cannot compile outside Android: they use Android-only APIs (`android.os.Build`, `LocalContext`, the dynamic-colour functions, Google Fonts and an Android resource). There is also no way for a person to choose light or dark, or to use their Android wallpaper colours.

## What Changes

- The app uses the new palette and typography everywhere. The theme files are fixed so every platform builds: dynamic colour sits behind a small per-platform check, the Google Fonts setup is dropped in favour of each platform's default font, and the unused contrast schemes and imports go.
- By default the app follows the device's light or dark setting. An "Appearance" item in the top-bar menu (both views) opens a sheet with Follow device, Light and Dark, applied at once.
- On Android 12 and later the sheet also has a "Dynamic colours" switch, off by default, that takes the colours from the wallpaper. Other platforms do not show it.
- The choice is remembered on the device (not per account), read before the first frame so the wrong theme never flashes.
- The Android window theme follows day and night, and the web page's background follows the device's setting before the app has loaded.
- No server change.

## Capabilities

### New Capabilities

- `client-appearance`: the palette, following the device, the light and dark choice, dynamic colours, and remembering the choice.

### Modified Capabilities

- `client-app`: the top-bar menu gains "Appearance".

## Impact

- `app/shared/src/ui/theme/*` (Theme.kt, Type.kt; Color.kt unchanged), `ui/App.kt` (the old `AppTheme` is replaced), `ui/shell/ShellScreen.kt` (menu item and sheet), a new settings store (`SettingsStore` with Android, iOS and web implementations) and `di/AppModules.kt` (`initKoin` takes a list of platform modules), the three host entry points, Android resources and manifest, the web `styles.css`, `Strings.kt`, tests and the README.

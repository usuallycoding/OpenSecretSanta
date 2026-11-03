# Tasks

## 1. The stored choice

- [x] 1.1 Add `snow` (false by default) to `Appearance` and a stored `appearance.snow` key with `setSnow` to `AppearanceSettings`, read when it is created and written on every change; extend `app/shared/test/ui/AppearanceSettingsTest.kt` with off by default, kept across a new `AppearanceSettings` on the same store, unreadable storage counting as off, and the mode and dynamic colour unaffected, and verify with `./kotlin test -m shared`

## 2. The snow

- [x] 2.1 Write the movement as a pure function in `app/shared/src/ui` (flakes in, elapsed time and size in, flakes out wrapped to the top and kept in the width, a seeded random source for the starting flakes, a flake count that follows the area with an upper limit) with tests for falling, wrapping, swaying inside the width, the count for a phone and a very large window, and repeatable starts; verify with `./kotlin test -m shared`
- [x] 2.2 Add the overlay: a full-size canvas with no pointer handling and no semantics, moved by the frame clock with the time read only while drawing, white on the dark theme and pale blue-grey on the light one, small, soft and fairly faint; draw it above the content in `App()` only while the switch is on; verify by building for Android, iOS and the web with `./kotlin build`

## 3. The switch

- [x] 3.1 Add the "Make it snow" switch to `AppearanceSheet.kt` on every platform, with its strings in `Strings.kt`, applying at once; verify that it is off on a fresh install, that switching it on and off starts and stops the snow, and that it is shown on the platforms with no dynamic colours
- [ ] 3.2 Check the snow on Android, iOS and the web: that it falls over sign-in, the groups, a group and Settings in both themes, that taps, scrolling and typing work through it, that sheets and pickers open over it, that it survives a restart and a sign-out, and that scrolling a long list with it on stays smooth

## 4. Specs and integration

- [x] 4.1 Run `openspec validate add-snow-toggle --strict` and verify it passes, then run `./kotlin test -m shared`, `./kotlin test -p jvm` and `./kotlin build` and verify they are green

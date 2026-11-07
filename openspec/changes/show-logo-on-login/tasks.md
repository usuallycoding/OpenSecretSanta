# Tasks

## 1. The artwork

- [x] 1.1 Copy `/Users/sometimes/Downloads/app_logo_custom.svg` into the repository as `assets/app-logo.svg`, unchanged, and add `scripts/generate-logo.sh`: it outlines the text with `inkscape --export-text-to-path --export-plain-svg` into a temporary SVG, makes the dark variant by recolouring every black (`#000` strokes and fills, in styles and attributes) to `#ffe9e4`, renders both with `rsvg-convert` at 840 px wide with a transparent background, trims the margin with ImageMagick, writes `app_logo_light.png` and `app_logo_dark.png` into the shared module's Compose resources, stops before writing anything when a tool or the source is missing and names it, and gives identical files on a second run.
- [x] 1.2 Run the script and look at both PNGs: on a white and a near-black background, the hat, "OPEN", "SECRET SANTA" and "DINCOLO" in their lettering, no black left in the dark one, a transparent background. Fix the recolouring until nothing black remains, and check the Compose resource folder name by building the shared module.

## 2. The screen

- [x] 2.1 Add `logoIsDark(background: Color)` (luminance under 0.5) next to the sign-in screen, and test it with the app's light and dark backgrounds, pure black and white, and mid-tones on either side of the threshold.
- [x] 2.2 Replace the title in `LoginScreen` with the logo image, choosing the dark or light picture with `logoIsDark(MaterialTheme.colorScheme.background)`, named with `Strings.APP_NAME`, at most 280dp wide and no wider than the fields, fit inside its box, keeping the spacing under it; leave `Strings.APP_NAME` for the notifications.
- [x] 2.3 Build for Android, iOS and the web and run the shared tests; fix any test that expects the title text.

## 3. Docs and verification

- [x] 3.1 Describe the script and `assets/app-logo.svg` in the README next to the icon script, and run `openspec validate --specs`.
- [x] 3.2 Look at the sign-in screen on a phone and on the web: in the light and the dark theme chosen in the app while the device is the other way round, with dynamic colours on Android 12 and later, and with the keyboard open.

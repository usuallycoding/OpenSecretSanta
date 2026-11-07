# Proposal

## Why

The sign-in screen introduces the app with the plain title "Open Secret Santa". There is now a logo for it (a Santa hat with the name on its brim), and the first screen is where it belongs. The logo's black outline would disappear on the dark theme, so it needs a light and a dark version.

## What Changes

- The sign-in screen shows the logo, `app_logo_custom.svg`, in the place of the title text. The text is not shown any more; the logo has the name in it, and the image is named "Open Secret Santa" for a screen reader.
- The logo has **two versions**: the light one is the artwork as drawn; the dark one has the same artwork with its black outline made light, so it reads on a dark background. The screen shows the one that fits the colours it is on, following the app's own light, dark or system choice, and not only the device's setting.
- The source SVG is kept in the repository's `assets` folder, and a script makes both pictures from it. The SVG draws its words in the macOS-only font Trattatello, which Android, iOS and the web do not have, so the script turns the words into outlines first and the pictures look the same everywhere.
- The pictures are PNGs in the app's shared resources, shown the same way on Android, iOS and the web, because Android cannot show an SVG from the shared resources.
- Nothing else changes: the fields, buttons, messages and version line on the sign-in screen stay as they are, and the name "Open Secret Santa" stays in notifications and everywhere else it is used as text.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `client-app`: "Sign in" says the sign-in screen opens with the logo and not the title text, that the logo has a spoken name, and that it comes in a light and a dark version that follow the theme the app is showing.

## Impact

- `app/shared/src/ui/login/LoginScreen.kt` (the image replaces the title), two new pictures in the shared resources, and tests for the choice of version and the screen's labels.
- `assets/app-logo.svg` (the source, as given) and `scripts/generate-logo.sh` (makes the two PNGs; needs `inkscape`, `rsvg-convert` and ImageMagick, which the icon script already asks for in part).
- `README.md` (the script, next to the icon script's description).
- No server, contract or domain change, and no change to the app icons.
- Assumptions made without asking: the word "DINCOLO" under the hat is part of the artwork and is shown with it; in the dark version only the black parts are recoloured (to a soft light pink-white), the red hat and the pink and white fills stay as they are.

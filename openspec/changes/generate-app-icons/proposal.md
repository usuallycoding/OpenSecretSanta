## Why

The app has no identity of its own: Android shows the default green robot, the iOS icon is a template with an alpha channel (which the App Store rejects), and the website has no favicon or manifest at all. There is a Santa-hat drawing, `secret-santa-icon.svg`, and no repeatable way to turn it into the dozen differently sized files the three platforms need.

## What Changes

- A script, `scripts/generate-icons.sh`, turns one SVG into every icon the app needs and writes them in place: the Android launcher icons (legacy PNGs at five densities, square and round, and an adaptive icon), the iOS app icon, and the website's favicon, touch icon, manifest icons and web manifest. Running it again gives the same files.
- The icons sit on the app's own theme colour, the light peach `#FFDBD1` (the theme's primary container), by default; `--background` takes another colour. The hat itself is transparent in the source, so the script adds the background where a platform needs an opaque icon.
- The iOS icon is flattened to 1024×1024 without transparency, and the Android adaptive icon keeps the hat inside the safe zone so no launcher mask cuts it.
- `index.html` gets the icon, touch-icon and manifest links once, by hand; the script only regenerates the images and the manifest.
- The source SVG moves to `assets/secret-santa-icon.svg` and is committed with the generated icons, so building the app never needs the script or its tools.
- The README says how to change the artwork and run the script.

## Capabilities

### New Capabilities

- `app-icons`: how the app's launcher, store and website icons are produced from one source SVG, what files they are, and what each platform needs of them.

### Modified Capabilities

None.

## Impact

- New: `scripts/generate-icons.sh`, `assets/secret-santa-icon.svg`, and the generated files under `app/androidApp/res/`, `app/iosApp/src/Assets.xcassets/AppIcon.appiconset/` and `app/webApp/resources/`. Changed: `app/webApp/resources/index.html`, `app/webApp/docker/nginx.conf` (the manifest's content type), `README.md`. Removed: the template's adaptive-icon vector files.
- The script needs `rsvg-convert` and ImageMagick (`magick`), which this Mac has (`brew install librsvg imagemagick` elsewhere); it checks for them and says so. Nothing else depends on them.
- The source SVG is a 614×680 bitmap placed on a 1024×1024 canvas, not vector paths, so the hat is sharp at that size and would soften if enlarged. A true vector export from the original file would allow larger or crisper icons later; the script takes any SVG.
- Not in scope: the Android notification (status-bar) icon, which must be a plain white silhouette and stays as it is; iOS dark and tinted icon variants; a monochrome themed Android icon; a Google Play store graphic; changing the app's name or colours.

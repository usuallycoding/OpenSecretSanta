## Context

`secret-santa-icon.svg` is a 1024×1024 canvas holding one embedded 614×680 PNG of a Santa hat (dark red body, pale pink brim and pompom, a thin outline) on a transparent background. Today Android has the default template (`mipmap-*` PNGs, a vector adaptive foreground and a grid-pattern vector background), iOS has `AppIcon.appiconset/app-icon-1024.png` with an alpha channel and entries for dark and tinted variants without files, and the web resources (`index.html`, `styles.css`, `push.js`, `firebase-messaging-sw.js`, `config.json`) have no icon, touch icon or manifest. The Android module's minSdk is 24, so launchers before 26 need the legacy PNGs. The Mac has `rsvg-convert`, ImageMagick 7 and `sips`; Docker's nginx knows the common image types.

## Goals / Non-Goals

**Goals:** one command from one SVG to all platforms' icons, repeatable, with results committed so builds need no tooling.

**Non-Goals:** the status-bar notification icon, dark or tinted iOS variants, a themed monochrome Android icon, a Play Store graphic, vectorising the artwork.

## Decisions

- **`rsvg-convert` renders, ImageMagick composes.** `rsvg-convert -w 1024 -h 1024` renders the SVG to a transparent master PNG; `magick` trims it to the artwork, scales and places it on each canvas, rounds or circles the legacy icons, writes the `.ico`, and strips metadata (`-strip`) so the output is byte-identical between runs. The script checks both tools first and names what to install.
- **One background colour, from the theme.** The default is `#FFDBD1`, the light theme's primary container: four candidates were rendered and the light peach made the red hat stand out most while staying in the app's palette. `--background` overrides it; the same value is the web manifest's `background_color`, and the manifest's `theme_color` is the theme's primary, `#8F4C38`.
- **Android.**
  - Legacy `ic_launcher.png` is a rounded square (corner radius about 18% of the side) with the hat at about 72% of the height; `ic_launcher_round.png` is a circle with the hat at about 62%, so it stays inside the circle.
  - The adaptive foreground is `drawable-nodpi/ic_launcher_foreground.png`, 432×432 (108dp at xxxhdpi), the trimmed hat fitted into the 66dp safe zone (264 px) and centred; the background is a solid-colour vector in `drawable/ic_launcher_background.xml`. The two adaptive XML files keep pointing at those names. The template's vector `drawable-v24/ic_launcher_foreground.xml` is deleted, because a vector with the same name in a more specific folder would win over the PNG.
- **iOS.** One 1024×1024 PNG, the hat at about 72% of the height on the background, flattened and written without an alpha channel (`-alpha off`) in sRGB, because the App Store rejects transparency. The dark and tinted entries keep having no file and fall back to it.
- **Web.** `favicon.ico` (16, 32, 48) shows the hat alone on a transparent background so the tab's colour shows through; `apple-touch-icon.png` (180), `icon-192.png` and `icon-512.png` sit on the background colour and are opaque. `manifest.webmanifest` is written by the script because it carries the colours. `index.html` gets `<link rel="icon" href="favicon.ico" sizes="any">`, `<link rel="apple-touch-icon" href="apple-touch-icon.png">`, `<link rel="manifest" href="manifest.webmanifest">` and a `theme-color` meta once, by hand, since it is HTML the script does not own. Files in the module's `resources/` are served as they are by the dev server and by the nginx image. nginx has no content type for `.webmanifest` (it would send a generic binary type), so `app/webApp/docker/nginx.conf` gets a rule for `/manifest.webmanifest`, as it already has for `.mjs`.
- **Source and results are committed**, the SVG under `assets/` (it leaves the repository root), and the generated PNGs, ICO, manifest and Android background beside the code that uses them, so the Gradle-free toolchain build and the Docker build never run the script.
- **The artwork is a bitmap.** At the 1024 canvas the hat is 1:1; the iOS icon scales it up about 8%, which is invisible. Larger output would need a vector source, which the script would accept unchanged.
- **Scope of writes.** The script writes only the files listed in the spec and nothing else, and takes the output folders relative to the repository root, so running it from any directory works.

## Risks / Trade-offs

- A light background makes the pale pink brim less distinct from it than a dark one would; the thin outline and the red body carry the shape, and `--background` changes it in one command.
- The adaptive icon's hat is smaller than a legacy one because of the safe zone; that is how launchers expect it.
- `manifest.webmanifest` needs a content type browsers accept; the nginx image and the dev server's handling are checked, and `.webmanifest` is the standard extension.
- ImageMagick's output can differ between versions, so a later run on another machine may change bytes; the committed files stay valid either way.

## Open Questions

None.

# Spec Delta

## Purpose
How the app's launcher, store and website icons are produced from one source SVG by a script, which files that makes for Android, iOS and the web, and what each platform needs of them.

## ADDED Requirements

### Requirement: One script makes every icon from one SVG
`scripts/generate-icons.sh` SHALL turn a source SVG, `assets/secret-santa-icon.svg` unless `--source` names another, into all the icon files of the Android, iOS and web apps and write them in place, using a background colour from `--background` (default `#FFDBD1`). It SHALL stop before writing anything, naming it, when the source or a tool it needs is missing, and running it twice SHALL give identical files.

#### Scenario: Default run
- **WHEN** the script is run with no options
- **THEN** every icon file of the three platforms is written from `assets/secret-santa-icon.svg` on the background `#FFDBD1`

#### Scenario: Another colour
- **WHEN** it is run with `--background "#8F4C38"`
- **THEN** the opaque icons and the web manifest use that colour

#### Scenario: Missing tool
- **WHEN** `rsvg-convert` or ImageMagick is not installed
- **THEN** the script stops before changing any file and says what to install

#### Scenario: Same result twice
- **WHEN** the script is run twice in a row
- **THEN** `git status` shows no change after the second run

### Requirement: Android launcher icons
The script SHALL write `ic_launcher.png` (a rounded square) and `ic_launcher_round.png` (a circle) at 48, 72, 96, 144 and 192 pixels into the `mipmap-mdpi` to `mipmap-xxxhdpi` folders of the Android module, an adaptive icon whose foreground is the artwork on a transparent 432×432 canvas with the artwork inside the 66dp safe zone and whose background is the solid colour, and SHALL remove the template's green robot files.

#### Scenario: Legacy icons
- **WHEN** the script has run
- **THEN** the five densities hold both icons at the sizes above, showing the hat on the background colour

#### Scenario: Adaptive icon
- **WHEN** the script has run
- **THEN** the foreground image holds the hat within the central 66dp of the 108dp canvas and the background is the solid colour

#### Scenario: Template gone
- **WHEN** the script has run
- **THEN** no file of the default green robot icon remains

### Requirement: iOS app icon
The script SHALL write `app-icon-1024.png` in `AppIcon.appiconset` as a 1024×1024 sRGB PNG with no alpha channel, showing the artwork on the background colour.

#### Scenario: Opaque
- **WHEN** the script has run
- **THEN** the iOS icon is 1024×1024 and has no transparency

### Requirement: Website icons
The script SHALL write `favicon.ico` (16, 32 and 48 pixels), `apple-touch-icon.png` (180 pixels, opaque), `icon-192.png`, `icon-512.png` and `manifest.webmanifest` (name, theme and background colours and both icons) into the web module's resources, and `index.html` SHALL link the favicon, the touch icon and the manifest so that a browser tab shows the hat and the page can be added to a home screen.

#### Scenario: Files
- **WHEN** the script has run
- **THEN** the five website files exist with the sizes above and the manifest names both icons

#### Scenario: Tab icon
- **WHEN** the web app is opened in a browser
- **THEN** the page's links name the favicon, and requesting each of them answers 200 with an image or manifest type

#### Scenario: Valid manifest
- **WHEN** a browser reads the manifest
- **THEN** it reports no errors and finds both icons

### Requirement: Artwork and results are kept in the repository
The source SVG SHALL live at `assets/secret-santa-icon.svg` and be committed together with the generated icons, so that building the apps and the Docker images needs neither the script nor its tools. The README SHALL say how to replace the artwork and run the script.

#### Scenario: Build without the tools
- **WHEN** the Android app and the web image are built on a machine without `rsvg-convert` or ImageMagick
- **THEN** they build and carry the icons

#### Scenario: Changing the artwork
- **WHEN** the SVG is replaced and the script is run
- **THEN** the icons of all three platforms show the new artwork

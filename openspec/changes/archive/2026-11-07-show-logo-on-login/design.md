# Design

## Context

The sign-in screen (`LoginScreen`) is a centred column no wider than 420dp that starts with `Text(Strings.APP_NAME)`. The shared module has Compose resources switched on (`$compose.components.resources`, a `resources` section with exposed accessors) but no resource files yet. The source SVG is a 168 KB Affinity export of 9775 by 7209 units: six paths and three `<text>` elements ("DINCOLO", "OPEN", "SECRET SANTA") set in `Trattatello`, a macOS-only font, in three fills (`#ffcbcb`, `#cf0000`, `#fffcfc`) with black outlines (`stroke:#000`). The app picks its light or dark theme itself (`AppTheme`: the app's Light, Dark or System setting against the device, and dynamic colours on Android 12 and later can replace the palette), and exposes no "is dark" flag.

## Goals / Non-Goals

**Goals:**
- The logo looks the same on Android, iOS and the web, reads on light and dark backgrounds, and can be remade from one source.
- The sign-in screen keeps working with the keyboard open on a small phone.

**Non-Goals:**
- Changing the app icons, the notification title or any other use of the name as text.
- Showing the logo anywhere but the sign-in screen, or redesigning the artwork.

## Decisions

**Outline the text first.** The script runs `inkscape --export-text-to-path --export-plain-svg` on the source into a temporary SVG, so no variant has live text. Live text would be drawn in whatever fallback font a platform has, and the lettering of the logo would change.

**Two variants made by recolouring.** The light version is the outlined artwork as it is. The dark version is the same file with every black (`#000`) outline recoloured to a soft light pink-white (`#ffe9e4`, near the logo's own pale fills) and nothing else touched; the red hat and the pink and white fills stay. A first test on the unoutlined source showed the hat reads well on a dark background with a light outline, but the lettering of "DINCOLO" kept a black edge in that test, so the script's recolouring must cover every black the file uses (stroke and fill, in styles and attributes), and the task that looks at the PNGs checks the lettering edge by edge. Alternative: a plate or card behind one logo; rejected because it adds a box to the first screen.

**PNGs in the shared resources, not the SVG.** Compose Multiplatform draws SVG resources on iOS and the web but not on Android, and its SVG reader does not draw text anyway. PNGs work everywhere with one call, `painterResource(Res.drawable.app_logo_light)` and `...dark`. Alternatives: a Compose `ImageVector` from the paths (a very long file of path data for 168 KB of artwork, hard to regenerate), or Android vector XML plus an SVG for the others (two kinds of asset to keep the same). The PNGs are 840 px wide, three times the 280dp they are shown at, trimmed of empty margin and transparent.

**Choosing by the colours it is drawn on, not by a resource qualifier.** Compose resources can pick a `-dark` file by the device's setting, but the app has its own Light, Dark and System choice and dynamic colours, so a qualifier would show the wrong logo whenever they disagree. The screen instead asks the theme it is inside: `MaterialTheme.colorScheme.background` with a luminance under 0.5 means the dark version. A pure function `logoIsDark(background: Color)` holds the rule, so it can be tested with the app's own light and dark backgrounds and with a few dynamic-looking colours. Alternative: expose an `isDark` flag from `AppTheme`; rejected as a bigger change to the theme for one use, and it would not cover dynamic colours' real background.

**One script makes them.** `scripts/generate-logo.sh [--source assets/app-logo.svg]` outlines, recolours, renders with `rsvg-convert`, trims with ImageMagick and writes `app_logo_light.png` and `app_logo_dark.png` into the shared resources. It stops before writing anything when a tool or the source is missing and names it, and gives identical files when run twice, like `generate-icons.sh`. `assets/app-logo.svg` is the source as given; the outlined and recoloured SVGs are temporary, so there is one source of truth.

**How it is shown.** `Image(painterResource(if (dark) Res.drawable.app_logo_dark else Res.drawable.app_logo_light), contentDescription = Strings.APP_NAME, modifier = Modifier.widthIn(max = 280.dp).fillMaxWidth(), contentScale = ContentScale.Fit)` in place of the title, with the spacing below it that the title had. The width limit keeps it narrower than the fields, and the screen already scrolls (`App.kt` wraps it in a vertical scroll), so a small phone with the keyboard open can still reach the button. `Strings.APP_NAME` stays: notifications use it.

**Where the resource folder goes.** Amper reads Compose resources from the module's `composeResources` folder (to be confirmed by the first build; if it names another folder, the script and the tasks use that one).

## Risks / Trade-offs

- [A black edge is missed by the recolouring] → the task that looks at the dark PNG checks every part of the artwork, and the script recolours every black form it finds.
- [Dynamic colours give a mid-tone background] → the threshold is on luminance, so a mid colour picks whichever side it is nearer; both versions are readable on a mid background, and the sign-in screen's background is the theme's own, which Material keeps clearly light or dark.
- [The PNG is blurred on a very dense or zoomed screen] → 840 px for at most 280dp is three times the size; the web at 200 percent zoom is still sharp.
- [`inkscape` is missing on another machine] → the script checks its tools first and says what to install, and the PNGs are committed, so nobody needs it just to build.
- [The resource folder name is wrong] → the first build shows it at once (the generated `Res.drawable` accessor is missing); the fix is a path change.
- [A change of the logo later] → replace the SVG and run the script again.

## Migration Plan

None: no data, API or setting changes. An older app simply shows the old title.

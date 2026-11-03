## Context

Amper 0.13.0 builds the app with Compose Multiplatform 1.12.1 by default. `ComposeSettings` in Amper's module schema has a `version` setting that overrides the default, set per module. Compose draws the web app on a canvas and keeps a hidden HTML input for the keyboard; on iOS 27 Safari the keyboard does not open for it, and the library's 1.12.2 release says it adapts that input's focus behaviour for Safari 27.0.

## Goals / Non-Goals

**Goals:** the keyboard opens on iOS 27 Safari when a text field is tapped, with nothing else about the app changing.

**Non-Goals:** a workaround script in the page, moving to the 1.13 alphas, or changing any screen.

## Decisions

- **The patch release, not an alpha.** 1.12.2 is the latest stable one and carries the Safari fix; the 1.13 line has it only in alphas.
- **The version is set in every Compose module.** `settings.compose.version: 1.12.2` in the shared, web, Android and iOS modules, so they cannot end up on different releases of the library.
- **The back handler follows.** `compose-ui-backhandler` in `libs.versions.toml` goes to 1.12.2 for the same reason its comment gives.
- **No spec change.** Nothing the app promises is different, so the change sets `skip_specs`.
- **If it does not fix it.** The next step is Safari's remote inspector on the phone, to see whether the hidden input exists and has focus after a tap; only then would a page script be considered.

## Risks / Trade-offs

- Checked after the bump: every `org.jetbrains.compose` library the web app resolves moved from 1.12.1 to 1.12.2, and Material 3 stays at 1.12.0-alpha03, which Amper pins separately, so it did not move.
- A new library release can change behaviour on any platform, so the whole test suite and a look at each platform's main screens are part of the change.
- Overriding the version might move other Compose libraries that Amper resolves from the same release, such as Material 3; the resolved versions are checked after the change.
- The fix is aimed at Safari 27.0; a later Safari could behave differently again.

## Outcome

On an iPhone with iOS 27, 1.12.2 made the keyboard open on some taps but not all, so the upgrade helps without fixing the problem. In Safari's "Request Desktop Website" mode the keyboard always opens, which points at how mobile-mode Safari 27 delivers the tap and the `focus()` call rather than at a Compose code path, because the web layer has no check for the browser's identity. The 1.12.2 change is that the second focus request, made in an animation frame, is skipped when the input is already active, since Safari 27 treats it as a new request outside user interaction. What is left is probably in the first request's timing or in the library's suppression of the compatibility mouse events on `touchend`; neither was diagnosed. The upgrade is kept, and the rest is left open.

# Design

## Context

`AppearanceSettings` holds an `Appearance` (mode and dynamic colour) read from the device's `SettingsStore` when it is created and written on every change; `App()` collects it and wraps everything in `AppTheme`. `AppearanceSheet` shows the choices. See proposal.md for the behaviour and the delta spec for the requirements.

## Goals / Non-Goals

**Goals:**
- A snow effect in common Compose code, identical on Android, iOS and the web, that costs nothing when it is off.
- A movement model that can be tested without a screen.

**Non-Goals:**
- No per-platform code and no new dependency.
- No honouring of the system's reduce-motion setting, and no snow inside the windows the platform opens for sheets and dialogs.
- No settings for how much snow, its speed or its colour.

## Decisions

- **The switch is another field of `Appearance`,** stored under its own key beside the mode and dynamic colour, so it is read before the first frame, belongs to the device and survives sign-out like them. Anything unreadable counts as off.
- **One overlay at the top of `App()`:** the content stays as it is and, above it in a `Box`, a full-size `Canvas` is drawn only while the switch is on. Putting it in `App()` and not in the shell covers the sign-in screens as well. Alternative: draw inside each screen. Rejected: many places to keep in step and no snow on sign-in.
- **The overlay takes no input and has no semantics.** A canvas with no pointer handling lets every tap and drag through to what is under it, and a screen reader has nothing to read.
- **A fixed set of flakes moved by the frame clock.** Each flake has a position, a size, a fall speed that grows with its size, a sway (amplitude, speed and phase) and an opacity; smaller ones are slower and fainter, which gives depth. Time is read only while drawing, so a frame redraws the canvas and recomposes nothing. The frame clock stops when the app is not shown, on Android, iOS and in a hidden browser tab, so the effect uses no battery then.
- **The number of flakes follows the window's area** with an upper limit, so a phone and a wide desktop window look about equally dense and a huge window does not draw thousands.
- **A pure step function.** Given the flakes, the elapsed time and the size, it returns the flakes moved, wrapped to the top when they pass the bottom and kept inside the width; a seeded random source makes the starting flakes repeatable. The function and the flake count are unit-tested; the look is checked by eye on each platform.
- **Colour by theme:** white flakes on the dark theme and a pale blue-grey on the light one, soft-edged and fairly faint, chosen from whether the dark variant is in force. White on the light theme would not show.
- **Sheets and dialogs sit above the snow.** The platform shows them in their own window on Android and iOS, so the overlay cannot be drawn over them; this is accepted.

## Risks / Trade-offs

- [Flakes over text can distract] → Small, soft and faint, and a switch that is off by default.
- [Constant redrawing costs battery and can drop frames on a weak device] → One canvas, no recomposition, a capped flake count, and it is off by default; verified by scrolling a long list with the snow on.
- [Skia on the web is slower than on a phone] → The cap keeps the draw small; checked in a browser.

## Migration Plan

None: an unset key means off, so existing devices are unchanged. Rollback is reverting the app code.

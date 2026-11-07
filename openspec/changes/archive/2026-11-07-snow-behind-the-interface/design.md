# Design

## Context

`App` draws the app and then, after it, `SnowOverlay()`, so the snow is the top layer. The app's colour comes from one `Surface(color = background)` in `AppContent`, and the signed-in shell paints its own background again (`BoxWithConstraints(...background(...))`) with a `Scaffold` whose container colour is the theme's (opaque). The bars (`CenterAlignedTopAppBar`, `NavigationBar`, `PermanentDrawerSheet`) and cards have opaque fills of their own. Bottom sheets, dialogs and pickers are shown by the platform in their own windows.

## Goals / Non-Goals

**Goals:**
- Snow behind the interface: seen in the open space, hidden by anything with a fill, including the bars.
- No change to how the snow moves, looks or is switched.

**Non-Goals:**
- Making the bars transparent (decided: they hide the snow), a dark or light variant of the snow, or touching the card pattern.

## Decisions

**Snow is the first layer inside the background surface.** `AppContent` draws `Surface(color = background) { Box { if (snow) SnowOverlay(); <the screens> } }`, so the background is painted first, the snow over it and the screens over the snow. `AppContent` is told whether the snow is on (from `App`, which already reads the appearance), and `App` no longer draws a snow layer of its own. Compose draws siblings in order, so no z-index is needed, and the overlay still takes no input, so touches reach the screens as before. Alternative: keep the overlay on top and clip it by what is drawn (a mask of the content); rejected as complex and costly every frame.

**The shell stops painting an opaque background of its own.** The `background(...)` modifier on the shell's `BoxWithConstraints` and the `Scaffold`'s `containerColor` become transparent, so the open space of a screen shows the snow over the surface's background. The surface already has the same colour, so nothing changes for anyone with the snow off. Alternative: make every screen transparent one by one; not needed, only these two fills cover the whole window.

**Bars hide the snow by being what they are.** The top bar, the tab bar and the sidebar keep their opaque container colours, so flakes pass behind them without any code. Cards and sheets likewise. This is the decision made for the change; making a bar transparent later is a one-line change of its colour.

**The sign-in screens.** They sit directly in the surface's box without a fill of their own, so the snow shows behind them with no further change.

**Every outlined field on a screen gets a fill, from one place.** An outlined text field is transparent inside, so a flake would show behind what is typed. A single `appFieldColors()` in `form/FormComponents.kt` returns `OutlinedTextFieldDefaults.colors(...)` with the focused, unfocused, disabled and error container colours all set to `MaterialTheme.colorScheme.background` (read at use, so the light and dark themes and dynamic colours follow), and every outlined field passes it: `SheetTextField` (which `MultiLineTextField` and the search fields use), the password field, and the five fields that call `OutlinedTextField` directly (sign-in, letters, group form, group picker, delivery info).

**Sheets and dialogs are left exactly as they are.** They are opaque and over the snow, so their fields need no fill and nothing in them is coloured. A composition local, `LocalFieldsOverSnow` (true by default, so a screen's fields are filled), is turned off inside `AppBottomSheet`'s content (a bottom sheet, and the dialog it becomes on a wide window) and around the one field in a plain dialog (the hand-over password); with it off, `appFieldColors()` returns the usual `OutlinedTextFieldDefaults.colors()`. Alternative: look at where each field is used and pass the fill only on screens; rejected, since `SheetTextField` is used both in sheets and on screens and the same field would need two forms.

**No change to the snow itself.** `SnowOverlay`, `Snowfall` and the settings stay as they are, so their tests stay. A check by eye on both themes is part of the tasks.

## Risks / Trade-offs

- [A flake shows inside a card or an element with no fill] → most surfaces are filled, and the outlined fields on screens now are too; anything else found when trying it is given a fill.
- [A new sheet or dialog that does not go through `AppBottomSheet` gets filled fields] → the fill is the page background, a small difference in shade; such a dialog turns the local off as the hand-over dialog does.
- [The shell without its background fill shows something else under it] → the surface under it has the same colour; the Android edge-to-edge areas behind the system bars are the window's own background, as before.
- [Less snow seen on a screen full of cards] → that is the intent; the open space around and between cards still shows it.

## Migration Plan

None: no data, API or setting changes.

## Context

Every bottom sheet is a Material `ModalBottomSheet` with `rememberAppSheetState()`, created in four places: `SheetFrame` (read-only sheets and the announcement and password sheets), `TextSheet`, `ItemSheetHost` and the participants sheet in `AdminGroupScreen`. Text sheets use `FormColumn`, whose `dismissKeyboardOnDrag` closes the keyboard only when the drag actually scrolls the form. A drag that does not scroll goes to the sheet's own drag handling, which can close it, and the dimmed area above the sheet handles taps by closing the sheet but ignores drags.

## Goals / Non-Goals

**Goals:** while the keyboard is open, a swipe on the sheet and a tap on the dimmed area only hide the keyboard; one place implements this for every sheet.

**Non-Goals:** swipes on the dimmed area, changing what a sheet does with the keyboard closed, taps on the sheet's own body, or the Android back button (the system already hides the keyboard first).

## Decisions

- **One shared sheet.** `AppBottomSheet(onDismiss, content)` in `SheetSupport.kt` wraps `ModalBottomSheet` and `rememberAppSheetState()` and replaces the four direct uses. Read-only sheets get it too: it does nothing unless the keyboard is open, which is why they are unchanged.
- **Knowing the keyboard is open.** `WindowInsets.ime.getBottom(density) > 0`, read inside the sheet's content, because the sheet is a dialog window and the keyboard's insets go to the window with the focus: read from the window around the sheet, as `ShellScreen` does, they stay zero while a sheet is open. The sheet's content writes the value to a state the rest of `AppBottomSheet` reads. Where a platform reports no keyboard, the sheet behaves as before.
- **Result of the spike (Material 3 1.12 sources).** `rememberBottomSheetState` takes `confirmValueChange`, and both a tap on the scrim and the sheet's swipe down ask it before the sheet hides. `ModalBottomSheet` also takes `sheetGesturesEnabled`. The scrim handles taps only, and the window that holds it (`ModalBottomSheetDialog`) is internal, so nothing the app writes sits over the dimmed area.
- **The dimmed area's tap and the swipe down.** `AppBottomSheet` passes a `confirmValueChange` that, for a move to Hidden while the keyboard is open, hides the keyboard (through a hider the sheet's own content fills in, because the text fields live in the sheet's window) and returns false. Nothing closes, and the sheet settles back.
- **Swipes on the sheet.** While the keyboard is open `sheetGesturesEnabled` is false, so the sheet cannot be dragged, and a pointer watcher around the content hides the keyboard when a finger moves past the touch slop in any direction and no child used the drag. A field that scrolls or selects its text and a button consume the drag, so they keep working. A form's own `verticalScroll` consumes every vertical drag, even with nothing to scroll, so vertical drags are judged by what is left of them: a nested-scroll hook around the content hides the keyboard when the content could not scroll as far as the finger moved. A field dragged past the end of its own text passes the rest on in the same way, and that also hides the keyboard.
- **Swipes on the dimmed area: not covered, by decision.** Only a platform hook can see them: on Android the dialog window's callback, on iOS and the web whatever the Compose scene offers. That would add platform code to every sheet and cannot be tested here, so a swipe on the dimmed area does nothing and only its tap hides the keyboard.
- **Scrolling forms keep their behaviour.** `dismissKeyboardOnDrag` still scrolls the form and closes the keyboard; the new layer only stops the sheet from moving.

## Risks / Trade-offs

- A touch layer over the sheet's window can swallow taps meant for buttons if it is not transparent to taps; the spike checks that Cancel, Save and the fields still work.
- iOS and Android report the keyboard differently; the behaviour is checked on both, and the web is checked on a phone browser.
- Ignoring the Android back button means one back press hides the keyboard through the system and a second closes the sheet; that is the system's own behaviour and stays.

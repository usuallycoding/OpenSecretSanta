# Design

## Context

`ReadOnlyText` is a row of the text, which takes the remaining width, and the copy icon at its end. Both Delivery Info sheets (`MySheet` and `ReceiverSheet` in `PairSheets.kt`) use it for the username with a headline style. `GroupPage` shows the sheets but has no navigation controller; `ShellScreen` owns it and opens tabs with `openTab`. See proposal.md for scope.

## Goals / Non-Goals

**Goals:**
- The name sits in the middle of the sheet, not shifted by the copy icon.
- The button reaches the Profile tab the way the tab bar does.

**Non-Goals:**
- No change to the other uses of `ReadOnlyText`, which stay start-aligned.
- No change to the Santa sheet or to what the sheets load.

## Decisions

- **A `centered` option on `ReadOnlyText`, with a balancing spacer.** Centering the text inside its own weight would put it left of the middle, because the copy icon takes room at the end. When `centered` is set the row starts with a spacer as wide as the icon, so the name is centered on the sheet and the icon hangs at the end. Alternative: put the icon on its own line under the name; rejected because it changes the look and the touch layout more than asked.
- **The cut-and-open behaviour stays.** A name longer than a row is still cut with an ellipsis and opens the full-text dialog; the option only changes the alignment.
- **`GroupPage` takes an `onEditProfile` callback.** `ShellScreen` passes `navController.openTab(ProfileRoute)`, the same call the tab bar uses, so the back stack and the saved state behave as when the tab is chosen. Alternative: give `GroupPage` the controller; rejected, as it would couple the page to navigation it does not otherwise need.
- **The sheet is closed first.** The button dismisses the sheet and then calls the callback, so the sheet does not stay over the Profile tab. The sheets are not screens (see `client-group-pages`), so the address does not change while one is open.
- **The button lives with the details.** It is drawn inside the loaded content of `MySheet`, after the card or the "no profile" note, so it is absent while the sheet is loading or has failed, where "Try again" is the action.

## Risks / Trade-offs

- [The spacer width must match the copy icon, or the name is off-center] → derive both from the same constant instead of two numbers.
- [Returning from the Profile tab goes to wherever the tab bar goes, not back to the group] → the same as choosing the tab; a back-to-group path is a separate change if wanted.

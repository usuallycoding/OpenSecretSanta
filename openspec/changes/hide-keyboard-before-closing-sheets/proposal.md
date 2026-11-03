# Proposal

## Why

With the keyboard open in a bottom sheet, a swipe on a short sheet that does not scroll drags the sheet itself, and a tap on the dimmed area above it closes the sheet, both with the keyboard still open and what was typed possibly lost. The person who swipes or taps outside the keyboard almost always means "put the keyboard away", not "leave the sheet".

## What Changes

- While the keyboard is open in a bottom sheet, a swipe or drag in any direction on the sheet hides the keyboard and does nothing else: the sheet is not moved or closed by that gesture.
- In the same state a tap on the dimmed area outside the sheet hides the keyboard and does not close the sheet. A swipe on the dimmed area does nothing: it is outside what the app can reach without platform-specific code.
- With the keyboard closed, sheets behave as they do today: drag to close, tap the dimmed area to close, forms scroll.
- A form that scrolls still scrolls under the drag and closes the keyboard, as "Dragging dismisses the keyboard" already says; the new rule adds that the sheet itself stays put.
- Every bottom sheet goes through one shared sheet composable, so the rule holds for each sheet that takes text (wishlist item, question and answer, participants, announcement, change password) and does nothing for the read-only ones, where the keyboard never opens.

## Capabilities

### New Capabilities

### Modified Capabilities
- `client-app`: a gesture outside the keyboard only hides the keyboard while it is open in a bottom sheet.

## Impact

- `app/shared/src/ui/form/SheetSupport.kt` (the shared sheet) and the sheets that call `ModalBottomSheet` directly: `SheetFrame.kt`, `TextSheet.kt`, `ItemSheetHost.kt` and `AdminGroupScreen.kt`.
- No server, API or data change.

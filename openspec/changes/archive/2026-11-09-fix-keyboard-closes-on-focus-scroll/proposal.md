# Proposal

## Why

On Android, tapping a text field can open the keyboard and have it close again at once. It happens whenever the keyboard's arrival makes the form scroll the focused field into view: always for the last field (Delivery information) on a portrait phone, and for every field on a short screen such as a phone in landscape. The spec already says the app's own scrolling to bring a field into view must not close the keyboard; the app-wide dismiss hook breaks that rule, so people cannot edit their delivery info.

## What Changes

- The app-wide keyboard-dismiss hook (`KeyboardDismissRoot`) closes the keyboard on a scroll only while a finger is on the screen, as the form's own hook (`dismissKeyboardOnDrag`) already does. The framework reports the bring-into-view scroll as user input with no finger down, so it no longer counts as a drag.
- The finger-down state the root already tracks (`GestureTracker`) is exposed to the scroll hook, so the root has one source for "a finger is down".
- A real drag that scrolls a form or a list still closes the keyboard, as before.
- Regression tests cover a scroll with no finger down (keyboard stays), a scroll with a finger down (keyboard closes), and a short screen where every field needs the scroll.
- The two keyboard requirements in `client-app` state the rule for the whole app, and gain scenarios for a short screen and for the app-wide hook.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `client-app`: "Dragging dismisses the keyboard" and "Swiping anywhere closes the keyboard" state that the app's own scroll to bring a field into view never closes the keyboard, on any screen size, and gain scenarios for a short (landscape) screen.

## Impact

- Code: `app/shared/src/ui/form/KeyboardDismiss.kt` (`GestureTracker`, `scrollHook`, `KeyboardDismissRoot`); `SheetSupport.kt` also uses `scrollHook` and must keep working.
- Tests: `app/shared/test/ui/GestureTrackerTest.kt` and a test for the scroll hook.
- No server, API or data changes; iOS and web behave as before (their keyboard does not trigger this scroll).
- Out of scope: the form-level hook in `FormComponents.kt`, which is already correct, and any change to the working-copy edits in `ServerAddress.kt` and the network config.

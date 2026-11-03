# Proposal

## Why

The search fields on the Groups tab, the admin groups list, the users list and a group's participants can only be put away with the keyboard's own search key; swiping the list or tapping beside the field leaves the keyboard open and covering the list. The app already closes the keyboard on a drag in its forms and its sheets, but not on screens with lists, and the people who use the app expect a swipe or a tap outside the field to put the keyboard away everywhere.

## What Changes

- On every screen of the app that is not a sheet or a dialog, a swipe or drag in any direction closes the keyboard, wherever on the screen it starts, outside the keyboard itself.
- A vertical swipe on a list scrolls the list and also closes the keyboard; pulling a list down to reload still reloads it.
- A tap on anything that does not handle it, such as empty space or plain text, closes the keyboard. A tap on a text field still focuses it, and a tap on a button, chip or card still does its own action.
- A drag inside a text field that moves the cursor, selects text or scrolls the field's own text does not close the keyboard.
- One modifier on the app's root does this for the signed-in shell and for the sign-in and password-change screens, instead of each screen doing it. The sheets keep their own rule from `client-app`: a swipe on the sheet or a tap on the dimmed area only closes the keyboard.
- The drag-watching pieces of `AppBottomSheet` and `FormColumn` move into one shared place, so there is a single definition of a drag that closes the keyboard.

## Capabilities

### New Capabilities

### Modified Capabilities
- `client-app`: swiping or tapping outside a text field closes the keyboard on every screen.

## Impact

- `app/shared/src/ui/form/SheetSupport.kt` and `FormComponents.kt` (the drag pieces move out), a new `app/shared/src/ui/form/KeyboardDismiss.kt`, and `app/shared/src/ui/App.kt` (the modifier on the root).
- All screens with a text field are affected: Groups, admin groups, users, a group's participants, Profile, the group form, sign-in and change password.
- No server, API or data change.

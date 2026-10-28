# Proposal

## Why

Every screen that loads something has a "Refresh" text button in the top-right corner. On a phone that is an awkward place to reach and it takes a row of the screen on every page; the natural gesture for reloading a list is to pull it down. On the web the button is redundant now that a browser refresh keeps the person signed in on the same page (the previous change), and the address of each screen is already in the address bar, so the browser's own refresh is the web's refresh.

## What Changes

- **Pull to refresh on Android and iOS.** The screens that had a Refresh button reload when the person pulls the content down, showing the standard pull indicator until the answer arrives. The button is gone from them.
- **No refresh control on the web.** The browser's refresh reloads the page, and the person stays signed in and on the same screen.
- **Unchanged.** Screens still reload when the app returns to the foreground, still keep what they show while a reload runs or fails, and still show "Try again" when something cannot be loaded. The automatic reload keeps its quiet thin progress bar; the pull indicator is shown only for a pull.
- **Short and empty screens can be pulled too.** A screen with little or nothing to show ("You have not added anything yet") is made scrollable so the gesture works there as well.

Out of scope:
- The Profile tab (it has no Refresh button today and a reload must not discard unsaved edits), bottom sheets (each part reloads when opened and has its own "Try again"), and cleaner web addresses (the next change).

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `client-app`: a screen can be refreshed by pulling it down on Android and iOS, and has no refresh control on the web.
- `client-groups`, `client-wishlist`, `client-admin-groups`, `client-admin-users`, `client-receiver-and-questions`: their loading, errors and refreshing requirements point at that behavior instead of a Refresh action.

## Impact

- Code: `LoadContent` in `app/shared/ui/load` gains the pull gesture; a platform flag says whether the platform has it (Android and iOS yes, web no); a scrollable empty-state composable; the eleven screens that had a Refresh button lose it; the unused string goes; README.
- APIs: none. Server: none. Dependencies: none (`PullToRefreshBox` is in the Material 3 already used).
- Platforms: Android and the web are checked by running them; iOS is checked by the person running the app.

    # Tasks

## 1. The gesture

- [x] 1.1 Add `hasPullToRefresh` to `Platform` (true on Android and iOS, false on the web), add an `onRefresh` parameter to `LoadContent` that wraps the content in `PullToRefreshBox` where the platform has it (the pull indicator only for a pull, the thin bar skipped while pulled, the first load and a failed first load unchanged), and add a scrollable `ScrollableMessage` for empty states; verify `./kotlin build` and a test of the flag on the wasm platform

## 2. Participant screens

- [x] 2.1 On Groups, the group page, the participant's wishlist, the receiver's wishlist, the receiver page, the Santa's questions and the Santa reveal: pass `onRefresh`, delete the Refresh row and make the empty states scrollable; verify on the Android emulator as a participant: a pull from the top of the Groups list reloads it and shows then hides the indicator, a pull on a group page and on an empty wishlist works, no Refresh button remains on any of them, the screen reloads quietly on returning to the app, and with the server stopped a pull ends with the content kept and "Try again" shown

## 3. Admin screens

- [x] 3.1 On the admin groups list, group page, users list and user page: the same change; verify on the Android emulator as the administrator that a pull works on each, no Refresh button remains, and a pull on the group page does not close or open a sheet

## 4. Web and the rest

- [x] 4.1 Verify in headless Chrome that no screen has a Refresh button and that refreshing the page on a group's screen keeps the screen and the session; delete `Strings.REFRESH` and update the README (the refresh sentences for each platform)
- [x] 4.2 Run `./kotlin build` and `./kotlin test` for the whole repo; note what could not be tried on iOS and ask the person running the iOS app to pull a list, a detail page and an empty screen

## Workflow follow-up

- Archive the change after review and sync the delta specs into `openspec/specs`.

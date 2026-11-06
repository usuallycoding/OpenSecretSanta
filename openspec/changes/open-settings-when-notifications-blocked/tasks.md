# Tasks

## 1. The blocked answer

- [x] 1.1 Add `PushEnable.Blocked`, `TurnOn.Blocked` and `NotificationsMessage.PermissionBlocked`, and the `canOpenSettings` and `openSettings()` hook on `PushDevice` with the no-op defaults, passing them through `PushRegistration` and `NotificationsViewModel`; verify with a `PushRegistrationTest` case that a blocked device gives `TurnOn.Blocked` and a denied one still gives `TurnOn.Denied`.
- [x] 1.2 Make the view model show `PermissionBlocked` for `TurnOn.Blocked`, keep the switch off, and clear a permission message on reload once the device is permitted again, leaving the switch off; verify with `NotificationsViewModelTest` cases for blocked, still blocked after a reload, and allowed after a reload.
- [x] 1.3 Extend the fake push device so tests can make it answer blocked and record that its settings were opened; verify the existing push tests still pass.

## 2. The sheet

- [x] 2.1 Add the strings for the blocked message and the "Open settings" button to `Strings.kt`, and show them in `NotificationsSheet` for `PermissionBlocked`, the button only when `canOpenSettings` is true; verify by running the Android app and by a build of all targets.
- [x] 2.2 Keep the web's message without a button; verify by building the web app and reading that `WebPushDevice` still answers `Denied` and does not offer settings.

## 3. Android

- [x] 3.1 Add `notificationAnswer(sdkInt, granted, canAskAgain)` to `androidApp` and unit-test it for granted, denied with the system able to ask again, denied for good, and Android 12 and older.
- [x] 3.2 Make `NotificationPermission.request()` answer through it, with `MainActivity` reading `shouldShowRequestPermissionRationale` in the result callback; make `AndroidPushDevice.enable()` return `Blocked` or `Denied` accordingly; verify on a device or emulator by refusing the permission twice.
- [x] 3.3 Implement `openSettings()` in `NotificationPermission`: the app's notification settings, the app-info page as the fallback; verify on a device or emulator that the button opens it and that coming back after allowing clears the message.

## 4. Specs

- [x] 4.1 Check the scenarios of the delta spec against the app on an emulator (first refusal, second refusal, the button, back from the settings), and sync the delta into `client-notifications` and archive the change when they pass.

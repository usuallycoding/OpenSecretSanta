# Design

## Context

See proposal.md for the motivation. `NotificationPermission` (`PushSupport.kt`) answers `request()` with a boolean, so a refusal the system will ask about again, one it will not ask about again and Android 12's missing question all come out as `PushEnable.Denied`, then `TurnOn.Denied`, then `NotificationsMessage.PermissionDenied`, which the sheet shows as red text. `LocalNetworkGate.kt` already tells these apart for its own permission by reading `shouldShowRequestPermissionRationale` right after the answer, and opens the app's settings with `ACTION_APPLICATION_DETAILS_SETTINGS`.

## Goals / Non-Goals

**Goals:**
- Show the button only where the system's own settings are the only way: the system will not ask again, or there is no question to ask.
- Keep the shared code free of Android types.

**Non-Goals:**
- No button on the web (a page cannot open browser settings) or on iOS (no Notifications row).
- No turning the switch on by itself when the person comes back from the settings.
- No button after a refusal the system will ask about again.

## Decisions

**A separate answer for "blocked".** `PushEnable` gets a `Blocked` case beside `Denied`, `TurnOn` and `NotificationsMessage` get one each. `Denied` keeps meaning a refusal that asking again can change, and the web keeps returning it, so its message and tests do not change. A flag on `Denied` was the alternative; a case is clearer to branch on and leaves the existing tests as they are.

**The rationale flag is read when the answer arrives.** After a refusal, `shouldShowRequestPermissionRationale(POST_NOTIFICATIONS)` is true while the system will ask again and false once it will not. Before the first question it is also false, so it only means something after an answer, as in the local-network gate. `MainActivity` reads it in the result callback and passes it to `NotificationPermission.onResult`, because only the activity can read it. When the activity has no launcher the answer stays `Denied`: nothing was refused.

**The classification is a pure function.** `notificationAnswer(sdkInt, granted, canAskAgain)` gives granted, denied or blocked: granted wins; below Android 13 an ungranted permission is blocked, as no question exists; otherwise blocked exactly when the system will not ask again. A function of plain values can be unit-tested in `androidApp` without a device.

**Opening the settings is a hook on `PushDevice`.** `canOpenSettings` (false by default) and `openSettings()`, which only `AndroidPushDevice` implements, through `NotificationPermission`. The view model exposes both through `PushRegistration`, and the sheet shows the button when the message is the blocked one and `canOpenSettings` is true. The shared UI never sees an `Intent`.

**Which settings page.** The app's notification settings (`ACTION_APP_NOTIFICATION_SETTINGS` with the package, Android 8 and later), where the allow switch is one tap away, with the app-info page as the fallback on older versions or when no activity answers. Started with a new-task flag, as it is started from the application context.

**Coming back.** The sheet already reloads on resume. Reloading also clears a permission message once the device is permitted again; it leaves the switch off, because turning it on is also what registers the device with the server.

## Risks / Trade-offs

- [Some devices report the rationale flag differently after "Don't ask again" ticked in older dialogs] → the flag is false in that case too, so it reads as blocked, which is correct.
- [A person who dismissed the first dialog by tapping outside is counted as refused] → Android counts it as a refusal too, so the button appears when Android stops asking, not before.
- [The settings page cannot be opened on a device with a stripped-down system] → the fallback to the app-info page, and if that fails too the call is ignored and the message stays.

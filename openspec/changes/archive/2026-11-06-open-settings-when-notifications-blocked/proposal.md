# Proposal

## Why

When the person has refused the notification permission so often that Android no longer shows its question, turning the device switch on does nothing but show a message that says to change a setting somewhere. The only way out is to find the app's settings by hand. The local-network gate already offers a button for the same situation.

## What Changes

- On Android, when the notification permission is refused and the system will not ask again, the Notifications sheet shows an "Open settings" button under its message. It opens the app's notification settings in the system, where notifications are allowed or denied.
- That situation is told apart from a refusal the system will ask about again: after one refusal the message stays as it is, with no button.
- Android 12 and older never show a question, so a person whose notifications are off there gets the button at once.
- When the person comes back from the settings and notifications are allowed, the message goes away. The switch stays off until they turn it on.
- The web keeps its message: a browser cannot open its settings for a site. iOS has no Notifications row.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `client-notifications`: the "A switch for this device" requirement gains the refused-for-good case with its button, and the message clearing when notifications are allowed again.

## Impact

- `app/shared`: `PushDevice` (a "blocked" answer and a way to open the system settings that only Android has), `PushRegistration`, `NotificationsViewModel`, `NotificationsSheet`, `Strings.kt`.
- `app/androidApp`: `PushSupport.kt` (the permission answer says whether the system will ask again, and the settings are opened) and `MainActivity.kt` (reports that to it).
- `app/shared/src@wasmJs`: unchanged in behavior; it keeps answering "denied".
- Tests: the fake push device, `PushRegistrationTest`, `NotificationsViewModelTest`, and a unit test in `androidApp` for how an answer is classified.
- No server, API or contract change.

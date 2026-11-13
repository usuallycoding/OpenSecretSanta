# Proposal

## Why

Wishlist changes, questions, answers and letters each push a notification a minute after the action. For a gift exchange that is a lot of noise, and it tells a person almost in real time when their Santa or receiver is active. One quiet notification a day is enough to say "come and look", and the app already shows what is new.

## What Changes

- Announcement notifications (added to a group, names drawn, a message from the owner) stay exactly as they are.
- The wishlist, question, answer and letter notifications are replaced by **one "Daily digest"** push per person, sent at **20:00 Europe/Bucharest** and only when something happened for that person since the last one. It is one notification for all groups and never says what, who or which group.
- Tapping it opens the group list.
- What is waiting for the digest is stored in the database, so a server restart does not lose it.
- The five per-person switches become two: **Announcements** and **Daily digest**. The Daily switch starts on if any of the four old ones was on, and off only if all four were off. The four old fields stay accepted and are reported by the server for older apps.
- The "Notifications" sheet shows two toggles instead of five; Android lists two notification channels.
- The per-minute holding of wishlist, question, answer and letter notifications is removed.
- No API version bump: the change adds an optional field and a new notification category.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `push-notifications`: the wishlist, question/answer and letter notification requirements are removed; a daily digest, its pending marks and its time are added; two categories replace five; the secrecy and "opening a notification" rules cover the digest.
- `client-notifications`: two toggles instead of five; a tap on the daily notification opens the group list; the taps for questions, answers, wishlist and letters are removed.
- `user-administration`: deleting a user also removes what is waiting for their digest.

## Impact

- Server: `Notifier`, `NotificationDispatcher` (the hold goes away, a daily schedule and a pending store come in), the notification repositories, a new migration (V018), the notification routes and DTO mapping, `Application.kt`/`ServerModules.kt` wiring.
- Contract: `NotificationSettingsDto` and `NotificationSettingsRequest` gain `daily`; `NotificationData` gains the `daily` category.
- Domain: `NotificationSettings`, `NotificationCategory`, the notification use cases.
- Clients: settings sheet and view model, `Strings`, `NotificationTarget`, the Android channels and notification builder, the web service worker's address (already falls back to the group list without a group).
- Older Android apps do not know the `daily` category and will not show the digest until updated; the web app is deployed with the server.

## 1. Contract and domain

- [x] 1.1 Add `ADMIN_GROUP_ANNOUNCEMENTS` and its helper, `AnnouncementRequest`, `AnnouncementResponse`, `PushSummaryDto`, and `emailAvailable`/`pushAvailable` on `AccountDto`
- [x] 1.2 Split `drawEmail` into shared helpers and add `announcementEmail`; the draw email text must not change
- [x] 1.3 Add `Notifier.planAnnouncement` with the 300-character cut and the reached/not-reached counts
- [x] 1.4 Add the `SendAnnouncement` use case with validation order, state and availability checks
- [x] 1.5 Domain tests: states, validation, channels, email layout, push plan, counts, no draw details in the email

## 2. Server

- [x] 2.1 Provide the two availability flags on `GET /me`
- [x] 2.2 Add `AnnouncementRoutes.kt`, queue the push messages, and wire it in Koin
- [x] 2.3 Route tests: success per channel, 400, 404/403, 409 state, 409 unavailable, failed emails, no content in logs
- [x] 2.4 Add the README paragraph, including the limits

## 3. Client logic

- [x] 3.1 Add `ApiClient.announce` and the `ScriptedServer` announcement fake
- [x] 3.2 Add `AnnouncementViewModel` and the strings, and reword the Announcements toggle description
- [x] 3.3 View model and API tests

## 4. Client UI

- [x] 4.1 Add `AnnouncementSheet` with the channels, subject preview, push hint, confirmation and result
- [x] 4.2 Add the "Send announcement" button for Pending and Active groups
- [x] 4.3 Check on the device: greyed-out channels, confirm, results, push arrival

## 5. Integration

- [x] 5.1 Run the full test suites and `openspec validate send-group-announcements --strict`
- [x] 5.2 Docker build and the user's web check

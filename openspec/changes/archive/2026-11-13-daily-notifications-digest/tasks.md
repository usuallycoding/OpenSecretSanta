# Tasks

## 1. Settings: two switches

- [x] 1.1 In `core/domain`, change `NotificationCategory` to `ANNOUNCEMENTS, DAILY` and `NotificationSettings` to `(announcements, daily)`, with the update use case; verify with domain tests that both default to on and `allows` answers per category
- [x] 1.2 Add migration V018 (register it in `server/resources/db/migration/index.txt`): a `daily` column on `notification_prefs` backfilled with 1 when any of wishlist, questions, answers, letters is 1 and 0 when all are 0, and the `digest_marks` table (username primary key referencing `users` with `ON DELETE CASCADE`, `marked_at`); verify with `NotificationMigrationTest` for some-on, all-off and no-row people
- [x] 1.3 Update `SqliteNotificationSettingsRepository` to read and write `daily`, writing the four old columns with its value; verify with `SqliteNotificationRepositoriesTest`
- [x] 1.4 Add `daily` to `NotificationSettingsDto` and, optionally, to `NotificationSettingsRequest`, keep the four older fields (reported equal to `daily`) and derive `daily` from them when it is absent, in `core/contract` and `server/.../NotificationRoutes.kt`; verify with `NotificationRoutesTest` for defaults, a new client, an older client with some on, an older client with all off and a 400 for bad values, and with the contract serialization tests

## 2. Marks and the digest on the server

- [x] 2.1 Add `DigestMarkRepository` (`mark`, atomic `takeAll`) to the domain, a SQLite implementation and a fake for tests; verify that a mark set during `takeAll` is not lost or sent twice and that deleting a user removes their mark (`DeleteUserRoutesTest`)
- [x] 2.2 In `Notifier`, replace `wishlistChanged`, `questionAsked`, `questionAnswered` and `letterSent` with marking of the right person (Active groups only, never the actor, no mark on reorder) and add `sendDigest()` that takes all marks, keeps those with Daily on, sends one `daily` message to each of their devices and clears everything; make `PushMessage.groupId` nullable and have `FcmPushSender` omit it; verify with `NotifierTest` and `FcmPushSenderTest` (text "Some things changed in your groups", no group, nothing for people who are not marked, nothing when Daily is off, marks cleared when push is off)
- [x] 2.3 In `NotificationDispatcher`, remove the hold (`Held`, quiet and maximum waits) and have the four entry points mark through the existing error-swallowing wrapper; verify with `NotificationDispatcherTest` and `NotificationEventRoutesTest` that actions still succeed when marking fails and that no push is sent at the time of the action
- [x] 2.4 Add the daily trigger: compute the next 20:00 in `Europe/Bucharest` from the injected clock, schedule the digest on the notification executor and reschedule after each run; start it from `Application.kt`/`ServerModules.kt`; verify with a test that drives a manual scheduler across a normal day, the March and October clock changes and a restart with stored marks

## 3. The apps

- [x] 3.1 In `app/shared`, make the Notifications sheet show two toggles ("Announcements", "Daily digest" with its description) in `NotificationsViewModel`, `Strings` and the sheet, saving `daily`; verify with `NotificationsViewModelTest` and `NotificationApiTest`
- [x] 3.2 Make `NotificationTarget.routesOf` accept a missing group and return no routes for `daily`, drop the question, answer, wishlist and letter targets, and have `openNotification` land on the group list; verify with `NotificationTargetTest` and `LinkTargetTest`
- [x] 3.3 On Android, replace the channels with `announcements` and `daily` (deleting the retired channel ids), show a `daily` push without a group and open the group list on its tap (including in `MainActivity.routesOf`); verify by sending a test push with category `daily` to a debug build and tapping it
- [x] 3.4 On the web, check that a `daily` push with no group opens `#/groups` in `firebase-messaging-sw.js` and adjust only if it does not; verify with a test push in a browser

## 4. Wrap-up

- [x] 4.1 Update the Purpose paragraphs of the main `push-notifications` and `client-notifications` specs to name the two kinds and the daily digest, and run `openspec validate daily-notifications-digest --strict`
- [x] 4.2 Run the full `server`, `core` and `app/shared` test suites and verify they pass

## Workflow follow-up

- Archive the change once implemented and reviewed, which syncs the deltas into `push-notifications`, `client-notifications` and `user-administration`.
